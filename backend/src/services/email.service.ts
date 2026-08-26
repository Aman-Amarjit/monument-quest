import { supabase } from '../lib/supabase';

// In-memory OTP store: email -> { code, expiresAt }
const otpStore = new Map<string, { code: string; expiresAt: number }>();

function generateOtp(): string {
  return String(Math.floor(100000 + Math.random() * 900000));
}

const HARDCODED_RESEND_KEY = 're_jfKDehjR_DEvS45UtNV1eYJ83sprevo95';

export class EmailService {
  static async sendOtp(email: string): Promise<string> {
    const cleanEmail = email.trim().toLowerCase();
    const code = generateOtp();
    otpStore.set(cleanEmail, { code, expiresAt: Date.now() + 10 * 60 * 1000 });

    const resendApiKey = process.env.RESEND_API_KEY || HARDCODED_RESEND_KEY;

    // 1. Dispatch real email via Resend API
    try {
      const res = await fetch('https://api.resend.com/emails', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${resendApiKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          from: 'MonumentQuest <onboarding@resend.dev>',
          to: [cleanEmail],
          subject: `${code} — Your MonumentQuest Verification Code`,
          html: `
            <div style="font-family:Arial,sans-serif;background:#0A1628;color:#fff;padding:40px;border-radius:12px;max-width:480px;margin:auto">
              <h2 style="color:#D4AF37">🏛️ MonumentQuest</h2>
              <p style="color:#94A3B8">Heritage Discovery Platform</p>
              <p>Your verification code is:</p>
              <div style="background:#1B2A33;border:2px solid #D4AF37;border-radius:12px;padding:24px;text-align:center;margin:20px 0">
                <span style="font-size:40px;font-weight:bold;color:#D4AF37;letter-spacing:12px">${code}</span>
              </div>
              <p style="color:#94A3B8;font-size:13px">Expires in <b>10 minutes</b>. Never share this code.</p>
            </div>`
        })
      });

      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        console.log(`[EmailService] Resend email dispatched to ${cleanEmail}`);
      } else {
        console.error(`[EmailService] Resend API notice for ${cleanEmail}:`, data);
      }
    } catch (err) {
      console.error('[EmailService] Resend fetch error:', err);
    }

    // 2. Also trigger Supabase built-in auth OTP
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

    otpStore.delete(cleanEmail);
    return true;
  }
}
