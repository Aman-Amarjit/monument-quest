import { Request, Response, NextFunction } from 'express';
import { AIService } from '../services/ai.service';

export class AIController {
  public static async talkToNarrator(req: Request, res: Response, next: NextFunction) {
    try {
      const { monumentName, message } = req.body;
      if (!monumentName || !message) {
        return res.status(400).json({ success: false, error: 'monumentName and message are required' });
      }

      const response = await AIService.getPersonaResponse(monumentName, message);
      res.json({ success: true, data: { role: 'assistant', message: response } });
    } catch (err) {
      next(err);
    }
  }

  public static async verifyReflection(req: Request, res: Response, next: NextFunction) {
    try {
      const { monumentName, content } = req.body;
      if (!monumentName || !content) {
        return res.status(400).json({ success: false, error: 'monumentName and content are required' });
      }

      const result = await AIService.verifyReflection(monumentName, content);
      res.json({ success: true, data: result });
    } catch (err) {
      next(err);
    }
  }
}
