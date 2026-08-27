import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config/env';
export interface AuthUser { id: string; email: string; role: string; isGuest?: boolean; }
export interface AuthRequest extends Request { user?: AuthUser; }
function readUser(req: Request): AuthUser { const header = req.headers.authorization; const token = header?.startsWith('Bearer ') ? header.slice(7).trim() : ''; if (!token) throw { status: 401, message: 'Bearer access token required' }; const decoded = jwt.verify(token, config.jwtSecret) as jwt.JwtPayload & Partial<AuthUser>; const id = decoded.sub || decoded.id; if (!id || typeof decoded.email !== 'string' || typeof decoded.role !== 'string') throw { status: 401, message: 'Invalid access token' }; return { id: String(id), email: decoded.email, role: decoded.role, isGuest: Boolean(decoded.isGuest) || decoded.email.endsWith('@guest.monumentquest.app') }; }
export function authenticateToken(req: AuthRequest, res: Response, next: NextFunction) { try { req.user = readUser(req); next(); } catch (error: any) { const message = error?.name === 'TokenExpiredError' ? 'Access token expired' : (error?.message || 'Invalid access token'); res.status(error?.status || 401).json({ success: false, error: message }); } }
export function optionalAuthenticateToken(req: AuthRequest, res: Response, next: NextFunction) { if (!req.headers.authorization) return next(); return authenticateToken(req, res, next); }
export function requireRegisteredUser(req: AuthRequest, res: Response, next: NextFunction) {
  if (req.user?.isGuest) return res.status(403).json({ success: false, error: 'Sign in to like or comment on posts' });
  return next();
}