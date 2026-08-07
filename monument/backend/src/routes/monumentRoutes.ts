import { Router, Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';

const router = Router();
const prisma = new PrismaClient();

// GET /api/monuments/nearby?lat=...&lng=...&radius=...
router.get('/nearby', async (req: Request, res: Response) => {
  const { lat, lng, radius } = req.query;

  if (!lat || !lng || !radius) {
    return res.status(400).json({ error: 'Missing lat, lng, or radius' });
  }

  try {
    const latitude = parseFloat(lat as string);
    const longitude = parseFloat(lng as string);
    const rad = parseFloat(radius as string);

    // SQL query using PostGIS ST_DWithin
    // ST_MakePoint(longitude, latitude) is used because PostGIS uses (x, y) which is (lng, lat)
    const monuments = await prisma.$queryRaw`
      SELECT
        id,
        name,
        description,
        ST_X(location::geometry) as lng,
        ST_Y(location::geometry) as lat,
        points
      FROM "Monument"
      WHERE ST_DWithin(
        location,
        ST_SetSRID(ST_MakePoint(${longitude}, ${latitude}), 4326)::geography,
        ${rad}
      )
    `;

    res.json(monuments);
  } catch (error) {
    console.error('Error fetching nearby monuments:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /api/monuments/discover
router.post('/discover', async (req: Request, res: Response) => {
  const { monumentId, userId, imageUrl } = req.body;

  if (!monumentId || !userId) {
    return res.status(400).json({ error: 'Missing monumentId or userId' });
  }

  try {
    const discovery = await prisma.discovery.create({
      data: {
        monumentId: parseInt(monumentId),
        userId,
        imageUrl,
      },
    });

    res.status(201).json(discovery);
  } catch (error) {
    console.error('Error creating discovery:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export default router;
