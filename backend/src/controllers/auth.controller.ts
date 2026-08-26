import { Request, Response, NextFunction } from 'express';
import { AuthService } from '../services/auth.service';
import { EmailService } from '../services/email.service';
import { AuthRequest } from '../middleware/auth.middleware';

export class AuthController {
  // POST /auth/send-otp
  static async sendOtp(req: Request, res: Response, next: NextFunction) {
    try {
      const { email } = req.body ?? {};
      if (typeof email !== 'string' || !email.includes('@'))
        return res.status(400).json({ success: false, error: 'Valid email required' });
      const otpCode = await EmailService.sendOtp(email.trim().toLowerCase());
      return res.json({ success: true, message: 'OTP sent to ' + email, otpCode });
    } catch (error) { next(error); }
  }

  // POST /auth/login-with-otp — verify database OTP then log in (do NOT delete OTP if 404 signup required)
  static async loginWithOtp(req: Request, res: Response, next: NextFunction) {
    try {
      const { email, code } = req.body ?? {};
      if (typeof email !== 'string' || typeof code !== 'string')
        return res.status(400).json({ success: false, error: 'Email and OTP code required' });

      const valid = await EmailService.verifyOtpAsync(email.trim().toLowerCase(), code.trim(), false);
      if (!valid)
        return res.status(400).json({ success: false, error: 'Invalid or expired OTP code' });

      try {
        const data = await AuthService.loginWithOtp(email);
        // Login succeeded — now consume the OTP code
        await EmailService.verifyOtpAsync(email.trim().toLowerCase(), code.trim(), true);
        return res.json({ success: true, data });
      } catch (error: any) {
        if (error?.status === 404)
          return res.status(404).json({ success: false, error: error.message, needsSignup: true });
        throw error;
      }
    } catch (error: any) {
      if (error?.status === 404)
        return res.status(404).json({ success: false, error: error.message, needsSignup: true });
      next(error);
    }
  }

  // POST /auth/register-with-otp — verify database OTP & consume it then create account
  static async registerWithOtp(req: Request, res: Response, next: NextFunction) {
    try {
      const { email, code, name } = req.body ?? {};
      if (typeof email !== 'string' || typeof code !== 'string' || typeof name !== 'string')
        return res.status(400).json({ success: false, error: 'Email, OTP code, and name required' });

      const valid = await EmailService.verifyOtpAsync(email.trim().toLowerCase(), code.trim(), true);
      if (!valid)
        return res.status(400).json({ success: false, error: 'Invalid or expired OTP code' });

      const data = await AuthService.registerWithOtp(email, name);
      return res.status(201).json({ success: true, data });
    } catch (error: any) {
      if (error?.status === 409)
        return res.status(409).json({ success: false, error: error.message, alreadyExists: true });
      next(error);
    }
  }

  // POST /auth/guest
  static async guest(_req: Request, res: Response, next: NextFunction) {
    try {
      return res.status(201).json({ success: true, data: await AuthService.createGuest() });
    } catch (error) { next(error); }
  }

  // GET /auth/me
  static async me(req: AuthRequest, res: Response, next: NextFunction) {
    try {
      return res.json({ success: true, data: await AuthService.getProfile(req.user!.id) });
    } catch (error) { next(error); }
  }
}