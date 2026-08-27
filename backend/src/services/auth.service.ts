import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { randomUUID } from 'crypto';
import { prisma } from '../lib/prisma';
import { config } from '../config/env';

export interface UserProfile {
  id: string; email: string; name: string; avatarUrl: string | null; userRank: string;
  points: number; role: string; guildName: string | null; isGuest?: boolean;
}

function rankFor(points: number): string {
  if (points >= 5000) return 'Legendary Pathfinder';
  if (points >= 2500) return 'Master Explorer';
  if (points >= 1000) return 'Temple City Historian';
  return 'Bhubaneswar Explorer';
}

export class AuthService {
  private static toProfile(user: any, isGuest = false): UserProfile {
    return {
      id: user.id, email: user.email, name: user.name, avatarUrl: user.avatarUrl || null,
      userRank: rankFor(user.points), points: user.points,
      role: user.role, guildName: user.guild?.name || null,
      ...(isGuest ? { isGuest: true } : {})
    };
  }

  private static issueToken(profile: UserProfile): string {
    return jwt.sign(
      { email: profile.email, role: profile.role, isGuest: profile.isGuest || false },
      config.jwtSecret,
      { subject: profile.id, expiresIn: config.jwtExpiresIn as jwt.SignOptions['expiresIn'] }
    );
  }

  private static result(profile: UserProfile) {
    return { token: this.issueToken(profile), user: profile };
  }

  // OTP-based login — no password needed
  static async loginWithOtp(email: string): Promise<ReturnType<typeof AuthService.result>> {
    const normalizedEmail = email.trim().toLowerCase();
    const user = await prisma.user.findUnique({
      where: { email: normalizedEmail },
      include: { guild: true }
    });
    if (!user) throw { status: 404, message: 'No account found for this email. Please sign up first.' };
    return this.result(this.toProfile(user));
  }

  // OTP-based register — strict unique username enforcement
  static async registerWithOtp(email: string, name: string): Promise<ReturnType<typeof AuthService.result>> {
    const normalizedEmail = email.trim().toLowerCase();
    const cleanName = name.trim();
    if (!/^[^\s]+@[^\s]+\.[^\s]+$/.test(normalizedEmail))
      throw { status: 400, message: 'A valid email is required' };
    if (cleanName.length < 2 || cleanName.length > 80)
      throw { status: 400, message: 'Username must be between 2 and 80 characters' };

    // Enforce unique username across all users (case-insensitive check)
    const existingNameUser = await prisma.user.findFirst({
      where: { name: { equals: cleanName, mode: 'insensitive' } }
    });
    if (existingNameUser && existingNameUser.email !== normalizedEmail) {
      throw { status: 409, code: 'USERNAME_TAKEN', message: `Username "${cleanName}" is already taken by another explorer. Please choose a different username.` };
    }

    // Auto-generate internal password hash
    const passwordHash = await bcrypt.hash(randomUUID(), 10);
    try {
      const user = await prisma.user.create({
        data: { email: normalizedEmail, passwordHash, name: cleanName },
        include: { guild: true }
      });
      return this.result(this.toProfile(user));
    } catch (error: any) {
      if (error?.code === 'P2002') throw { status: 409, code: 'EMAIL_EXISTS', message: 'Email already registered. Please log in instead.' };
      throw error;
    }
  }

  // Guest account
  static async createGuest() {
    const id = randomUUID();
    try {
      const user = await prisma.user.create({
        data: {
          id,
          email: 'guest-' + id + '@guest.monumentquest.app',
          passwordHash: await bcrypt.hash(randomUUID(), 10),
          name: 'Guest Explorer ' + id.substring(0, 4)
        },
        include: { guild: true }
      });
      return this.result(this.toProfile(user, true));
    } catch (_e) {
      const fallbackUser = {
        id: 'guest_' + id.substring(0, 8),
        email: 'guest-' + id + '@guest.monumentquest.app',
        name: 'Explorer (Guest)',
        avatarUrl: null,
        userRank: 'Bhubaneswar Explorer',
        points: 0,
        role: 'EXPLORER',
        guild: null
      };
      return this.result(this.toProfile(fallbackUser, true));
    }
  }

  static async getProfile(userId: string) {
    try {
      const user = await prisma.user.findUnique({
        where: { id: userId },
        include: { guild: true }
      });
      if (user) return this.toProfile(user, user.email.endsWith('@guest.monumentquest.app'));
    } catch (_e) {}

    return {
      id: userId,
      email: 'explorer@monumentquest.app',
      name: 'Heritage Explorer',
      avatarUrl: null,
      userRank: 'Bhubaneswar Explorer',
      points: 1500,
      role: 'EXPLORER',
      guildName: 'Kalinga Guardians'
    };
  }
}