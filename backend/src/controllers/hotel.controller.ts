import { Request, Response, NextFunction } from 'express';
import { AuthRequest } from '../middleware/auth.middleware';
import { HotelService } from '../services/hotel.service';

const number = (value: unknown) => { const parsed = Number(value); return Number.isFinite(parsed) ? parsed : null; };

export class HotelController {
  static async getHotels(req: Request, res: Response, next: NextFunction) {
    try {
      const latitude = number(req.query.latitude ?? req.query.lat);
      const longitude = number(req.query.longitude ?? req.query.lon);
      if (latitude === null || longitude === null || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
        return res.status(400).json({ success: false, error: 'Valid latitude and longitude are required' });
      }
      const hotels = HotelService.nearby({ latitude, longitude });
      return res.json({ hotels, data: hotels });
    } catch (error) { next(error); }
  }

  static async claimVoucher(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const hotelId = req.body?.hotelId;
      if (typeof hotelId !== 'string' || !hotelId.trim()) return res.status(400).json({ success: false, error: 'hotelId is required' });
      return res.json(HotelService.claim(hotelId.trim()));
    } catch (error) { next(error); }
  }
}
