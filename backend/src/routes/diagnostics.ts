import { Router } from 'express';
import { prisma } from '../lib/prisma';
import { config } from '../config/env';
import os from 'os';

const router = Router();

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}

function detectDbProvider(url: string): string {
  if (!url) return 'Not configured';
  const lower = url.toLowerCase();
  if (lower.includes('supabase')) return 'Supabase PostgreSQL';
  if (lower.includes('neon')) return 'Neon PostgreSQL';
  if (lower.includes('railway')) return 'Railway PostgreSQL';
  if (lower.includes('render')) return 'Render PostgreSQL';
  if (lower.includes('vercel')) return 'Vercel Postgres';
  if (lower.includes('localhost') || lower.includes('127.0.0.1')) return 'Local PostgreSQL';
  if (lower.startsWith('postgres://') || lower.startsWith('postgresql://')) return 'PostgreSQL';
  return 'Database';
}

router.get('/', async (_req, res) => {
  const startTime = Date.now();
  let dbConnected = false;
  let dbPingMs = -1;
  
  try {
    const pingStart = Date.now();
    await prisma.$queryRawUnsafe('SELECT 1');
    dbPingMs = Date.now() - pingStart;
    dbConnected = true;
  } catch (error) {
    dbConnected = false;
    dbPingMs = -1;
  }

  const memUsage = process.memoryUsage();
  const totalMem = os.totalmem();
  const freeMem = os.freemem();
  const usedMemPercent = Math.round(((totalMem - freeMem) / totalMem) * 100);

  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: Math.floor(process.uptime()),
    uptimeFormatted: `${Math.floor(process.uptime() / 3600)}h ${Math.floor((process.uptime() % 3600) / 60)}m ${Math.floor(process.uptime() % 60)}s`,
    platform: process.platform,
    arch: process.arch,
    nodeVersion: process.version,
    pid: process.pid,
    hostname: os.hostname(),
    memory: {
      rss: formatBytes(memUsage.rss),
      heapTotal: formatBytes(memUsage.heapTotal),
      heapUsed: formatBytes(memUsage.heapUsed),
      external: formatBytes(memUsage.external),
      total: formatBytes(totalMem),
      free: formatBytes(freeMem),
      usedPercent: usedMemPercent
    },
    env: {
      nodeEnv: config.nodeEnv,
      port: config.port,
      trustProxy: config.trustProxy,
      hasDb: Boolean(config.databaseUrl),
      hasGroq: Boolean(config.groqApiKey),
      corsOrigin: config.corsOrigin,
      dbProvider: detectDbProvider(config.databaseUrl)
    },
    db: {
      connected: dbConnected,
      pingMs: dbPingMs
    },
    version: '3.0.0'
  });
});

export default router;
