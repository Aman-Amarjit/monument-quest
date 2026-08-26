import { prisma } from '../lib/prisma';

const allowedPostTypes = new Set(['DISCOVERY', 'CHECKIN', 'TIME_CAPSULE', 'REFLECTION']);

export class FeedService {
  public static async getFeed(filter = 'GLOBAL', viewerId?: string, monumentId?: string) {
    const posts = await prisma.post.findMany({
      where: monumentId ? { monumentId } : undefined,
      orderBy: { createdAt: 'desc' },
      take: 100,
      include: {
        user: { select: { id: true, name: true, userRank: true } },
        monument: { select: { id: true, name: true, locationName: true } },
        likes: { select: { userId: true } },
        _count: { select: { likes: true } }
      }
    });

    return posts
      .filter((post) => filter !== 'NEARBY' || post.monument.locationName.toLowerCase().includes('bhubaneswar'))
      .map((post) => ({
        id: post.id,
        userId: post.user.id,
        userName: post.user.name,
        userRank: post.user.userRank,
        monumentId: post.monument.id,
        monumentName: post.monument.name,
        locationName: post.monument.locationName,
        imageUrl: post.imageUrl || undefined,
        caption: post.caption,
        postType: post.postType,
        likesCount: post._count.likes,
        isLiked: viewerId ? post.likes.some((like) => like.userId === viewerId) : false,
        commentsCount: 0,
        timestamp: post.createdAt.getTime()
      }));
  }

  public static async createPost(userId: string, monumentId: string | undefined, monumentName: string | undefined, caption: string, postType = 'CHECKIN', imageUrl?: string) {
    const cleanCaption = caption.trim();
    if (cleanCaption.length < 1 || cleanCaption.length > 2000) throw { status: 400, message: 'Caption must be between 1 and 2000 characters' };
    const normalizedName = monumentName?.trim().toLowerCase();
    const monument = monumentId
      ? await prisma.monument.findUnique({ where: { id: monumentId } })
      : (await prisma.monument.findMany()).find((item) => item.name.toLowerCase() === normalizedName);
    if (!monument) throw { status: 404, message: 'Choose a valid monument from the catalog' };
    if (!allowedPostTypes.has(postType)) throw { status: 400, message: 'Unsupported post type' };

    const post = await prisma.post.create({
      data: { userId, monumentId: monument.id, caption: cleanCaption, postType, imageUrl },
      include: {
        user: { select: { id: true, name: true, userRank: true } },
        monument: { select: { id: true, name: true, locationName: true } },
        _count: { select: { likes: true } }
      }
    });

    return {
      id: post.id,
      userId: post.user.id,
      userName: post.user.name,
      userRank: post.user.userRank,
      monumentId: post.monument.id,
      monumentName: post.monument.name,
      locationName: post.monument.locationName,
      imageUrl: post.imageUrl || undefined,
      caption: post.caption,
      postType: post.postType,
      likesCount: post._count.likes,
      isLiked: false,
      commentsCount: 0,
      timestamp: post.createdAt.getTime()
    };
  }

  public static async toggleLike(postId: string, userId: string) {
    const result = await prisma.$transaction(async (tx) => {
      const post = await tx.post.findUnique({ where: { id: postId } });
      if (!post) throw { status: 404, message: 'Post not found' };
      const existing = await tx.postLike.findUnique({ where: { postId_userId: { postId, userId } } });
      if (existing) await tx.postLike.delete({ where: { id: existing.id } });
      else await tx.postLike.create({ data: { postId, userId } });
      const likesCount = await tx.postLike.count({ where: { postId } });
      return { likesCount, isLiked: !existing };
    });
    return result;
  }
}
