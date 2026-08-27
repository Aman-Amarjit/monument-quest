import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { config, allowedOrigins } from './config/env';
import router from './routes';
import { errorHandler } from './middleware/error.middleware';
import { requestId } from './middleware/request.middleware';
import { disconnectPrisma, prisma } from './lib/prisma';

const app = express();
app.disable('x-powered-by');
if (config.trustProxy) app.set('trust proxy', 1);

app.use(helmet());
app.use(cors({
  origin: allowedOrigins() === '*' ? true : allowedOrigins(),
  methods: ['GET', 'POST', 'PATCH', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Request-Id']
}));
app.use(requestId);
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: false, limit: '100kb' }));

if (config.nodeEnv !== 'test') {
  app.use(morgan(config.nodeEnv === 'development' ? 'dev' : 'combined'));
}

// Favicon handlers for clean browser & Vercel log metrics
app.get('/favicon.ico', (_req, res) => res.status(204).end());
app.get('/favicon.png', (_req, res) => res.status(204).end());

// Root welcome route for browser & health monitoring
app.get('/', (_req, res) => res.json({
  status: 'online',
  service: 'MonumentQuest Production API',
  version: '3.0.0',
  database: 'Supabase PostgreSQL (Connected)',
  timestamp: new Date().toISOString()
}));

app.get('/health/live', (_req, res) => res.json({ status: 'ok', service: 'monument-quest-api' }));
app.get('/health/ready', async (_req, res) => {
  try {
    await prisma.$queryRawUnsafe('SELECT 1');
    res.json({ status: 'ready', database: 'ok' });
  } catch {
    res.status(503).json({ status: 'not_ready', database: 'unavailable' });
  }
});

// Mount router on all path prefixes to guarantee Vercel route matching
app.use('/api/v1', router);
app.use('/api', router);
app.use('/', router);

app.use((req, res) => res.status(404).json({ success: false, error: 'Route not found: ' + req.method + ' ' + req.url }));
app.use(errorHandler);

if (require.main === module) {
  const server = app.listen(config.port, '0.0.0.0', () =>
    console.log('MonumentQuest API listening on port ' + config.port)
  );
  const shutdown = async (signal: string) => {
    console.log(signal + ': shutting down');
    server.close(async () => {
      await disconnectPrisma();
      process.exit(0);
    });
  };
  process.once('SIGTERM', () => void shutdown('SIGTERM'));
  process.once('SIGINT', () => void shutdown('SIGINT'));
}

export default app;