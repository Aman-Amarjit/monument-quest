import { PrismaClient } from '@prisma/client';

const poolerUrl = 'postgresql://postgres.jilzmypcehdnydidjxib:yaowcedqaynotjww@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?sslmode=require';
const rawEnvUrl = process.env.DATABASE_URL?.trim();
const databaseUrl = (rawEnvUrl && rawEnvUrl.length > 10) ? rawEnvUrl : poolerUrl;

const globalForPrisma = globalThis as unknown as { prisma?: PrismaClient };

export const prisma = globalForPrisma.prisma ?? new PrismaClient({
  datasources: { db: { url: databaseUrl } },
  log: process.env.NODE_ENV === 'development' ? ['warn', 'error'] : ['error']
});

if (process.env.NODE_ENV !== 'production') globalForPrisma.prisma = prisma;

export async function disconnectPrisma(): Promise<void> {
  await prisma.$disconnect();
}
