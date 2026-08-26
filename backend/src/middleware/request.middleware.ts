import { NextFunction, Request, Response } from 'express';
import { randomUUID } from 'crypto';
import { config } from '../config/env';
import { prisma } from '../lib/prisma';

type Bucket = { count: number; resetAt: number };
type LimitResult = { count: number; resetAt: number; allowed: boolean };
const buckets = new Map<string, Bucket>();

function localLimit(key: string, max: number, windowMs: number, now = Date.now()): LimitResult {
  const current = buckets.get(key);
  if (!current || current.resetAt <= now) {
    const next = { count: 1, resetAt: now + windowMs };
    buckets.set(key, next);
    return { ...next, allowed: true };
  }
  current.count += 1;
  return { ...current, allowed: current.count <= max };
}

async function databaseLimit(key: string, max: number, windowMs: number): Promise<LimitResult> {
  const resetAt = new Date(Date.now() + windowMs);
  const rows = await prisma.$queryRaw<Array<{ count: number; resetAt: Date }>>`
    INSERT INTO "RateLimitBucket" ("key", "count", "resetAt")
    VALUES (${key}, 1, ${resetAt})
    ON CONFLICT ("key") DO UPDATE SET
      "count" = CASE
        WHEN "RateLimitBucket"."resetAt" <= NOW() THEN 1
        ELSE "RateLimitBucket"."count" + 1
      END,
      "resetAt" = CASE
        WHEN "RateLimitBucket"."resetAt" <= NOW() THEN EXCLUDED."resetAt"
        ELSE "RateLimitBucket"."resetAt"
      END
    RETURNING "count", "resetAt"
  `;
  const bucket = rows[0];
  if (!bucket) throw new Error('Rate limit bucket was not returned');
  const resetTime = bucket.resetAt instanceof Date ? bucket.resetAt.getTime() : new Date(bucket.resetAt).getTime();
  return { count: bucket.count, resetAt: resetTime, allowed: bucket.count <= max };
}

function setLimitHeaders(res: Response, max: number, result: LimitResult): void {
  res.setHeader('x-ratelimit-limit', String(max));
  res.setHeader('x-ratelimit-remaining', String(Math.max(0, max - result.count)));
  res.setHeader('x-ratelimit-reset', String(Math.ceil(result.resetAt / 1000)));
}

export function requestId(req: Request, res: Response, next: NextFunction) {
  const id = req.header('x-request-id')?.slice(0, 80) || randomUUID();
  res.setHeader('x-request-id', id);
  (req as Request & { requestId?: string }).requestId = id;
  next();
}

export function rateLimit(options: { max?: number; windowMs?: number; key?: string } = {}) {
  const max = options.max ?? config.rateLimitMax;
  const windowMs = options.windowMs ?? config.rateLimitWindowMs;
  const prefix = options.key || 'api';

  return async (req: Request, res: Response, next: NextFunction) => {
    const identity = req.ip || req.socket.remoteAddress || 'unknown';
    const key = prefix + ':' + identity;
    let result: LimitResult;

    try {
      result = config.databaseUrl
        ? await databaseLimit(key, max, windowMs)
        : localLimit(key, max, windowMs);
    } catch (error) {
      // Keep the API available during a short database outage; production uses
      // the shared bucket whenever PostgreSQL is healthy.
      console.error('Distributed rate limiter unavailable; using local fallback', error);
      result = localLimit(key, max, windowMs);
    }

    setLimitHeaders(res, max, result);
    if (!result.allowed) {
      res.setHeader('retry-after', Math.max(1, Math.ceil((result.resetAt - Date.now()) / 1000)));
      return res.status(429).json({ success: false, error: 'Too many requests. Please try again shortly.' });
    }
    return next();
  };
}

const intervalTimer = setInterval(() => {
  const now = Date.now();
  for (const [key, bucket] of buckets) if (bucket.resetAt <= now) buckets.delete(key);
}, 10 * 60 * 1000);
if (intervalTimer && typeof intervalTimer.unref === 'function') {
  intervalTimer.unref();
}
