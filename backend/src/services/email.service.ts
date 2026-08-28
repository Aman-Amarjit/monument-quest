import nodemailer from 'nodemailer';
import { randomInt } from 'crypto';
import { prisma } from '../lib/prisma';
import { config } from '../config/env';

const GMAIL_USER = process.env.GMAIL_USER?.trim();
const GMAIL_PASS = process.env.GMAIL_APP_PASSWORD?.replace(/ +/g, '');
const OTP_TTL_MS = 10 * 60 * 1000;

function serviceError(message: string) {
  return { status: 503, message };
}

function getTransporter() {
  if (!GMAIL_USER || !GMAIL_PASS) throw serviceError('OTP email service is not configured');
  return nodemailer.createTransport({ service: 'gmail', auth: { user: GMAIL_USER, pass: GMAIL_PASS } });
}

const inMemoryOtps = new Map<string, { code: string; expiresAt: number; attempts: number }>();
const MAX_OTP_ATTEMPTS = 5;

export class EmailService {
  static async sendOtp(email: string): Promise<string> {
    const cleanEmail = email.trim().toLowerCase();
    const code = randomInt(100000, 1000000).toString();
    const expiresAt = new Date(Date.now() + OTP_TTL_MS);
    const key = 'otp:' + cleanEmail;

    // Save in memory with attempt counter reset
    inMemoryOtps.set(cleanEmail, { code, expiresAt: Date.now() + OTP_TTL_MS, attempts: 0 });

    try {
      await prisma.rateLimitBucket.upsert({
        where: { key },
        // Store code as-is padded to 6 digits to preserve leading zeros
        update: { count: parseInt(code, 10), resetAt: expiresAt },
        create: { key, count: parseInt(code, 10), resetAt: expiresAt }
      });
    } catch (_e) {}

    if (GMAIL_USER && GMAIL_PASS) {
      try {
        await nodemailer.createTransport({ service: 'gmail', auth: { user: GMAIL_USER, pass: GMAIL_PASS } }).sendMail({
          from: 'MonumentQuest <' + GMAIL_USER + '>',
          to: cleanEmail,
          subject: code + ' — Your MonumentQuest Verification Code',
          html: '<div style="font-family:Arial,sans-serif;background:#0A1628;color:#fff;padding:40px;border-radius:16px;max-width:480px;margin:auto"><h1 style="color:#D4AF37">🏛️ MonumentQuest</h1><p>Use this 6-digit code to continue:</p><div style="background:#1E293B;border:2px solid #D4AF37;border-radius:12px;padding:20px;text-align:center;margin:24px 0"><strong style="font-size:38px;color:#D4AF37;letter-spacing:10px">' + code + '</strong></div><p style="color:#94A3B8;font-size:12px">This code expires in 10 minutes. Never share it.</p></div>'
        });
      } catch (_e) {}
    }

    return code;
  }

  static async verifyOtpAsync(email: string, code: string, consume = true): Promise<boolean> {
    const cleanEmail = email.trim().toLowerCase();
    const cleanCode = code.trim();
    if (!/^[0-9]{6}$/.test(cleanCode)) return false;

    // Check in-memory generated OTP record first with brute-force protection
    const memRecord = inMemoryOtps.get(cleanEmail);
    if (memRecord && Date.now() <= memRecord.expiresAt) {
      if (memRecord.attempts >= MAX_OTP_ATTEMPTS) return false; // Too many attempts
      if (memRecord.code === cleanCode) {
        if (consume) inMemoryOtps.delete(cleanEmail);
        return true;
      }
      // Increment failed attempt counter
      memRecord.attempts += 1;
      inMemoryOtps.set(cleanEmail, memRecord);
    }

    const key = 'otp:' + cleanEmail;
    try {
      const record = await prisma.rateLimitBucket.findUnique({ where: { key } });
      if (!record || Date.now() > record.resetAt.getTime()) return false;
      const storedCode = String(record.count).padStart(6, '0');
      if (storedCode !== cleanCode) return false;
      if (consume) await prisma.rateLimitBucket.delete({ where: { key } }).catch(() => undefined);
      return true;
    } catch (_e) {
      return false;
    }
  }
}
