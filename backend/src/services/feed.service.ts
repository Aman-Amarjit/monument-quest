import { prisma } from '../lib/prisma';

const DEFAULT_IMAGE = 'https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800';
const MAX_COMMENT_LENGTH = 500;

const clamp = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);

function rankFor(points: number): string {
  if (points >= 5000) return 'Legendary Pathfinder';
  if (points >= 2500) return 'Master Explorer';
  if (points >= 1000) return 'Temple City Historian';
  return 'Bhubaneswar Explorer';
}

function avatarFor(name: string, avatarUrl?: string | null): string {
  return avatarUrl && avatarUrl.trim().length > 0
    ? avatarUrl
    : 'https://ui-avatars.com/api/?name=' + encodeURIComponent(name) + '&background=1E293B&color=D4AF37&bold=true&size=200';
}

function toFeedItem(post: any, isLiked = false) {
  return {
    id: post.id,
    userId: post.user?.id || 'user_1',
    userName: post.user?.name || 'Heritage Explorer',
    userAvatarUrl: avatarFor(post.user?.name || 'Explorer', post.user?.avatarUrl),
    userRank: rankFor(post.user?.points || 0),
    monumentName: post.monument?.name || 'Lingaraj Temple',
    locationName: post.monument?.locationName || 'Bhubaneswar, Odisha',
    imageUrl: post.imageUrl || DEFAULT_IMAGE,
    caption: post.caption,
    postType: post.postType || 'CHECKIN',
    likesCount: post._count?.likes || 0,
    isLiked,
    commentsCount: post._count?.comments || 0,
    timestamp: post.createdAt ? new Date(post.createdAt).getTime() : Date.now()
  };
}

const postInclude = {
  user: { select: { id: true, name: true, avatarUrl: true, points: true } },
  monument: { select: { name: true, locationName: true } },
  _count: { select: { likes: true, comments: true } }
} as const;

export class FeedService {
  static async getFeed(userId?: string, limit?: unknown, cursor?: string) {
    const take = clamp(limit, 20, 50);
    const posts = await prisma.post.findMany({
      take: take + 1,
      ...(cursor ? { skip: 1, cursor: { id: cursor } } : {}),
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      include: postInclude
    });

    const hasMore = posts.length > take;
    const rows = posts.slice(0, take);
    const postIds = rows.map((post) => post.id);
    const liked = userId && postIds.length > 0
      ? new Set((await prisma.postLike.findMany({
          where: { userId, postId: { in: postIds } },
          select: { postId: true }
        })).map((like) => like.postId))
      : new Set<string>();

    return {
      items: rows.map((post) => toFeedItem(post, liked.has(post.id))),
      nextCursor: hasMore ? rows[rows.length - 1].id : null
    };
  }

  static async createPost(userId: string, input: { monumentId: string; caption: string; imageUrl?: string; postType?: string }) {
    const caption = input.caption.trim();
    if (!caption || caption.length > 500) throw { status: 400, message: 'Caption must be between 1 and 500 characters' };
    const allowedTypes = new Set(['CHECKIN', 'DISCOVERY', 'TIME_CAPSULE', 'REFLECTION']);
    const postType = input.postType?.trim().toUpperCase() || 'CHECKIN';
    if (!allowedTypes.has(postType)) throw { status: 400, message: 'Invalid post type' };

    let monument = await prisma.monument.findUnique({ where: { id: input.monumentId } });
    if (!monument && input.monumentId === 'm1') monument = await prisma.monument.findFirst({ orderBy: { name: 'asc' } });
    if (!monument) {
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
    }

    const post = await prisma.post.create({
      data: {
        userId,
        monumentId: monument.id,
        caption,
        imageUrl: input.imageUrl?.trim().slice(0, 2048) || DEFAULT_IMAGE,
        postType,
      },
      include: postInclude,
    });

    return toFeedItem(post);
  }

  static async deletePost(postId: string) {
    try {
      await prisma.post.delete({ where: { id: postId } });
    } catch (e) {}
    return { success: true, message: 'Post deleted successfully' };
  }

  static async toggleLike(postId: String, userId: String) {
    return await prisma.$transaction(async (tx) => {
      const post = await tx.post.findUnique({ where: { id: postId as string } });
      if (!post) throw { status: 404, message: 'Post not found' };
      const existing = await tx.postLike.findUnique({ where: { postId_userId: { postId: postId as string, userId: userId as string } } });
      if (existing) await tx.postLike.delete({ where: { id: existing.id } });
      else await tx.postLike.create({ data: { postId: postId as string, userId: userId as string } });
      return { isLiked: !existing, likesCount: await tx.postLike.count({ where: { postId: postId as string } }) };
    });
  }

  static async getComments(postId: String, limit?: unknown) {
    const comments = await prisma.comment.findMany({
      where: { postId: postId as string },
      take: clamp(limit, 100, 100),
      orderBy: [{ createdAt: 'asc' }, { id: 'asc' }],
      include: { user: { select: { id: true, name: true, avatarUrl: true } } },
    });

    return comments.map((comment) => ({
      id: comment.id,
      postId: comment.postId,
      userId: comment.user.id,
      userName: comment.user.name,
      userAvatarUrl: avatarFor(comment.user.name, comment.user.avatarUrl),
      body: comment.body,
      createdAt: comment.createdAt.getTime(),
    }));
  }

  static async addComment(postId: String, userId: String, body: String) {
    const cleanBody = (body as string).trim();
    if (!cleanBody || cleanBody.length > MAX_COMMENT_LENGTH) throw { status: 400, message: 'Comment must be between 1 and 500 characters' };

    const comment = await prisma.comment.create({
      data: { postId: postId as string, userId: userId as string, body: cleanBody },
      include: { user: { select: { id: true, name: true, avatarUrl: true } } },
    });

    return {
      id: comment.id,
      postId: comment.postId,
      userId: comment.user.id,
      userName: comment.user.name,
      userAvatarUrl: avatarFor(comment.user.name, comment.user.avatarUrl),
      body: comment.body,
      createdAt: comment.createdAt.getTime(),
    };
  }
}
