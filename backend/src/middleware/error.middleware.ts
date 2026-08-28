import { Request, Response, NextFunction } from 'express';
import { Prisma } from '@prisma/client';

export function errorHandler(err: any, req: Request, res: Response, _next: NextFunction) {
  const requestId = (req as Request & { requestId?: string }).requestId;
  const isProduction = process.env.NODE_ENV === 'production';

  // In production only log status + message, never stack traces
  if (isProduction) {
    console.error('API Error', { requestId, status: err?.status, message: err?.message, code: err?.code });
  } else {
    console.error('API Error', { requestId, error: err });
  }

  let status = Number.isInteger(err?.status) ? err.status : 500;
  let message = status >= 500 ? 'Internal Server Error' : (err?.message || 'Request failed');

  if (err instanceof Prisma.PrismaClientKnownRequestError) {
    if (err.code === 'P2002') { status = 409; message = 'A record with these details already exists'; }
    if (err.code === 'P2025') { status = 404; message = 'Requested record was not found'; }
  }

  res.status(status).json({ success: false, error: message, requestId });
}