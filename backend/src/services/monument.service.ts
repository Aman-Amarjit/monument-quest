import { prisma } from '../lib/prisma';

export interface GeoLocation {
  latitude: number;
  longitude: number;
}

const CAPTURE_RADIUS_METERS = 100;

export class MonumentService {
  public static calculateDistanceMeters(loc1: GeoLocation, loc2: GeoLocation): number {
    const radius = 6371000;
    const dLat = (loc2.latitude - loc1.latitude) * Math.PI / 180;
    const dLon = (loc2.longitude - loc1.longitude) * Math.PI / 180;
    const a = Math.sin(dLat / 2) ** 2 +
      Math.cos(loc1.latitude * Math.PI / 180) * Math.cos(loc2.latitude * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
    return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  public static calculateRarityPoints(previousUploads: number) {
    if (previousUploads === 0) return { points: 1000, multiplier: 5, rarityLabel: 'LEGENDARY (1st Discoverer)', tierBadge: '✦ FIRST DISCOVERER' };
    if (previousUploads <= 5) return { points: 600, multiplier: 3, rarityLabel: 'RARE (Early Pioneer)', tierBadge: '🛡️ EARLY PIONEER' };
    if (previousUploads <= 20) return { points: 300, multiplier: 1.5, rarityLabel: 'UNCOMMON EXPLORATION', tierBadge: '📍 ACTIVE EXPLORER' };
    return { points: 100, multiplier: 1, rarityLabel: 'COMMON LANDMARK', tierBadge: '🏛️ POPULAR LANDMARK' };
  }

  private static format(monument: any, distanceMeters?: number) {
    return {
      id: monument.id,
      name: monument.name,
      locationName: monument.locationName,
      latitude: monument.latitude,
      longitude: monument.longitude,
      points: monument.pointsValue,
      pointsValue: monument.pointsValue,
      category: monument.category,
      isVerified: monument.isVerified,
      totalContributionPoints: monument.totalContributionPoints,
      totalUploadersCount: monument._count?.discoveries ?? 0,
      ...(distanceMeters === undefined ? {} : { distanceMeters: Math.round(distanceMeters) })
    };
  }

  public static async getMonuments() {
    const monuments = await prisma.monument.findMany({
      orderBy: { name: 'asc' },
      include: { _count: { select: { discoveries: true } } }
    });
    return monuments.map((monument) => this.format(monument));
  }

  public static async getNearbyMonuments(location: GeoLocation, radiusMeters = 50000) {
    const monuments = await prisma.monument.findMany({
      include: { _count: { select: { discoveries: true } } }
    });
    return monuments
      .map((monument) => ({ monument, distance: this.calculateDistanceMeters(location, monument) }))
      .filter(({ distance }) => distance <= radiusMeters)
      .sort((a, b) => a.distance - b.distance)
      .map(({ monument, distance }) => this.format(monument, distance));
  }

  public static async getMonument(id: string) {
    const monument = await prisma.monument.findUnique({
      where: { id },
      include: { _count: { select: { discoveries: true } } }
    });
    if (!monument) throw { status: 404, message: 'Monument not found' };
    return this.format(monument);
  }

  public static async captureMonument(userId: string, monumentId: string | undefined, userLocation: GeoLocation, name?: string, imageUrl?: string) {
    const allMonuments = monumentId
      ? [await prisma.monument.findUnique({ where: { id: monumentId } })]
      : await prisma.monument.findMany();
    const normalizedName = name?.trim().toLowerCase();
    const target = allMonuments.find((monument) => monument && (!normalizedName || monument.name.toLowerCase() === normalizedName)) || null;
    if (!target) throw { status: 404, message: 'Choose a valid monument from the catalog' };

    const distanceMeters = this.calculateDistanceMeters(userLocation, target);
    if (distanceMeters > CAPTURE_RADIUS_METERS) {
      return {
        success: false,
        monumentName: target.name,
        locationName: target.locationName,
        distanceMeters: Math.round(distanceMeters),
        isInRange: false,
        pointsEarned: 0,
        message: 'Move closer to the monument before checking in.'
      };
    }

    const result = await prisma.$transaction(async (tx) => {
      const existing = await tx.discovery.findUnique({ where: { userId_monumentId: { userId, monumentId: target.id } } });
      const previousUploadersCount = await tx.discovery.count({ where: { monumentId: target.id } });
      if (existing) {
        return { existing: true, previousUploadersCount, newTotalUploadersCount: previousUploadersCount, pointsEarned: 0, rarity: this.calculateRarityPoints(previousUploadersCount) };
      }

      const rarity = this.calculateRarityPoints(previousUploadersCount);
      await tx.discovery.create({ data: { userId, monumentId: target.id, imageUrl, pointsEarned: rarity.points, isFirst: previousUploadersCount === 0 } });
      await tx.user.update({ where: { id: userId }, data: { points: { increment: rarity.points } } });
      await tx.monument.update({ where: { id: target.id }, data: { totalContributionPoints: { increment: rarity.points } } });
      return { existing: false, previousUploadersCount, newTotalUploadersCount: previousUploadersCount + 1, pointsEarned: rarity.points, rarity };
    });

    return {
      success: true,
      monumentId: target.id,
      monumentName: target.name,
      locationName: target.locationName,
      distanceMeters: Math.round(distanceMeters),
      isInRange: true,
      previousUploadersCount: result.previousUploadersCount,
      newTotalUploadersCount: result.newTotalUploadersCount,
      multiplier: result.existing ? 0 : result.rarity.multiplier,
      pointsEarned: result.pointsEarned,
      rarityLabel: result.existing ? 'ALREADY DISCOVERED' : result.rarity.rarityLabel,
      tierBadge: result.existing ? '✓ ALREADY CAPTURED' : result.rarity.tierBadge,
      alreadyCaptured: result.existing,
      message: result.existing
        ? 'You have already captured this monument. XP is awarded once per explorer.'
        : 'Upload verified! ' + result.rarity.tierBadge + ' (' + result.rarity.multiplier + 'x multiplier → +' + result.pointsEarned + ' XP).'
    };
  }
}
