import { Request, Response, NextFunction } from 'express';
import { FeedService } from '../services/feed.service';
import { AuthRequest } from '../middleware/auth.middleware';

export class FeedController {
  public static async getFeed(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const filter = typeof req.query.filter === 'string' ? req.query.filter : 'GLOBAL';
      const monumentId = typeof req.query.monumentId === 'string' ? req.query.monumentId : undefined;
      const posts = await FeedService.getFeed(filter, req.user?.id, monumentId);
      return res.json({ success: true, count: posts.length, data: posts });
    } catch (err) {
      return next(err);
    }
  }

  public static async createPost(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const { monumentId, monumentName, caption, postType, imageUrl } = req.body;
      if (typeof caption !== 'string' || (typeof monumentId !== 'string' && typeof monumentName !== 'string')) {
        return res.status(400).json({ success: false, error: 'A monument and caption are required' });
      }
      const post = await FeedService.createPost(req.user!.id, typeof monumentId === 'string' ? monumentId : undefined, typeof monumentName === 'string' ? monumentName : undefined, caption, typeof postType === 'string' ? postType : 'CHECKIN', typeof imageUrl === 'string' ? imageUrl : undefined);
      return res.status(201).json({ success: true, data: post });
    } catch (err) {
      return next(err);
    }
  }

  public static async toggleLike(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const result = await FeedService.toggleLike(req.params.id, req.user!.id);
      return res.json({ success: true, data: result });
    } catch (err) {
      return next(err);
    }
  }
}
