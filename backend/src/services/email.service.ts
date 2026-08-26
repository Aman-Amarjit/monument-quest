import nodemailer from 'nodemailer';
import { supabase } from '../lib/supabase';

// In-memory OTP store: email -> { code, expiresAt }
const otpStore = new Map<string, { code: string; expiresAt: number }>();

function generateOtp(): string {
  return String(Math.floor(100000 + Math.random() * 900000));
}

// Gmail SMTP transporter using authorized App Password
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
    otpStore.set(cleanEmail, { code, expiresAt: Date.now() + 10 * 60 * 1000 });

    // 1. Dispatch real 6-digit OTP email via Gmail SMTP
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

      const info = await transporter.sendMail(mailOptions);
      console.log(`[EmailService] Real Gmail OTP sent to ${cleanEmail}: ${info.messageId}`);
    } catch (err: any) {
      console.error('[EmailService] Gmail SMTP error:', err?.message || err);
      // Secondary fallback to secondary Gmail user if needed
      try {
        const altTransporter = nodemailer.createTransport({
          service: 'gmail',
          auth: { user: 'amanamarjit243222@gmail.com', pass: GMAIL_APP_PASS }
        });
        await altTransporter.sendMail({
          from: `"MonumentQuest" <amanamarjit243222@gmail.com>`,
          to: cleanEmail,
          subject: `${code} — Your MonumentQuest Verification Code`,
          html: `<p>Your code is <b>${code}</b></p>`
        });
      } catch (e) {}
    }

    // 2. Also trigger Supabase auth OTP as secondary backup
    try {
      await supabase.auth.signInWithOtp({ email: cleanEmail });
    } catch (e) {}

    return code;
  }

  static verifyOtp(email: string, code: string): boolean {
    const cleanEmail = email.trim().toLowerCase();
    const cleanCode = code.trim();

    const record = otpStore.get(cleanEmail);
    if (!record) return false;

    if (Date.now() > record.expiresAt) {
      otpStore.delete(cleanEmail);
      return false;
    }

    if (record.code !== cleanCode) {
      return false;
    }

    // Code matched — consume it
    otpStore.delete(cleanEmail);
    return true;
  }
}
