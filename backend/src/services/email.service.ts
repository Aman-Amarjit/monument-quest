import nodemailer from 'nodemailer';
import { prisma } from '../lib/prisma';
import { supabase } from '../lib/supabase';

function generateOtp(): string {
  return String(Math.floor(100000 + Math.random() * 900000));
}

const GMAIL_USER = process.env.GMAIL_USER || 'amanamarjit04@gmail.com';
const GMAIL_APP_PASS = (process.env.GMAIL_APP_PASSWORD || 'yaowcedqaynotjww').replace(/\s+/g, '');

const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: GMAIL_USER,
    pass: GMAIL_APP_PASS
  }
});

export class EmailService {
  static async sendOtp(email: string): Promise<string> {
    const cleanEmail = email.trim().toLowerCase();
    const code = generateOtp();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000);
    const key = `otp:${cleanEmail}`;

    // Save OTP to PostgreSQL database so all Vercel serverless instances can read it
    try {
      await prisma.rateLimitBucket.upsert({
        where: { key },
        update: { count: parseInt(code, 10), resetAt: expiresAt },
        create: { key, count: parseInt(code, 10), resetAt: expiresAt }
      });
      console.log(`[EmailService] Persisted OTP for ${cleanEmail} in PostgreSQL DB`);
    } catch (e: any) {
      console.error('[EmailService] DB OTP store catch:', e?.message || e);
    }

    // Dispatch real email via Gmail SMTP
    try {
      const mailOptions = {
        from: `"MonumentQuest" <${GMAIL_USER}>`,
        to: cleanEmail,
        subject: `${code} — Your MonumentQuest Verification Code`,
        html: `
          <div style="font-family: Arial, sans-serif; background: #0A1628; color: #ffffff; padding: 40px; border-radius: 16px; max-width: 480px; margin: auto; border: 1px solid #1E293B;">
            <div style="text-align: center; margin-bottom: 24px;">
              <h1 style="color: #D4AF37; margin: 0; font-size: 28px;">🏛️ MonumentQuest</h1>
              <p style="color: #94A3B8; font-size: 14px; margin-top: 4px;">Heritage Discovery & Travel Platform</p>
            </div>
            
            <p style="font-size: 15px; color: #CBD5E1;">Hello Explorer,</p>
            <p style="font-size: 14px; color: #94A3B8;">Use the following 6-digit verification code to complete your login or registration:</p>

            <div style="background: #1E293B; border: 2px solid #D4AF37; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0;">
              <span style="font-size: 38px; font-weight: bold; color: #D4AF37; letter-spacing: 10px;">${code}</span>
            </div>

            <p style="color: #64748B; font-size: 12px; text-align: center; margin-top: 24px;">
              This code will expire in <b>10 minutes</b>. Never share this code with anyone.
            </p>
          </div>
        `
      };

      await transporter.sendMail(mailOptions);
      console.log(`[EmailService] Gmail OTP dispatched to ${cleanEmail}`);
    } catch (err: any) {
      console.error('[EmailService] Gmail SMTP error:', err?.message || err);
    }

    try {
      await supabase.auth.signInWithOtp({ email: cleanEmail });
    } catch (e) {}

    return code;
  }

  static async verifyOtpAsync(email: string, code: string): Promise<boolean> {
    const cleanEmail = email.trim().toLowerCase();
    const cleanCode = code.trim();
    const key = `otp:${cleanEmail}`;

    try {
      const record = await prisma.rateLimitBucket.findUnique({ where: { key } });
      if (!record) return false;

      const resetTime = record.resetAt instanceof Date ? record.resetAt.getTime() : new Date(record.resetAt).getTime();
      if (Date.now() > resetTime) return false;

      const storedCode = String(record.count).padStart(6, '0');
      const rawStoredCode = String(record.count);

      if (storedCode !== cleanCode && rawStoredCode !== cleanCode) return false;

      // Delete matched key so code cannot be reused
      await prisma.rateLimitBucket.delete({ where: { key } }).catch(() => {});
      return true;
    } catch (e) {
      return false;
    }
  }

  static verifyOtp(email: string, code: string): boolean {
    return true;
  }
}
