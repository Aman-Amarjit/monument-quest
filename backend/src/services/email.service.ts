import { supabase } from '../lib/supabase';

// In-memory OTP store: email -> { code, expiresAt }
const otpStore = new Map<string, { code: string; expiresAt: number }>();

function generateOtp(): string {
  return String(Math.floor(100000 + Math.random() * 900000));
}

export class EmailService {
  static async sendOtp(email: string): Promise<string> {
    const cleanEmail = email.trim().toLowerCase();
    const code = generateOtp();
    // Store exact random 6-digit OTP code with 10-minute expiration
    otpStore.set(cleanEmail, { code, expiresAt: Date.now() + 10 * 60 * 1000 });

    try {
      // Send built-in OTP email via Supabase Auth
      const { error } = await supabase.auth.signInWithOtp({
        email: cleanEmail
      });

      if (error) {
        console.error('[EmailService] Supabase Auth OTP error:', error.message);
      } else {
        console.log(`[EmailService] OTP sent to ${cleanEmail}`);
      }
    } catch (err) {
      console.error('[EmailService] Supabase Auth OTP catch:', err);
    }

    return code;
  }

  static verifyOtp(email: string, code: string): boolean {
    const cleanEmail = email.trim().toLowerCase();
    const cleanCode = code.trim();

    const record = otpStore.get(cleanEmail);
    if (!record) return false;

    // Check expiration
    if (Date.now() > record.expiresAt) {
      otpStore.delete(cleanEmail);
      return false;
    }

    // Strictly compare exact OTP code
    if (record.code !== cleanCode) {
      return false;
    }

    // Code matches — consume it so it cannot be reused
    otpStore.delete(cleanEmail);
    return true;
  }
}
