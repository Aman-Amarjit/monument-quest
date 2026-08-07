import { Router } from 'express';
import { AuthController } from '../controllers/auth.controller';
import { MonumentController } from '../controllers/monument.controller';
import { FeedController } from '../controllers/feed.controller';
import { AIController } from '../controllers/ai.controller';
import { authenticateToken } from '../middleware/auth.middleware';

const router = Router();

// Health Check
router.get('/health', (req, res) => {
  res.json({
    status: 'online',
    version: '1.0.0',
    service: 'MonumentQuest Production API',
    timestamp: new Date().toISOString()
  });
});

// Authentication Routes
router.post('/auth/register', AuthController.register);
router.post('/auth/login', AuthController.login);

// Monument & Geospatial Capture Routes
router.get('/monuments', MonumentController.getMonuments);
router.post('/monuments/capture', authenticateToken, MonumentController.captureMonument);

// Social Media Feed Routes
router.get('/feed', FeedController.getFeed);
router.post('/feed/posts', authenticateToken, FeedController.createPost);
router.post('/feed/posts/:id/like', authenticateToken, FeedController.toggleLike);

// AI Narrator & Journalist Routes
router.post('/ai/narrator', AIController.talkToNarrator);
router.post('/ai/verify-reflection', AIController.verifyReflection);

export default router;
