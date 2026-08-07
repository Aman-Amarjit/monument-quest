import { PrismaClient } from '@prisma/client';
const prisma = new PrismaClient();

export interface GeoLocation {
  latitude: number;
  longitude: number;
}

export class MonumentService {
  // Map tracking upload count per monument id/name
  private static monumentUploadCounts: Map<string, number> = new Map([
    ['b1', 42], // Lingaraj Temple (Popular)
    ['b2', 14], // Mukteshvara Temple (Uncommon)
    ['b3', 4],  // Rajarani Temple (Rare Pioneer)
    ['b4', 18], // Dhauli Shanti Stupa (Uncommon)
    ['b5', 2]   // Khandagiri Caves (Rare Pioneer)
  ]);

  public static calculateDistanceMeters(loc1: GeoLocation, loc2: GeoLocation): number {
    const R = 6371000;
    const dLat = (loc2.latitude - loc1.latitude) * (Math.PI / 180);
    const dLon = (loc2.longitude - loc1.longitude) * (Math.PI / 180);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(loc1.latitude * (Math.PI / 180)) *
        Math.cos(loc2.latitude * (Math.PI / 180)) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  public static getBhubaneswarMonuments() {
    return [
      { id: 'b1', name: 'Lingaraj Temple', locationName: 'Old Town, Bhubaneswar', latitude: 20.2382, longitude: 85.8338, pointsValue: 500, category: '11th-Century Kalinga Temple', isVerified: true },
      { id: 'b2', name: 'Mukteshvara Temple', locationName: 'Kedargouri, Bhubaneswar', latitude: 20.2427, longitude: 85.8402, pointsValue: 450, category: 'Gem of Kalinga Architecture', isVerified: true },
      { id: 'b3', name: 'Rajarani Temple', locationName: 'Rajarani Colony, Bhubaneswar', latitude: 20.2458, longitude: 85.8427, latitude2: 20.2458, pointsValue: 400, category: '11th-Century Sandstone Relic', isVerified: true },
      { id: 'b4', name: 'Dhauli Shanti Stupa', locationName: 'Dhauli Hills, Bhubaneswar', latitude: 20.1925, longitude: 85.8394, pointsValue: 600, category: 'Ashokan Peace Pagoda', isVerified: true },
      { id: 'b5', name: 'Khandagiri & Udayagiri Caves', locationName: 'Jayadev Vihar, Bhubaneswar', latitude: 20.2604, longitude: 85.7865, pointsValue: 550, category: '2nd-Century BC Rock Caves', isVerified: true }
    ];
  }

  // Calculate dynamic points based on total previous uploaders count
  public static calculateRarityPoints(previousUploads: number): { points: number; multiplier: number; rarityLabel: string; tierBadge: string } {
    if (previousUploads === 0) {
      return {
        points: 1000,
        multiplier: 5.0,
        rarityLabel: 'LEGENDARY (1st Discoverer)',
        tierBadge: '✦ FIRST DISCOVERER'
      };
    } else if (previousUploads <= 5) {
      return {
        points: 600,
        multiplier: 3.0,
        rarityLabel: 'RARE (Early Pioneer)',
        tierBadge: '🛡️ EARLY PIONEER'
      };
    } else if (previousUploads <= 20) {
      return {
        points: 300,
        multiplier: 1.5,
        rarityLabel: 'UNCOMMON EXPLORATION',
        tierBadge: '📍 ACTIVE EXPLORER'
      };
    } else {
      return {
        points: 100,
        multiplier: 1.0,
        rarityLabel: 'COMMON LANDMARK',
        tierBadge: '🏛️ POPULAR LANDMARK'
      };
    }
  }

  // Capture / Upload Monument with Dynamic Rarity Points
  public static async captureMonument(
    userId: string,
    monumentId: string,
    userLocation: GeoLocation,
    name?: string,
    imageUrl?: string
  ) {
    const monuments = this.getBhubaneswarMonuments();
    const target = monuments.find(m => m.id === monumentId || m.name.toLowerCase().includes((name || '').toLowerCase()));
    const key = target ? target.id : (name ? name.toLowerCase() : 'new_discovery');

    const previousUploadCount = this.monumentUploadCounts.get(key) || 0;
    
    // Calculate dynamic points based on upload count
    const rarity = this.calculateRarityPoints(previousUploadCount);

    // Increment upload count for this monument
    this.monumentUploadCounts.set(key, previousUploadCount + 1);

    const targetLoc = target ? { latitude: target.latitude, longitude: target.longitude } : userLocation;
    const distanceMeters = this.calculateDistanceMeters(userLocation, targetLoc);
    const isInRange = distanceMeters <= 50;

    return {
      success: true,
      monumentName: target ? target.name : (name || 'New Discovery'),
      locationName: target ? target.locationName : 'Bhubaneswar, Odisha',
      distanceMeters: Math.round(distanceMeters),
      isInRange,
      previousUploadersCount: previousUploadCount,
      newTotalUploadersCount: previousUploadCount + 1,
      multiplier: rarity.multiplier,
      pointsEarned: rarity.points,
      rarityLabel: rarity.rarityLabel,
      tierBadge: rarity.tierBadge,
      message: `Upload verified! ${previousUploadCount} explorers have uploaded this monument before. You unlocked ${rarity.tierBadge} (${rarity.multiplier}x Multiplier → +${rarity.points} XP)!`
    };
  }
}
