import { Request, Response, NextFunction } from 'express';
import { prisma } from '../lib/prisma';
import { AuthRequest } from '../middleware/auth.middleware';

export class UserController {
  // GET /user/profile — returns full profile with progress
  static async getProfile(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const user = await prisma.user.findUnique({
        where: { id: req.user!.id },
        include: { guild: true, discoveries: true }
      });
      if (!user) return res.status(404).json({ success: false, error: 'User not found' });

      const visitedCount = user.discoveries.length;
      const xp = user.points;
      const level = Math.max(1, Math.floor(xp / 500) + 1);

      return res.json({
        success: true,
        data: {
          id: user.id,
          name: user.name,
          email: user.email,
          xp,
          level,
          streakDays: (user as any).streakDays ?? 0,
          visitedCount,
          totalDistanceKm: (user as any).totalDistanceKm ?? 0,
          areaUnlockedKm2: (user as any).areaUnlockedKm2 ?? 0,
          walkPathJson: (user as any).walkPathJson ?? '[]',
          exploredZonesJson: (user as any).exploredZonesJson ?? '[]',
          userRank: user.userRank,
          role: user.role,
          guild: user.guild ? { id: user.guild.id, name: user.guild.name } : null
        }
      });
    } catch (error) { next(error); }
  }

  // PATCH /user/progress — sync walk progress from phone to database
  static async syncProgress(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const {
        totalDistanceKm,
        areaUnlockedKm2,
        streakDays,
        walkPathJson,
        exploredZonesJson,
        xpDelta   // extra XP earned this session (from walking, etc.)
      } = req.body ?? {};

      const updateData: Record<string, any> = {};
      if (typeof totalDistanceKm === 'number') updateData.totalDistanceKm = totalDistanceKm;
      if (typeof areaUnlockedKm2 === 'number')  updateData.areaUnlockedKm2  = areaUnlockedKm2;
      if (typeof streakDays === 'number')        updateData.streakDays        = streakDays;
      if (typeof walkPathJson === 'string')      updateData.walkPathJson      = walkPathJson;
      if (typeof exploredZonesJson === 'string') updateData.exploredZonesJson = exploredZonesJson;

      // Add walking XP to existing points
      if (typeof xpDelta === 'number' && xpDelta > 0) {
        const user = await prisma.user.findUnique({ where: { id: req.user!.id }, select: { points: true } });
        if (user) updateData.points = user.points + Math.min(xpDelta, 500); // cap 500 XP per sync
      }

      if (Object.keys(updateData).length === 0) {
        return res.json({ success: true, message: 'Nothing to update' });
      }

      const updated = await prisma.user.update({
        where: { id: req.user!.id },
        data: updateData,
        select: { id: true, points: true }
      });

      return res.json({ success: true, data: { xp: updated.points } });
    } catch (error) { next(error); }
  }

  // PATCH /user/profile — update name/bio/avatarUrl
  static async updateProfile(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const { name, avatarUrl } = req.body ?? {};
      const updateData: Record<string, any> = {};
      if (typeof name === 'string' && name.trim().length > 0) updateData.name = name.trim();
      if (typeof avatarUrl === 'string') updateData.avatarUrl = avatarUrl;

      if (Object.keys(updateData).length === 0) {
        return res.status(400).json({ success: false, error: 'Nothing to update' });
      }

      await prisma.user.update({ where: { id: req.user!.id }, data: updateData });
      return res.json({ success: true, message: 'Profile updated' });
    } catch (error) { next(error); }
  }
}
