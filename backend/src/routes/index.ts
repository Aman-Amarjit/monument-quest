import { Router } from 'express';
import { AuthController } from '../controllers/auth.controller';
import { MonumentController } from '../controllers/monument.controller';
import { FeedController } from '../controllers/feed.controller';
import { AIController } from '../controllers/ai.controller';
import { authenticateToken, optionalAuthenticateToken } from '../middleware/auth.middleware';

const router = Router();

router.get('/health', (_req, res) => {
  res.json({ status: 'online', version: '2.0.0', service: 'MonumentQuest API', timestamp: new Date().toISOString() });
});

router.post('/auth/register', AuthController.register);
router.post('/auth/login', AuthController.login);
router.post('/auth/guest', AuthController.guest);
router.get('/auth/me', authenticateToken, AuthController.me);

router.get('/monuments', MonumentController.getMonuments);
router.get('/monuments/nearby', MonumentController.getNearby);
router.get('/monuments/:id', MonumentController.getOne);
router.post('/monuments/capture', authenticateToken, MonumentController.captureMonument);

router.get('/feed', optionalAuthenticateToken, FeedController.getFeed);
router.post('/feed/posts', authenticateToken, FeedController.createPost);
router.post('/feed/posts/:id/like', authenticateToken, FeedController.toggleLike);

router.post('/ai/narrator', AIController.talkToNarrator);
router.post('/ai/verify-reflection', AIController.verifyReflection);

export default router;
