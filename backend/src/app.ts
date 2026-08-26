import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { config } from './config/env';
import router from './routes';
import { errorHandler } from './middleware/error.middleware';

const app = express();

app.use(helmet());
app.use(cors({ origin: config.corsOrigin === '*' ? true : config.corsOrigin.split(',').map((origin) => origin.trim()) }));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

if (config.nodeEnv !== 'test') app.use(morgan(config.nodeEnv === 'development' ? 'dev' : 'combined'));

app.use('/api/v1', router);
app.use((_req, res) => res.status(404).json({ success: false, error: 'Route not found' }));
app.use(errorHandler);

if (require.main === module) {
  app.listen(config.port, () => {
    console.log('MonumentQuest API listening on port ' + config.port);
    console.log('Health check: http://localhost:' + config.port + '/api/v1/health');
  });
}

export default app;
