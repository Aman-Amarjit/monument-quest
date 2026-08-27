import { prisma } from '../lib/prisma';

const clamp = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);
const storyMedia = (value: string) => {
  const media = value.trim();
  const validRemote = /^https:\/\//i.test(media);
  const validDataImage = /^data:image\/(jpeg|png|webp);base64,[a-z0-9+/]+={0,2}$/i.test(media);
  if (!validRemote && !validDataImage) throw { status: 400, message: 'Story media must use an https URL or compressed image data' };
  if (media.length > 850000) throw { status: 413, message: 'Story image is too large' };
  return media;
};
const badgeFor = (points: number) => points >= 5000 ? 'LEGENDARY' : points >= 2500 ? 'MASTER' : 'EXPLORER';

export class SocialService {
  static async leaderboard(limit = 50) {
    const users = await prisma.user.findMany({ take: clamp(limit, 50, 100), orderBy: [{ points: 'desc' }, { createdAt: 'asc' }], select: { id: true, name: true, points: true, _count: { select: { discoveries: true } } } });
    return users.map((user, index) => ({ rank: index + 1, id: user.id, name: user.name, xp: user.points, monumentsCaptured: user._count.discoveries, badge: badgeFor(user.points) }));
  }

  static async guilds() {
    const guilds = await prisma.guild.findMany({ orderBy: { name: 'asc' }, include: { _count: { select: { members: true } }, members: { select: { points: true } } } });
    return guilds.map((guild) => ({ id: guild.id, name: guild.name, region: guild.region, description: guild.description || guild.region, membersCount: guild._count.members, totalXp: guild.members.reduce((total, member) => total + member.points, 0) })).sort((a, b) => b.totalXp - a.totalXp || a.name.localeCompare(b.name)).map((guild, index) => ({ ...guild, rank: index + 1 }));
  }

  static async currentGuild(userId: string) {
    const user = await prisma.user.findUnique({ where: { id: userId }, select: { guild: { select: { id: true, name: true, region: true, description: true } } } });
    return user?.guild || null;
  }

  static async joinGuild(userId: string, guildId: string) {
    const guild = await prisma.guild.findUnique({ where: { id: guildId }, select: { id: true, name: true, region: true, description: true } });
    if (!guild) throw { status: 404, message: 'Guild not found' };
    await prisma.$transaction(async (tx) => {
      const user = await tx.user.findUnique({ where: { id: userId }, select: { id: true } });
      if (!user) throw { status: 404, message: 'User not found' };
      await tx.user.update({ where: { id: userId }, data: { guildId } });
    });
    return guild;
  }

  static async leaveGuild(userId: string) {
    await prisma.user.update({ where: { id: userId }, data: { guildId: null } });
    return { success: true };
  }

  static async guildLeaderboard(guildId: string, limit?: unknown) {
    const guild = await prisma.guild.findUnique({ where: { id: guildId }, select: { id: true } });
    if (!guild) throw { status: 404, message: 'Guild not found' };
    const users = await prisma.user.findMany({ where: { guildId }, take: clamp(limit, 50, 100), orderBy: [{ points: 'desc' }, { name: 'asc' }], select: { id: true, name: true, points: true } });
    return users.map((user, index) => ({ id: user.id, name: user.name, points: user.points, rank: index + 1 }));
  }

  static async stories(viewerId?: string) {
    const stories = await prisma.story.findMany({ where: { expiresAt: { gt: new Date() } }, orderBy: [{ createdAt: 'desc' }], take: 100, include: { user: { select: { id: true, name: true, avatarUrl: true } }, _count: { select: { views: true } } } });
    const viewed = viewerId ? new Set((await prisma.storyView.findMany({ where: { userId: viewerId, storyId: { in: stories.map((story) => story.id) } }, select: { storyId: true } })).map((view) => view.storyId)) : new Set<string>();
    return stories.map((story) => ({ id: story.id, userId: story.user.id, userName: story.user.name, avatarUrl: story.user.avatarUrl, mediaUrl: story.mediaUrl, caption: story.caption, createdAt: story.createdAt.getTime(), expiresAt: story.expiresAt.getTime(), viewsCount: story._count.views, isViewed: viewed.has(story.id) }));
  }

  static async createStory(userId: string, mediaUrl: string, caption?: string) {
    const cleanCaption = (caption || '').trim();
    if (cleanCaption.length > 160) throw { status: 400, message: 'Story caption must be 160 characters or fewer' };
    const story = await prisma.story.create({ data: { userId, mediaUrl: storyMedia(mediaUrl), caption: cleanCaption, expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000) }, include: { user: { select: { id: true, name: true, avatarUrl: true } }, _count: { select: { views: true } } } });
    return { id: story.id, userId: story.user.id, userName: story.user.name, avatarUrl: story.user.avatarUrl, mediaUrl: story.mediaUrl, caption: story.caption, createdAt: story.createdAt.getTime(), expiresAt: story.expiresAt.getTime(), viewsCount: story._count.views, isViewed: false };
  }

  static async viewStory(storyId: string, userId: string) {
    const story = await prisma.story.findFirst({ where: { id: storyId, expiresAt: { gt: new Date() } }, select: { id: true } });
    if (!story) throw { status: 404, message: 'Story not found or expired' };
    await prisma.storyView.upsert({ where: { storyId_userId: { storyId, userId } }, create: { storyId, userId }, update: { viewedAt: new Date() } });
    return { success: true };
  }

  static async toggleFollow(userId: string, targetUserId: string) {
    if (userId === targetUserId) throw { status: 400, message: 'You cannot follow yourself' };
    const target = await prisma.user.findUnique({ where: { id: targetUserId }, select: { id: true } });
    if (!target) throw { status: 404, message: 'Explorer not found' };
    return prisma.$transaction(async (tx) => {
      const existing = await tx.follow.findUnique({ where: { followerId_followingId: { followerId: userId, followingId: targetUserId } } });
      if (existing) await tx.follow.delete({ where: { id: existing.id } });
      else await tx.follow.create({ data: { followerId: userId, followingId: targetUserId } });
      return { isFollowing: !existing };
    });
  }

  static async toggleSave(userId: string, postId: string) {
    const post = await prisma.post.findUnique({ where: { id: postId }, select: { id: true } });
    if (!post) throw { status: 404, message: 'Post not found' };
    return prisma.$transaction(async (tx) => {
      const existing = await tx.savedPost.findUnique({ where: { postId_userId: { postId, userId } } });
      if (existing) await tx.savedPost.delete({ where: { id: existing.id } });
      else await tx.savedPost.create({ data: { postId, userId } });
      return { isSaved: !existing };
    });
  }

  static async reportPost(userId: string, postId: string, reason: string) {
    const cleanReason = reason.trim();
    if (!cleanReason || cleanReason.length > 200) throw { status: 400, message: 'A report reason between 1 and 200 characters is required' };
    const post = await prisma.post.findUnique({ where: { id: postId }, select: { id: true } });
    if (!post) throw { status: 404, message: 'Post not found' };
    await prisma.postReport.upsert({ where: { postId_reporterId: { postId, reporterId: userId } }, create: { postId, reporterId: userId, reason: cleanReason }, update: { reason: cleanReason, status: 'OPEN' } });
    return { success: true, message: 'Thanks. The post was reported for review.' };
  }
}
