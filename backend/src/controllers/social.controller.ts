import { Request, Response, NextFunction } from 'express';
import { SocialService } from '../services/social.service';
import { AuthRequest } from '../middleware/auth.middleware';

export class SocialController {
  static async leaderboard(req: Request, res: Response, next: NextFunction) { try { const data = await SocialService.leaderboard(Number(req.query.limit) || 50); res.json({ success: true, data, leaderboard: data }); } catch (error) { next(error); } }
  static async guilds(_req: Request, res: Response, next: NextFunction) { try { const data = await SocialService.guilds(); res.json({ success: true, data, guilds: data }); } catch (error) { next(error); } }
  static async currentGuild(req: AuthRequest, res: Response, next: NextFunction) { try { const data = await SocialService.currentGuild(req.user!.id); res.json({ success: true, data }); } catch (error) { next(error); } }
  static async joinGuild(req: AuthRequest, res: Response, next: NextFunction) { try { const data = await SocialService.joinGuild(req.user!.id, req.params.id); res.json({ success: true, data }); } catch (error) { next(error); } }
  static async leaveGuild(req: AuthRequest, res: Response, next: NextFunction) { try { res.json({ success: true, data: await SocialService.leaveGuild(req.user!.id) }); } catch (error) { next(error); } }
  static async guildLeaderboard(req: Request, res: Response, next: NextFunction) { try { const data = await SocialService.guildLeaderboard(req.params.id, req.query.limit); res.json({ success: true, data }); } catch (error) { next(error); } }
  static async stories(req: AuthRequest, res: Response, next: NextFunction) { try { const data = await SocialService.stories(req.user?.id); res.json({ success: true, data }); } catch (error) { next(error); } }
  static async createStory(req: AuthRequest, res: Response, next: NextFunction) { try { const { mediaUrl, caption } = req.body || {}; if (typeof mediaUrl !== 'string') return res.status(400).json({ success: false, error: 'mediaUrl is required' }); res.status(201).json({ success: true, data: await SocialService.createStory(req.user!.id, mediaUrl, typeof caption === 'string' ? caption : undefined) }); } catch (error) { next(error); } }
  static async viewStory(req: AuthRequest, res: Response, next: NextFunction) { try { res.json({ success: true, data: await SocialService.viewStory(req.params.id, req.user!.id) }); } catch (error) { next(error); } }
  static async toggleFollow(req: AuthRequest, res: Response, next: NextFunction) { try { res.json({ success: true, data: await SocialService.toggleFollow(req.user!.id, req.params.id) }); } catch (error) { next(error); } }
  static async toggleSave(req: AuthRequest, res: Response, next: NextFunction) { try { res.json({ success: true, data: await SocialService.toggleSave(req.user!.id, req.params.id) }); } catch (error) { next(error); } }
  static async reportPost(req: AuthRequest, res: Response, next: NextFunction) { try { const reason = req.body?.reason; if (typeof reason !== 'string') return res.status(400).json({ success: false, error: 'reason is required' }); res.json({ success: true, data: await SocialService.reportPost(req.user!.id, req.params.id, reason) }); } catch (error) { next(error); } }
}
