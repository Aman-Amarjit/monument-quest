import { Request, Response, NextFunction } from 'express';
import { MonumentService } from '../services/monument.service';
import { AuthRequest } from '../middleware/auth.middleware';

function coordinate(value: unknown): number | null {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export class MonumentController {
  public static async getMonuments(req: Request, res: Response, next: NextFunction) {
    try {
      const lat = coordinate(req.query.latitude);
      const lon = coordinate(req.query.longitude);
      const radius = coordinate(req.query.radiusMeters);
      const monuments = lat !== null && lon !== null
        ? await MonumentService.getNearbyMonuments({ latitude: lat, longitude: lon }, radius === null ? 50000 : Math.min(Math.max(radius, 100), 100000))
        : await MonumentService.getMonuments();
      return res.json({ success: true, count: monuments.length, data: monuments });
    } catch (err) {
      return next(err);
    }
  }

  public static async getNearby(req: Request, res: Response, next: NextFunction) {
    try {
      const latitude = coordinate(req.query.latitude);
      const longitude = coordinate(req.query.longitude);
      if (latitude === null || longitude === null || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
        return res.status(400).json({ success: false, error: 'Valid latitude and longitude are required' });
      }
      const radius = coordinate(req.query.radiusMeters) ?? 50000;
      const monuments = await MonumentService.getNearbyMonuments({ latitude, longitude }, Math.min(Math.max(radius, 100), 100000));
      return res.json({ success: true, count: monuments.length, data: monuments });
    } catch (err) {
      return next(err);
    }
  }

  public static async getOne(req: Request, res: Response, next: NextFunction) {
    try {
      return res.json({ success: true, data: await MonumentService.getMonument(req.params.id) });
    } catch (err) {
      return next(err);
    }
  }

  public static async captureMonument(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const { monumentId, name, latitude: rawLatitude, longitude: rawLongitude, imageUrl } = req.body;
      const latitude = coordinate(rawLatitude);
      const longitude = coordinate(rawLongitude);
      if (latitude === null || longitude === null || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
        return res.status(400).json({ success: false, error: 'Valid latitude and longitude are required' });
      }
      if (typeof monumentId !== 'string' && typeof name !== 'string') {
        return res.status(400).json({ success: false, error: 'monumentId or monument name is required' });
      }

      const result = await MonumentService.captureMonument(req.user!.id, typeof monumentId === 'string' ? monumentId : undefined, { latitude, longitude }, typeof name === 'string' ? name : undefined, typeof imageUrl === 'string' ? imageUrl : undefined);
      return res.json({ success: true, data: result });
    } catch (err) {
      return next(err);
    }
  }
}
