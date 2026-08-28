export function sanitizeText(input: string, maxLength = 1000): string {
  if (typeof input !== 'string') return '';
  return input
    .replace(/<[^>]*>?/gm, '')            // Strip HTML tags
    .replace(/javascript:/gi, '')          // Block JS protocol
    .replace(/vbscript:/gi, '')            // Block VBScript protocol
    .replace(/data:/gi, '')                // Block data URIs
    .replace(/on\w+\s*=/gi, '')            // Block inline event handlers (onclick=, onload=, etc.)
    .replace(/&(#?[a-z0-9]+);?/gi, ' ')   // Strip HTML entities to prevent entity injection
    .replace(/\0/g, '')                    // Strip null bytes
    .trim()
    .slice(0, maxLength);
}
