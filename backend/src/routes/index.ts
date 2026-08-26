import { Router } from 'express';
import { AuthController } from '../controllers/auth.controller';
import { UserController } from '../controllers/user.controller';
import { MonumentController } from '../controllers/monument.controller';
import { FeedController } from '../controllers/feed.controller';
import { AIController } from '../controllers/ai.controller';
import { SocialController } from '../controllers/social.controller';
import { authenticateToken, optionalAuthenticateToken } from '../middleware/auth.middleware';
import { rateLimit } from '../middleware/request.middleware';
import { config } from '../config/env';

const router = Router();

router.get('/health', (_req, res) => res.json({ status: 'online', version: '3.0.0', service: 'MonumentQuest API', timestamp: new Date().toISOString() }));

// Auth routes (OTP-only — no passwords)
const authLimit = rateLimit({ max: config.authRateLimitMax, key: 'auth' });
router.post('/auth/send-otp',          authLimit, AuthController.sendOtp);
router.post('/auth/login-with-otp',    authLimit, AuthController.loginWithOtp);
router.post('/auth/register-with-otp', authLimit, AuthController.registerWithOtp);
router.post('/auth/guest',             authLimit, AuthController.guest);
router.get('/auth/me',                 authenticateToken, AuthController.me);

// User profile & progress routes
router.get('/user/profile',    authenticateToken, UserController.getProfile);
router.patch('/user/profile',  authenticateToken, UserController.updateProfile);
router.patch('/user/progress', authenticateToken, UserController.syncProgress);

// Monument routes
router.get('/monuments',          MonumentController.getMonuments);
router.get('/monuments/nearby',   MonumentController.getNearby);
router.get('/monuments/:id',      MonumentController.getOne);
router.post('/monuments/capture', authenticateToken, MonumentController.capture);

// Feed routes
router.get('/feed',                           optionalAuthenticateToken, FeedController.getFeed);
router.post('/feed/posts',                    authenticateToken, FeedController.createPost);
router.post('/feed/posts/:id/like',           authenticateToken, FeedController.toggleLike);

// Social routes
router.get('/leaderboard', SocialController.leaderboard);
router.get('/guilds',      SocialController.guilds);

// AI routes
const aiLimit = rateLimit({ max: config.aiRateLimitMax, key: 'ai' });
router.post('/ai/narrator',          authenticateToken, aiLimit, AIController.talkToNarrator);
router.post('/ai/verify-reflection', authenticateToken, aiLimit, AIController.verifyReflection);

export default router;