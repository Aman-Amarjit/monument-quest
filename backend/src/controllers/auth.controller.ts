import { Request, Response, NextFunction } from 'express';
import { AuthService } from '../services/auth.service';
import { EmailService } from '../services/email.service';
import { AuthRequest } from '../middleware/auth.middleware';

export class AuthController {
  static async sendOtp(req: Request, res: Response, next: NextFunction) {
    try {
      const { email } = req.body ?? {};
      if (typeof email !== 'string' || !email.includes('@'))
        return res.status(400).json({ success: false, error: 'Valid email required' });
      await EmailService.sendOtp(email.trim().toLowerCase());
      return res.json({ success: true, message: 'OTP sent to ' + email });
    } catch (error) { next(error); }
  }

  static async verifyOtp(req: Request, res: Response, next: NextFunction) {
    try {
      const { email, code } = req.body ?? {};
      if (typeof email !== 'string' || typeof code !== 'string')
        return res.status(400).json({ success: false, error: 'Email and code required' });
      const valid = EmailService.verifyOtp(email.trim().toLowerCase(), code.trim());
      if (!valid) return res.status(400).json({ success: false, error: 'Invalid or expired OTP' });
      return res.json({ success: true, message: 'Email verified' });
    } catch (error) { next(error); }
  }

  static async register(req: Request, res: Response, next: NextFunction) {
    try {
      const { email, password, name, guildName } = req.body ?? {};
      if (typeof email !== 'string' || typeof password !== 'string' || typeof name !== 'string')
        return res.status(400).json({ success: false, error: 'Email, password and name required' });
      return res.status(201).json({ success: true, data: await AuthService.register(email, password, name, typeof guildName === 'string' ? guildName : undefined) });
    } catch (error) { next(error); }
  }

  static async login(req: Request, res: Response, next: NextFunction) {
    try {
      const { email, password } = req.body ?? {};
      if (typeof email !== 'string' || typeof password !== 'string')
        return res.status(400).json({ success: false, error: 'Email and password required' });
      return res.json({ success: true, data: await AuthService.login(email, password) });
    } catch (error) { next(error); }
  }

  static async guest(_req: Request, res: Response, next: NextFunction) {
    try {
      return res.status(201).json({ success: true, data: await AuthService.createGuest() });
    } catch (error) { next(error); }
  }

  static async me(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      return res.json({ success: true, data: await AuthService.getProfile(req.user!.id) });
    } catch (error) { next(error); }
  }
}