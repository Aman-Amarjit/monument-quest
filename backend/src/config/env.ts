import dotenv from 'dotenv';
import { randomBytes } from 'crypto';

dotenv.config();
const nodeEnv = process.env.NODE_ENV || 'development';
const isProduction = nodeEnv === 'production';
const configuredJwtSecret = process.env.JWT_SECRET?.trim();
if (!configuredJwtSecret || configuredJwtSecret.length < 16) {
  if (nodeEnv === 'production') {
    throw new Error('FATAL: JWT_SECRET environment variable is not set or is too short. Set a strong secret before deploying.');
  }
  console.warn('[WARN] JWT_SECRET is not set. Using an insecure development fallback. DO NOT use this in production.');
}
const jwtSecret = (configuredJwtSecret && configuredJwtSecret.length >= 16)
  ? configuredJwtSecret
  : 'monument_quest_dev_only_jwt_secret_not_for_production';
const databaseUrl = process.env.DATABASE_URL?.trim() || '';
function integer(name: string, fallback: number, min: number, max: number): number { const value = Number(process.env[name] || fallback); return Number.isInteger(value) ? Math.min(Math.max(value, min), max) : fallback; }
export const config = {
  port: integer('PORT', 3000, 1, 65535), nodeEnv, isProduction, databaseUrl, jwtSecret,
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '7d', corsOrigin: process.env.CORS_ORIGIN || '*', trustProxy: process.env.TRUST_PROXY === 'true', groqApiKey: process.env.GROQ_API_KEY || '',
  rateLimitWindowMs: integer('RATE_LIMIT_WINDOW_MS', 60000, 1000, 3600000), rateLimitMax: integer('RATE_LIMIT_MAX', 120, 10, 10000), authRateLimitMax: integer('AUTH_RATE_LIMIT_MAX', 10, 3, 1000), aiRateLimitMax: integer('AI_RATE_LIMIT_MAX', 30, 3, 1000)
};
export function allowedOrigins(): string[] | '*' { if (config.corsOrigin === '*') return '*'; return config.corsOrigin.split(',').map((origin) => origin.trim()).filter(Boolean); }
