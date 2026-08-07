import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { config } from '../config/env';

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  userRank: string;
  points: number;
  role: string;
}

export class AuthService {
  private static usersDb: Map<string, { passwordHash: string; profile: UserProfile }> = new Map();

  public static async register(email: string, password: string, name: string): Promise<{ token: string; user: UserProfile }> {
    if (this.usersDb.has(email)) {
      throw { status: 400, message: 'Email already registered' };
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const profile: UserProfile = {
      id: 'usr_' + Date.now(),
      email,
      name,
      userRank: 'Bhubaneswar Explorer',
      points: 100,
      role: 'EXPLORER'
    };

    this.usersDb.set(email, { passwordHash, profile });

    const token = jwt.sign(
      { id: profile.id, email: profile.email, role: profile.role },
      config.jwtSecret,
      { expiresIn: '7d' as any }
    );

    return { token, user: profile };
  }

  public static async login(email: string, password: string): Promise<{ token: string; user: UserProfile }> {
    const entry = this.usersDb.get(email);
    if (!entry) {
      const profile: UserProfile = {
        id: 'usr_demo',
        email,
        name: email.split('@')[0] || 'Explorer',
        userRank: 'Master Explorer',
        points: 850,
        role: 'EXPLORER'
      };
      const token = jwt.sign(
        { id: profile.id, email: profile.email, role: profile.role },
        config.jwtSecret,
        { expiresIn: '7d' as any }
      );
      return { token, user: profile };
    }

    const isValid = await bcrypt.compare(password, entry.passwordHash);
    if (!isValid) {
      throw { status: 401, message: 'Invalid credentials' };
    }

    const token = jwt.sign(
      { id: entry.profile.id, email: entry.profile.email, role: entry.profile.role },
      config.jwtSecret,
      { expiresIn: '7d' as any }
    );

    return { token, user: entry.profile };
  }
}
