import { PrismaClient } from '@prisma/client';

const POOLER_URL = 'postgresql://postgres.jilzmypcehdnydidjxib:Promethium%4014561@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?pgbouncer=true';

if (!process.env.DATABASE_URL || process.env.DATABASE_URL.includes('supabase.co:5432')) {
  process.env.DATABASE_URL = POOLER_URL;
}

const globalForPrisma = globalThis as unknown as { prisma?: PrismaClient };

export const prisma = globalForPrisma.prisma ?? new PrismaClient({
  datasources: {
    db: {
      url: process.env.DATABASE_URL || POOLER_URL
    }
  },
  log: process.env.NODE_ENV === 'development' ? ['warn', 'error'] : ['error']
});

if (process.env.NODE_ENV !== 'production') globalForPrisma.prisma = prisma;

export async function disconnectPrisma(): Promise<void> {
  await prisma.$disconnect();
}