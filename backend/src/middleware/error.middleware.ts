import { Request, Response, NextFunction } from 'express';

export const errorHandler = (err: any, _req: Request, res: Response, _next: NextFunction) => {
  console.error('API Error:', err);
  const status = Number.isInteger(err?.status) ? err.status : 500;
  const message = status >= 500 ? 'Internal Server Error' : (err?.message || 'Request failed');
  res.status(status).json({ success: false, error: message, timestamp: new Date().toISOString() });
};
