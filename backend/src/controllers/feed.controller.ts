import { Response, NextFunction } from 'express';
import { FeedService } from '../services/feed.service';
import { AuthRequest } from '../middleware/auth.middleware';

export class FeedController {
  static async getFeed(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const result = await FeedService.getFeed(req.user?.id, req.query.limit, typeof req.query.cursor === 'string' ? req.query.cursor : undefined);
      res.json({ success: true, count: result.items.length, data: result.items, pagination: { nextCursor: result.nextCursor } });
    } catch (error) { next(error); }
  }

  static async createPost(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const { monumentId, caption, imageUrl, postType } = req.body || {};
      if (typeof monumentId !== 'string' || typeof caption !== 'string') return res.status(400).json({ success: false, error: 'monumentId and caption are required' });
      res.status(201).json({
        success: true,
        data: await FeedService.createPost(req.user!.id, {
          monumentId,
          caption,
          imageUrl: typeof imageUrl === 'string' ? imageUrl : undefined,
          postType: typeof postType === 'string' ? postType : undefined
        })
      });
    } catch (error) { next(error); }
  }

  static async deletePost(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      res.json(await FeedService.deletePost(req.params.id));
    } catch (error) { next(error); }
  }

  static async toggleLike(req: AuthRequest, res: Response, next: NextFunction) {
    try { res.json({ success: true, data: await FeedService.toggleLike(req.params.id, req.user!.id) }); }
    catch (error) { next(error); }
  }

  static async getComments(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const data = await FeedService.getComments(req.params.id, req.query.limit);
      res.json({ success: true, count: data.length, data });
    } catch (error) { next(error); }
  }

  static async addComment(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const { body, text } = req.body || {};
      const comment = typeof body === 'string' ? body : text;
      if (typeof comment !== 'string') return res.status(400).json({ success: false, error: 'Comment body is required' });
      res.status(201).json({ success: true, data: await FeedService.addComment(req.params.id, req.user!.id, comment) });
    } catch (error) { next(error); }
  }
}
