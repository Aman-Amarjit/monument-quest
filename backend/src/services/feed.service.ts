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
    userId: post.user.id,
    userName: post.user.name,
    userAvatarUrl: avatarFor(post.user.name, post.user.avatarUrl),
    userRank: rankFor(post.user.points),
    monumentName: post.monument.name,
    locationName: post.monument.locationName,
    imageUrl: post.imageUrl || DEFAULT_IMAGE,
    caption: post.caption,
    postType: post.postType,
    likesCount: post._count.likes,
    isLiked,
    commentsCount: post._count.comments,
    timestamp: post.createdAt.getTime()
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

    // Older mobile builds used m1 for the first catalog item. Keep that alias,
    // but never silently redirect arbitrary invalid monument IDs.
    let monument = await prisma.monument.findUnique({ where: { id: input.monumentId } });
    if (!monument && input.monumentId === 'm1') monument = await prisma.monument.findFirst({ orderBy: { name: 'asc' } });
    if (!monument) throw { status: 404, message: 'Choose a valid monument from the catalog' };

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

  static async getComments(postId: string, limit?: unknown) {
    const post = await prisma.post.findUnique({ where: { id: postId }, select: { id: true } });
    if (!post) throw { status: 404, message: 'Post not found' };

    const comments = await prisma.comment.findMany({
      where: { postId },
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

  static async addComment(postId: string, userId: string, body: string) {
    const cleanBody = body.trim();
    if (!cleanBody || cleanBody.length > MAX_COMMENT_LENGTH) throw { status: 400, message: 'Comment must be between 1 and 500 characters' };

    const post = await prisma.post.findUnique({ where: { id: postId }, select: { id: true } });
    if (!post) throw { status: 404, message: 'Post not found' };

    const comment = await prisma.comment.create({
      data: { postId, userId, body: cleanBody },
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
