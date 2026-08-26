import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

const monuments = [
  { id: 'b1', name: 'Lingaraj Temple', locationName: 'Old Town, Bhubaneswar', latitude: 20.2382, longitude: 85.8338, pointsValue: 500, category: '11th-Century Kalinga Temple' },
  { id: 'b2', name: 'Mukteshvara Temple', locationName: 'Kedargouri, Bhubaneswar', latitude: 20.2427, longitude: 85.8402, pointsValue: 450, category: 'Gem of Kalinga Architecture' },
  { id: 'b3', name: 'Rajarani Temple', locationName: 'Rajarani Colony, Bhubaneswar', latitude: 20.2458, longitude: 85.8427, pointsValue: 400, category: '11th-Century Sandstone Relic' },
  { id: 'b4', name: 'Dhauli Shanti Stupa', locationName: 'Dhauli Hills, Bhubaneswar', latitude: 20.1925, longitude: 85.8394, pointsValue: 600, category: 'Ashokan Peace Pagoda' },
  { id: 'b5', name: 'Khandagiri & Udayagiri Caves', locationName: 'Khandagiri, Bhubaneswar', latitude: 20.2604, longitude: 85.7865, pointsValue: 550, category: '2nd-Century BC Rock Caves' }
];

async function main() {
  for (const monument of monuments) {
    await prisma.monument.upsert({ where: { id: monument.id }, update: monument, create: monument });
  }

  await prisma.guild.upsert({
    where: { id: 'guild-kalinga' },
    update: { name: 'Kalinga Keepers', region: 'Odisha' },
    create: { id: 'guild-kalinga', name: 'Kalinga Keepers', region: 'Odisha' }
  });

  console.log(`Seeded ${monuments.length} monuments and the Kalinga Keepers guild.`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
}).finally(async () => {
  await prisma.$disconnect();
});
