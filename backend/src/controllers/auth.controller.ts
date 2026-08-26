import { Request, Response, NextFunction } from 'express';
import { AuthService } from '../services/auth.service';
import { AuthRequest } from '../middleware/auth.middleware';

export class AuthController {
  public static async register(req: Request, res: Response, next: NextFunction) {
    try {
      const { email, password, name, guildName } = req.body;
      if (typeof email !== 'string' || typeof password !== 'string' || typeof name !== 'string') {
        return res.status(400).json({ success: false, error: 'Email, password, and name are required' });
      }
      const result = await AuthService.register(email, password, name, typeof guildName === 'string' ? guildName : undefined);
      return res.status(201).json({ success: true, data: result });
    } catch (err) {
      return next(err);
    }
  }

  public static async login(req: Request, res: Response, next: NextFunction) {
    try {
      const { email, password } = req.body;
      if (typeof email !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ success: false, error: 'Email and password are required' });
      }
      const result = await AuthService.login(email, password);
      return res.json({ success: true, data: result });
    } catch (err) {
      return next(err);
    }
  }

  public static async guest(_req: Request, res: Response, next: NextFunction) {
    try {
      const result = await AuthService.createGuest();
      return res.status(201).json({ success: true, data: result });
    } catch (err) {
      return next(err);
    }
  }

  public static async me(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      const user = await AuthService.getProfile(req.user!.id);
      return res.json({ success: true, data: user });
    } catch (err) {
      return next(err);
    }
  }
}
