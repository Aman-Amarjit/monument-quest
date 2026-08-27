import { prisma } from '../lib/prisma';

const DEFAULT_IMAGE = 'https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800';
const MAX_COMMENT_LENGTH = 500;
const MAX_CAPTION_LENGTH = 500;
const ALLOWED_POST_TYPES = new Set(['CHECKIN', 'DISCOVERY', 'TIME_CAPSULE', 'REFLECTION']);

const clamp = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);

function rankFor(points: number): string {
  if (points >= 5000) return 'Legendary Pathfinder';
  if (points >= 2500) return 'Master Explorer';
  if (points >= 1000) return 'Temple City Historian';
  return 'Bhubaneswar Explorer';
}

function avatarFor(name: string, avatarUrl?: string | null): string {
  return avatarUrl && avatarUrl.trim().length > 0 ? avatarUrl : 'https://ui-avatars.com/api/?name=' + encodeURIComponent(name) + '&background=1E293B&color=D4AF37&bold=true&size=200';
}

function validateMediaUrl(value?: string): string | undefined {
  if (!value) return undefined;
  const media = value.trim();
  const validRemote = /^https:\/\//i.test(media);
  const validDataImage = /^data:image\/(jpeg|png|webp);base64,[a-z0-9+/]+={0,2}$/i.test(media);
  if (!validRemote && !validDataImage) throw { status: 400, message: 'Images must use an https URL or compressed image data' };
  if (media.length > 850000) throw { status: 413, message: 'Image is too large. Choose a smaller photo.' };
  return media;
}

export function toFeedItem(post: any, isLiked = false, isSaved = false, isFollowing = false) {
  return {
    id: post.id,
    userId: post.user?.id || '',
    userName: post.user?.name || 'Heritage Explorer',
    userAvatarUrl: avatarFor(post.user?.name || 'Explorer', post.user?.avatarUrl),
    userRank: rankFor(post.user?.points || 0),
    monumentName: post.monument?.name || 'Unknown monument',
    locationName: post.monument?.locationName || '',
    imageUrl: post.imageUrl || DEFAULT_IMAGE,
    caption: post.caption,
    postType: post.postType || 'CHECKIN',
    likesCount: post._count?.likes || 0,
    isLiked,
    isSaved,
    isFollowing,
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
  static async getFeed(userId?: string, limit?: unknown, cursor?: string, scope?: string) {
    const take = clamp(limit, 20, 50);
    const where: any = { visibility: 'PUBLIC' };
    if (scope?.toLowerCase() === 'guild') {
      if (!userId) return { items: [], nextCursor: null };
      const viewer = await prisma.user.findUnique({ where: { id: userId }, select: { guildId: true } });
      if (!viewer?.guildId) return { items: [], nextCursor: null };
      where.user = { guildId: viewer.guildId };
    }

    const posts = await prisma.post.findMany({
      where,
      take: take + 1,
      ...(cursor ? { skip: 1, cursor: { id: cursor } } : {}),
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      include: postInclude
    });
    const hasMore = posts.length > take;
    const rows = posts.slice(0, take);
    const postIds = rows.map((post) => post.id);
    const liked = userId && postIds.length ? new Set((await prisma.postLike.findMany({ where: { userId, postId: { in: postIds } }, select: { postId: true } })).map((item) => item.postId)) : new Set<string>();
    const saved = userId && postIds.length ? new Set((await prisma.savedPost.findMany({ where: { userId, postId: { in: postIds } }, select: { postId: true } })).map((item) => item.postId)) : new Set<string>();
    const followingIds = userId && rows.length ? new Set((await prisma.follow.findMany({ where: { followerId: userId, followingId: { in: rows.map((post) => post.userId) } }, select: { followingId: true } })).map((item) => item.followingId)) : new Set<string>();
    return { items: rows.map((post) => toFeedItem(post, liked.has(post.id), saved.has(post.id), followingIds.has(post.userId))), nextCursor: hasMore ? rows[rows.length - 1].id : null };
  }

  static async createPost(userId: string, input: { monumentId: string; caption: string; imageUrl?: string; postType?: string }) {
    const caption = input.caption.trim();
    if (!caption || caption.length > MAX_CAPTION_LENGTH) throw { status: 400, message: 'Caption must be between 1 and 500 characters' };
    const postType = input.postType?.trim().toUpperCase() || 'CHECKIN';
    if (!ALLOWED_POST_TYPES.has(postType)) throw { status: 400, message: 'Invalid post type' };
    const imageUrl = validateMediaUrl(input.imageUrl);
    const monument = await prisma.monument.findUnique({ where: { id: input.monumentId } });
    if (!monument) throw { status: 404, message: 'Monument not found' };
    const post = await prisma.post.create({ data: { userId, monumentId: monument.id, caption, imageUrl, postType }, include: postInclude });
    return toFeedItem(post);
  }

  static async deletePost(postId: string, userId: string) {
    const post = await prisma.post.findUnique({ where: { id: postId }, select: { userId: true } });
    if (!post) throw { status: 404, message: 'Post not found' };
    if (post.userId !== userId) throw { status: 403, message: 'You can only delete your own posts' };
    await prisma.post.delete({ where: { id: postId } });
    return { success: true, message: 'Post deleted successfully' };
  }

  static async toggleLike(postId: string, userId: string) {
    return prisma.$transaction(async (tx) => {
      const post = await tx.post.findUnique({ where: { id: postId }, select: { id: true } });
      if (!post) throw { status: 404, message: 'Post not found' };
      const existing = await tx.postLike.findUnique({ where: { postId_userId: { postId, userId } } });
      if (existing) await tx.postLike.delete({ where: { id: existing.id } });
      else await tx.postLike.create({ data: { postId, userId } });
      return { isLiked: !existing, likesCount: await tx.postLike.count({ where: { postId } }) };
    });
  }

  static async getComments(postId: string, limit?: unknown) {
    const comments = await prisma.comment.findMany({ where: { postId }, take: clamp(limit, 100, 100), orderBy: [{ createdAt: 'asc' }, { id: 'asc' }], include: { user: { select: { id: true, name: true, avatarUrl: true } } } });
    return comments.map((comment) => ({ id: comment.id, postId: comment.postId, userId: comment.user.id, userName: comment.user.name, userAvatarUrl: avatarFor(comment.user.name, comment.user.avatarUrl), body: comment.body, createdAt: comment.createdAt.getTime() }));
  }

  static async addComment(postId: string, userId: string, body: string) {
    const cleanBody = body.trim();
    if (!cleanBody || cleanBody.length > MAX_COMMENT_LENGTH) throw { status: 400, message: 'Comment must be between 1 and 500 characters' };
    const post = await prisma.post.findUnique({ where: { id: postId }, select: { id: true } });
    if (!post) throw { status: 404, message: 'Post not found' };
    const comment = await prisma.comment.create({ data: { postId, userId, body: cleanBody }, include: { user: { select: { id: true, name: true, avatarUrl: true } } } });
    return { id: comment.id, postId: comment.postId, userId: comment.user.id, userName: comment.user.name, userAvatarUrl: avatarFor(comment.user.name, comment.user.avatarUrl), body: comment.body, createdAt: comment.createdAt.getTime() };
  }
}
