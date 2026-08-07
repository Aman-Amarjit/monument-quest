import { Request, Response, NextFunction } from 'express';
import { FeedService } from '../services/feed.service';

export class FeedController {
  public static async getFeed(req: Request, res: Response, next: NextFunction) {
    try {
      const filter = (req.query.filter as string) || 'GLOBAL';
      const posts = await FeedService.getFeed(filter);
      res.json({ success: true, count: posts.length, data: posts });
    } catch (err) {
      next(err);
    }
  }

  public static async createPost(req: Request, res: Response, next: NextFunction) {
    try {
      const { monumentName, caption, postType, imageUrl } = req.body;
      const user = (req as any).user || { id: 'usr_demo', name: 'Adventurer' };

      if (!monumentName || !caption) {
        return res.status(400).json({ success: false, error: 'monumentName and caption are required' });
      }

      const post = await FeedService.createPost(
        user.id,
        user.name,
        monumentName,
        caption,
        postType || 'CHECKIN',
        imageUrl
      );

      res.status(201).json({ success: true, data: post });
    } catch (err) {
      next(err);
    }
  }

  public static async toggleLike(req: Request, res: Response, next: NextFunction) {
    try {
      const { id } = req.params;
      const result = await FeedService.toggleLike(id);
      res.json({ success: true, data: result });
    } catch (err) {
      next(err);
    }
  }
}
