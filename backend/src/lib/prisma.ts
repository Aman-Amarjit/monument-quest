import { PrismaClient } from '@prisma/client';

const primaryUrl = process.env.DATABASE_URL?.trim();
const fallbackUrl = 'postgresql://postgres.jilzmypcehdnydidjxib:yaowcedqaynotjww@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?sslmode=require&pgbooster=true';
const databaseUrl = primaryUrl || fallbackUrl;

const globalForPrisma = globalThis as unknown as { prisma?: PrismaClient };

export const prisma = globalForPrisma.prisma ?? new PrismaClient({
  datasources: { db: { url: databaseUrl } },
  log: process.env.NODE_ENV === 'development' ? ['warn', 'error'] : ['error']
});

if (process.env.NODE_ENV !== 'production') globalForPrisma.prisma = prisma;

export async function disconnectPrisma(): Promise<void> {
  await prisma.$disconnect();
}
