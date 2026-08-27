import { PrismaClient } from '@prisma/client';

const fallbackDatabaseUrl = 'postgres://postgres.jilzmypcehdnydidjxib:yaow%20cedqaynotjww@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres';
const databaseUrl = process.env.DATABASE_URL?.trim() || fallbackDatabaseUrl;

const globalForPrisma = globalThis as unknown as { prisma?: PrismaClient };

export const prisma = globalForPrisma.prisma ?? new PrismaClient({
  datasources: { db: { url: databaseUrl } },
  log: process.env.NODE_ENV === 'development' ? ['warn', 'error'] : ['error']
});

if (process.env.NODE_ENV !== 'production') globalForPrisma.prisma = prisma;

export async function disconnectPrisma(): Promise<void> {
  await prisma.$disconnect();
}
