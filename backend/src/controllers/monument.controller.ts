import { Request, Response, NextFunction } from 'express';
import { MonumentService } from '../services/monument.service';

export class MonumentController {
  public static async getMonuments(req: Request, res: Response, next: NextFunction) {
    try {
      const monuments = MonumentService.getBhubaneswarMonuments();
      res.json({ success: true, count: monuments.length, data: monuments });
    } catch (err) {
      next(err);
    }
  }

  public static async captureMonument(req: Request, res: Response, next: NextFunction) {
    try {
      const { monumentId, name, latitude, longitude, imageUrl } = req.body;
      const userId = (req as any).user?.id || 'usr_demo';

      if (latitude === undefined || longitude === undefined) {
        return res.status(400).json({ success: false, error: 'Latitude and longitude are required' });
      }

      const result = await MonumentService.captureMonument(
        userId,
        monumentId,
        { latitude, longitude },
        name,
        imageUrl
      );

      res.json({ success: true, data: result });
    } catch (err) {
      next(err);
    }
  }
}
