import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config/env';

export interface AuthUser {
  id: string;
  email: string;
  role: string;
}

export interface AuthRequest extends Request {
  user?: AuthUser;
}

function readUser(req: Request): AuthUser {
  const authHeader = req.headers.authorization;
  const token = authHeader?.startsWith('Bearer ') ? authHeader.slice(7) : undefined;

  if (!token) {
    throw { status: 401, message: 'Bearer access token required' };
  }

  const decoded = jwt.verify(token, config.jwtSecret) as jwt.JwtPayload & Partial<AuthUser>;
  const id = decoded.sub || decoded.id;
  if (!id || !decoded.email || !decoded.role) {
    throw { status: 401, message: 'Invalid access token' };
  }

  return { id: String(id), email: String(decoded.email), role: String(decoded.role) };
}

export const authenticateToken = (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    req.user = readUser(req);
    next();
  } catch (error: any) {
    const message = error?.name === 'TokenExpiredError' ? 'Access token expired' : (error?.message || 'Invalid access token');
    res.status(error?.status || 401).json({ success: false, error: message });
  }
};

export const optionalAuthenticateToken = (req: AuthRequest, res: Response, next: NextFunction) => {
  if (!req.headers.authorization) return next();
  return authenticateToken(req, res, next);
};
