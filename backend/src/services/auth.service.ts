import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { randomUUID } from 'crypto';
import { config } from '../config/env';
import { prisma } from '../lib/prisma';

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  userRank: string;
  points: number;
  role: string;
  guildName: string | null;
  isGuest?: boolean;
}

function rankFor(points: number): string {
  if (points >= 5000) return 'Legendary Pathfinder';
  if (points >= 2500) return 'Master Explorer';
  if (points >= 1000) return 'Temple City Historian';
  return 'Bhubaneswar Explorer';
}

export class AuthService {
  private static toProfile(user: { id: string; email: string; name: string; points: number; role: string; guild?: { name: string } | null }): UserProfile {
    return {
      id: user.id,
      email: user.email,
      name: user.name,
      userRank: rankFor(user.points),
      points: user.points,
      role: user.role,
      guildName: user.guild?.name || null
    };
  }

  private static issueToken(profile: UserProfile): string {
    return jwt.sign(
      { email: profile.email, role: profile.role },
      config.jwtSecret,
      { subject: profile.id, expiresIn: config.jwtExpiresIn as jwt.SignOptions['expiresIn'] }
    );
  }

  public static async register(email: string, password: string, name: string, guildName?: string): Promise<{ token: string; user: UserProfile }> {
    const normalizedEmail = email.trim().toLowerCase();
    const cleanName = name.trim();
    if (!/^[^\s]+@[^\s]+\.[^\s]+$/.test(normalizedEmail)) throw { status: 400, message: 'A valid email is required' };
    if (password.length < 8) throw { status: 400, message: 'Password must be at least 8 characters' };
    if (cleanName.length < 2 || cleanName.length > 80) throw { status: 400, message: 'Name must be between 2 and 80 characters' };

    const existing = await prisma.user.findUnique({ where: { email: normalizedEmail } });
    if (existing) throw { status: 409, message: 'Email already registered' };

    const passwordHash = await bcrypt.hash(password, 12);
    let guildId: string | undefined;
    if (guildName?.trim()) {
      const guild = await prisma.guild.upsert({
        where: { name: guildName.trim() },
        update: {},
        create: { name: guildName.trim(), region: 'Odisha' }
      });
      guildId = guild.id;
    }

    const user = await prisma.user.create({
      data: { email: normalizedEmail, passwordHash, name: cleanName, guildId },
      include: { guild: true }
    });
    const profile = this.toProfile(user);
    return { token: this.issueToken(profile), user: profile };
  }

  public static async login(email: string, password: string): Promise<{ token: string; user: UserProfile }> {
    const normalizedEmail = email.trim().toLowerCase();
    const user = await prisma.user.findUnique({ where: { email: normalizedEmail }, include: { guild: true } });
    if (!user || !(await bcrypt.compare(password, user.passwordHash))) {
      throw { status: 401, message: 'Invalid email or password' };
    }
    const profile = this.toProfile(user);
    return { token: this.issueToken(profile), user: profile };
  }

  public static async createGuest(): Promise<{ token: string; user: UserProfile }> {
    const id = randomUUID();
    const user = await prisma.user.create({
      data: {
        id,
        email: 'guest-' + id + '@guest.monumentquest.app',
        passwordHash: await bcrypt.hash(randomUUID(), 10),
        name: 'Guest Explorer',
        points: 100
      },
      include: { guild: true }
    });
    const profile = { ...this.toProfile(user), isGuest: true };
    return { token: this.issueToken(profile), user: profile };
  }

  public static async getProfile(userId: string): Promise<UserProfile> {
    const user = await prisma.user.findUnique({ where: { id: userId }, include: { guild: true } });
    if (!user) throw { status: 404, message: 'User not found' };
    return this.toProfile(user);
  }
}
