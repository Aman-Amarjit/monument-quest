import { prisma } from '../lib/prisma';

const clamp = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);

export class FeedService {
  static async getFeed(userId?: string, limit?: unknown, cursor?: string) {
    const take = clamp(limit, 20, 50);
    const posts = await prisma.post.findMany({
      take: take + 1,
      ...(cursor ? { skip: 1, cursor: { id: cursor } } : {}),
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      include: {
        user: { select: { name: true, avatarUrl: true } },
        monument: { select: { name: true, locationName: true } },
        _count: { select: { likes: true } }
      }
    });

    const hasMore = posts.length > take;
    const rows = posts.slice(0, take);
    const postIds: string[] = rows.map((post) => post.id);
    const liked = userId && postIds.length > 0
      ? new Set((await prisma.postLike.findMany({
          where: { userId, postId: { in: postIds } },
          select: { postId: true }
        })).map((like) => like.postId))
      : new Set<string>();

    const items = rows.map((post) => ({
      id: post.id,
      userName: post.user.name,
      userAvatarUrl: post.user.avatarUrl || null,
      monumentName: post.monument.name,
      locationName: post.monument.locationName,
      imageUrl: post.imageUrl || 'https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800',
      caption: post.caption,
      postType: post.postType,
      likesCount: post._count.likes,
      isLiked: liked.has(post.id),
      commentsCount: 0,
      timestamp: post.createdAt.getTime()
    }));

    return { items, nextCursor: hasMore ? rows[rows.length - 1].id : null };
  }

  static async createPost(userId: string, input: { monumentId: string; caption: string; imageUrl?: string; postType?: string }) {
    const caption = input.caption.trim();
    if (!caption || caption.length > 500) throw { status: 400, message: 'Caption must be between 1 and 500 characters' };

    let monumentId = input.monumentId;
    let monument = await prisma.monument.findUnique({ where: { id: monumentId } });
    if (!monument) {
      const firstMonument = await prisma.monument.findFirst();
      if (firstMonument) {
        monument = firstMonument;
        monumentId = firstMonument.id;
      } else {
        monument = await prisma.monument.create({
          data: {
            id: 'm1',
            name: 'Lingaraj Temple',
            locationName: 'Bhubaneswar, Odisha',
            latitude: 20.2381,
            longitude: 85.8338,
            category: 'Temple',
            pointsValue: 500,
            isVerified: true
          }
        });
        monumentId = monument.id;
      }
    }

    const post = await prisma.post.create({
      data: {
        userId,
        monumentId,
        caption,
        imageUrl: input.imageUrl?.slice(0, 2048) || 'https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800',
        postType: input.postType || 'CHECKIN'
      },
      include: {
        user: { select: { name: true, avatarUrl: true } },
        monument: { select: { name: true, locationName: true } },
        _count: { select: { likes: true } }
      }
    });

    return {
      id: post.id,
      userName: post.user.name,
      userAvatarUrl: post.user.avatarUrl || null,
      monumentName: post.monument.name,
      locationName: post.monument.locationName,
      imageUrl: post.imageUrl,
      caption: post.caption,
      postType: post.postType,
      likesCount: post._count.likes,
      isLiked: false,
      commentsCount: 0,
      timestamp: post.createdAt.getTime()
    };
  }

  static async toggleLike(postId: string, userId: string) {
    return prisma.$transaction(async (tx) => {
      const post = await tx.post.findUnique({ where: { id: postId } });
      if (!post) throw { status: 404, message: 'Post not found' };
      const existing = await tx.postLike.findUnique({ where: { postId_userId: { postId, userId } } });
      if (existing) await tx.postLike.delete({ where: { id: existing.id } });
      else await tx.postLike.create({ data: { postId, userId } });
      return { isLiked: !existing, likesCount: await tx.postLike.count({ where: { postId } }) };
    });
  }
}