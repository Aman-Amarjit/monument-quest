import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = process.env.SUPABASE_URL || 'https://jilzmypcehdnydidjxib.supabase.co';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImppbHpteXBjZWhkbnlkaWRqeGliIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc3NDE1MjEsImV4cCI6MjEwMzMxNzUyMX0.eWseyxzk8cVZIteynb0CB15pI7PsH8tVkqWfZrmsL2s';

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
