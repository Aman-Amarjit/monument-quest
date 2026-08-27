import { prisma } from '../lib/prisma';

function badgeFor(points: number): string {
  return points >= 5000 ? 'LEGENDARY' : points >= 2500 ? 'MASTER' : 'EXPLORER';
}

export class SocialService {
  static async leaderboard(limit = 50) {
    const users = await prisma.user.findMany({ take: Math.min(Math.max(limit, 1), 100), orderBy: [{ points: 'desc' }, { createdAt: 'asc' }], select: { id: true, name: true, points: true, _count: { select: { discoveries: true } } } });
    return users.map((user, index) => ({ rank: index + 1, id: user.id, name: user.name, xp: user.points, monumentsCaptured: user._count.discoveries, badge: badgeFor(user.points) }));
  }

  static async guilds() {
    const guilds = await prisma.guild.findMany({ orderBy: [{ totalPoints: 'desc' }, { name: 'asc' }], include: { _count: { select: { members: true } }, members: { select: { points: true } } } });
    return guilds.map((guild, index) => ({ id: guild.id, name: guild.name, region: guild.region, membersCount: guild._count.members, totalXp: guild.members.reduce((total, member) => total + member.points, 0), rank: index + 1 }));
  }
}
