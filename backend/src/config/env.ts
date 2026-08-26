import dotenv from 'dotenv';

dotenv.config();

const nodeEnv = process.env.NODE_ENV || 'development';
const isProduction = nodeEnv === 'production';
const jwtSecret = process.env.JWT_SECRET || (isProduction ? '' : 'local-only-monument-quest-secret-change-me');
const databaseUrl = process.env.DATABASE_URL || '';

if (isProduction && jwtSecret.length < 32) throw new Error('JWT_SECRET must be at least 32 characters in production');
if (isProduction && !databaseUrl.startsWith('postgres')) throw new Error('DATABASE_URL must point to PostgreSQL in production');
function integer(name: string, fallback: number, min: number, max: number): number { const value = Number(process.env[name] || fallback); return Number.isInteger(value) ? Math.min(Math.max(value, min), max) : fallback; }

export const config = {
  port: integer('PORT', 3000, 1, 65535), nodeEnv, isProduction, databaseUrl, jwtSecret,
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '7d',
  corsOrigin: process.env.CORS_ORIGIN || (isProduction ? '' : '*'),
  trustProxy: process.env.TRUST_PROXY === 'true', groqApiKey: process.env.GROQ_API_KEY || '',
  rateLimitWindowMs: integer('RATE_LIMIT_WINDOW_MS', 60000, 1000, 3600000),
  rateLimitMax: integer('RATE_LIMIT_MAX', 120, 10, 10000), authRateLimitMax: integer('AUTH_RATE_LIMIT_MAX', 10, 3, 1000), aiRateLimitMax: integer('AI_RATE_LIMIT_MAX', 30, 3, 1000)
};
export function allowedOrigins(): string[] | '*' { if (config.corsOrigin === '*') return '*'; return config.corsOrigin.split(',').map((origin) => origin.trim()).filter(Boolean); }