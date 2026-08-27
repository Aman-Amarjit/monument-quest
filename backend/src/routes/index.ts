import { Router } from 'express';
import { AuthController } from '../controllers/auth.controller';
import { UserController } from '../controllers/user.controller';
import { MonumentController } from '../controllers/monument.controller';
import { FeedController } from '../controllers/feed.controller';
import { AIController } from '../controllers/ai.controller';
import { SocialController } from '../controllers/social.controller';
import { authenticateToken, optionalAuthenticateToken, requireRegisteredUser } from '../middleware/auth.middleware';
import { rateLimit } from '../middleware/request.middleware';
import { config } from '../config/env';
import { HotelController } from '../controllers/hotel.controller';

const router = Router();
router.get('/health', (_req, res) => res.json({ status: 'online', version: '3.1.0', service: 'MonumentQuest API', timestamp: new Date().toISOString() }));

const authLimit = rateLimit({ max: config.authRateLimitMax, key: 'auth' });
router.post('/auth/send-otp', authLimit, AuthController.sendOtp);
router.post('/auth/login-with-otp', authLimit, AuthController.loginWithOtp);
router.post('/auth/register-with-otp', authLimit, AuthController.registerWithOtp);
router.post('/auth/guest', authLimit, AuthController.guest);
router.get('/auth/me', authenticateToken, AuthController.me);

router.get('/user/profile', authenticateToken, UserController.getProfile);
router.patch('/user/profile', authenticateToken, UserController.updateProfile);
router.patch('/user/progress', authenticateToken, UserController.syncProgress);

router.get('/monuments', MonumentController.getMonuments);
router.get('/monuments/nearby', MonumentController.getNearby);
router.get('/monuments/:id', MonumentController.getOne);
router.get('/hotels', HotelController.getHotels);
router.post('/hotels/claim-voucher', authenticateToken, HotelController.claimVoucher);
router.post('/monuments/capture', authenticateToken, MonumentController.capture);

const socialWriteLimit = rateLimit({ max: 60, key: 'social-write' });
router.get('/feed', optionalAuthenticateToken, FeedController.getFeed);
router.post('/feed/posts', authenticateToken, requireRegisteredUser, socialWriteLimit, FeedController.createPost);
router.delete('/feed/posts/:id', authenticateToken, requireRegisteredUser, socialWriteLimit, FeedController.deletePost);
router.post('/feed/posts/:id/like', authenticateToken, requireRegisteredUser, socialWriteLimit, FeedController.toggleLike);
router.get('/feed/posts/:id/comments', FeedController.getComments);
router.post('/feed/posts/:id/comments', authenticateToken, requireRegisteredUser, socialWriteLimit, FeedController.addComment);

router.get('/leaderboard', SocialController.leaderboard);
router.get('/guilds/me', authenticateToken, SocialController.currentGuild);
router.get('/guilds', SocialController.guilds);
router.get('/guilds/:id/leaderboard', SocialController.guildLeaderboard);
router.post('/guilds/:id/join', authenticateToken, requireRegisteredUser, socialWriteLimit, SocialController.joinGuild);
router.delete('/guilds/me', authenticateToken, requireRegisteredUser, socialWriteLimit, SocialController.leaveGuild);

router.get('/social/stories', optionalAuthenticateToken, SocialController.stories);
router.post('/social/stories', authenticateToken, requireRegisteredUser, socialWriteLimit, SocialController.createStory);
router.post('/social/stories/:id/view', authenticateToken, requireRegisteredUser, socialWriteLimit, SocialController.viewStory);
router.post('/social/users/:id/follow', authenticateToken, requireRegisteredUser, socialWriteLimit, SocialController.toggleFollow);
router.post('/social/posts/:id/save', authenticateToken, requireRegisteredUser, socialWriteLimit, SocialController.toggleSave);
router.post('/social/posts/:id/report', authenticateToken, requireRegisteredUser, socialWriteLimit, SocialController.reportPost);

const aiLimit = rateLimit({ max: config.aiRateLimitMax, key: 'ai' });
router.post('/ai/narrator', authenticateToken, aiLimit, AIController.talkToNarrator);
router.post('/ai/verify-reflection', authenticateToken, aiLimit, AIController.verifyReflection);
export default router;
