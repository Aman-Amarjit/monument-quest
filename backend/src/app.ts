import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { config } from './config/env';
import router from './routes';
import { errorHandler } from './middleware/error.middleware';

const app = express();

// Security & Utility Middlewares
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

if (config.nodeEnv === 'development') {
  app.use(morgan('dev'));
}

// API Routes
app.use('/api/v1', router);

// Centralized Error Handling
app.use(errorHandler);

// Start Server
app.listen(config.port, () => {
  console.log(`=======================================================`);
  console.log(`🚀 MonumentQuest Production API Backend Running`);
  console.log(`📡 Port: ${config.port}`);
  console.log(`🌍 Mode: ${config.nodeEnv}`);
  console.log(`🔗 Health Check: http://localhost:${config.port}/api/v1/health`);
  console.log(`=======================================================`);
});

export default app;
