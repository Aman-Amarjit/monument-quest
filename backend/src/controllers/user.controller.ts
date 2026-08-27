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
      const userRank = xp >= 5000 ? 'Legendary Pathfinder' : xp >= 2500 ? 'Master Explorer' : xp >= 1000 ? 'Temple City Historian' : 'Bhubaneswar Explorer';

      const profile = {
        id: user.id,
        name: user.name,
        email: user.email,
        avatarUrl: user.avatarUrl,
        xp,
        level,
        streakDays: user.streakDays,
        visitedCount,
        totalDistanceKm: user.totalDistanceKm,
        areaUnlockedKm2: user.areaUnlockedKm2,
        walkPathJson: user.walkPathJson,
        exploredZonesJson: user.exploredZonesJson,
        userRank,
        role: user.role,
        guild: user.guild ? { id: user.guild.id, name: user.guild.name } : null
      };

      // Keep the documented envelope and expose fields at the root for the Android client.
      return res.json({ success: true, data: profile, ...profile });
    } catch (error) { next(error); }
  }

  // PATCH /user/progress — sync walk progress from phone to database (never accepts client XP)
  static async syncProgress(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const {
        totalDistanceKm,
        areaUnlockedKm2,
        streakDays,
        walkPathJson,
        exploredZonesJson,
      } = req.body ?? {};

      const updateData: Record<string, any> = {};
      if (typeof totalDistanceKm === 'number' && Number.isFinite(totalDistanceKm) && totalDistanceKm >= 0) updateData.totalDistanceKm = totalDistanceKm;
      if (typeof areaUnlockedKm2 === 'number' && Number.isFinite(areaUnlockedKm2) && areaUnlockedKm2 >= 0) updateData.areaUnlockedKm2 = areaUnlockedKm2;
      if (typeof streakDays === 'number' && Number.isInteger(streakDays) && streakDays >= 0) updateData.streakDays = streakDays;
      if (typeof walkPathJson === 'string')      updateData.walkPathJson      = walkPathJson;
      if (typeof exploredZonesJson === 'string') updateData.exploredZonesJson = exploredZonesJson;



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
      if (typeof name === 'string') {
        const cleanName = name.trim();
        if (cleanName.length < 2 || cleanName.length > 80) {
          return res.status(400).json({ success: false, error: 'Name must be between 2 and 80 characters' });
        }
        const duplicate = await prisma.user.findFirst({
          where: { name: { equals: cleanName, mode: 'insensitive' }, NOT: { id: req.user!.id } },
          select: { id: true }
        });
        if (duplicate) return res.status(409).json({ success: false, error: 'That explorer name is already taken' });
        updateData.name = cleanName;
      }
      if (typeof avatarUrl === 'string') {
        const cleanAvatarUrl = avatarUrl.trim();
        const isDataImage = /^data:image\/(?:jpeg|png|webp|gif);base64,[a-z0-9+/]+={0,2}$/i.test(cleanAvatarUrl);
        const isRemoteImage = /^https?:\/\//i.test(cleanAvatarUrl);
        if (cleanAvatarUrl.length > 900_000) return res.status(413).json({ success: false, error: 'Profile photo is too large' });
        if (cleanAvatarUrl && !isDataImage && !isRemoteImage) return res.status(400).json({ success: false, error: 'Profile photo must be an https URL or an encoded image' });
        updateData.avatarUrl = cleanAvatarUrl || null;
      }

      if (Object.keys(updateData).length === 0) {
        return res.status(400).json({ success: false, error: 'Nothing to update' });
      }

      await prisma.user.update({ where: { id: req.user!.id }, data: updateData });
      return res.json({ success: true, message: 'Profile updated' });
    } catch (error) { next(error); }
  }
}
