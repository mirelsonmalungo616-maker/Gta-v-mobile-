import { CityBuilding } from './types';

// City boundary is 2000 x 2000 pixels
export const CITY_SIZE = 2000;

// Road network definition
// High-level coordinate lines for horizontal and vertical main avenues (width 80px)
export const ROAD_AVENUES_H = [100, 400, 700, 1100, 1500, 1900];
export const ROAD_AVENUES_V = [100, 500, 900, 1300, 1700, 1900];
export const ROAD_WIDTH = 80;

// Specific interest locations
export const AMMU_NATION_LOCATION = { x: 850, y: 450, width: 80, height: 60 };
export const CAR_DEALER_LOCATION = { x: 1250, y: 750, width: 90, height: 70 };

// Generate buildings in grid blocks
export function generateCityBuildings(): CityBuilding[] {
  const buildings: CityBuilding[] = [];

  // 1. Add specific stores
  buildings.push({
    id: 'ammu_nation',
    x: AMMU_NATION_LOCATION.x,
    y: AMMU_NATION_LOCATION.y,
    width: AMMU_NATION_LOCATION.width,
    height: AMMU_NATION_LOCATION.height,
    type: 'store',
    color: '#064e3b', // Deep green
    roofColor: '#105e49',
    name: 'Ammu-Nation',
  });

  buildings.push({
    id: 'car_dealer',
    x: CAR_DEALER_LOCATION.x,
    y: CAR_DEALER_LOCATION.y,
    width: CAR_DEALER_LOCATION.width,
    height: CAR_DEALER_LOCATION.height,
    type: 'store',
    color: '#1e3a8a', // Dark blue dealership
    roofColor: '#2563eb',
    name: 'Concessionária Simeon',
  });

  // 2. Generate office skyscrapers, stores, residential blocks procedurally inside blocks
  // Block-by-block filling
  for (let i = 0; i < ROAD_AVENUES_V.length - 1; i++) {
    const v1 = ROAD_AVENUES_V[i] + ROAD_WIDTH / 2;
    const v2 = ROAD_AVENUES_V[i + 1] - ROAD_WIDTH / 2;
    const blockWidth = v2 - v1;

    for (let j = 0; j < ROAD_AVENUES_H.length - 1; j++) {
      const h1 = ROAD_AVENUES_H[j] + ROAD_WIDTH / 2;
      const h2 = ROAD_AVENUES_H[j + 1] - ROAD_WIDTH / 2;
      const blockHeight = h2 - h1;

      // Skip blocks that overlap with explicit interest zones
      if (v1 < AMMU_NATION_LOCATION.x + 100 && v2 > AMMU_NATION_LOCATION.x - 50 &&
          h1 < AMMU_NATION_LOCATION.y + 100 && h2 > AMMU_NATION_LOCATION.y - 50) {
        continue;
      }
      if (v1 < CAR_DEALER_LOCATION.x + 100 && v2 > CAR_DEALER_LOCATION.x - 50 &&
          h1 < CAR_DEALER_LOCATION.y + 100 && h2 > CAR_DEALER_LOCATION.y - 50) {
        continue;
      }

      // Procedural filling inside this block
      // If the block is very large, place multiple buildings separated by alleys (e.g., width 50-90px)
      let currentX = v1 + 15;
      while (currentX < v2 - 40) {
        let currentY = h1 + 15;
        while (currentY < h2 - 40) {
          const bW = Math.min(Math.floor(45 + Math.random() * 40), v2 - 10 - currentX);
          const bH = Math.min(Math.floor(45 + Math.random() * 40), h2 - 10 - currentY);

          // Randomize type
          const rand = Math.random();
          let type: 'sky' | 'office' | 'residential' | 'park' = 'residential';
          let color = '#374151'; // grey
          let roofColor = '#1f2937';

          if (rand < 0.25) {
            type = 'sky';
            color = '#1e293b'; // Slate modern glass scraper
            roofColor = '#0f172a';
          } else if (rand < 0.55) {
            type = 'office';
            color = '#4b5563'; // lighter corporate office
            roofColor = '#374151';
          } else if (rand < 0.85) {
            type = 'residential';
            color = '#78350f'; // reddish-brown brick residential
            roofColor = '#92400e';
          } else {
            type = 'park'; // a green empty space - no building collision
          }

          if (type !== 'park' && bW > 25 && bH > 25) {
            buildings.push({
              id: `b_${currentX}_${currentY}`,
              x: currentX,
              y: currentY,
              width: bW,
              height: bH,
              type,
              color,
              roofColor,
            });
          }

          currentY += bH + 25; // 25px alley/spacer
        }
        currentX += 80; // step size
      }
    }
  }

  return buildings;
}

// Global cached buildings
export const CITY_BUILDINGS = generateCityBuildings();

// High performance box collision
export function checkBuildingCollision(
  x: number,
  y: number,
  radius: number
): { hit: boolean; adjustX: number; adjustY: number; buildingType?: string } {
  let hit = false;
  let adjustX = 0;
  let adjustY = 0;
  let buildingType: string | undefined;

  // Broadphase check: clamp player coordinates inside CITY_SIZE
  if (x - radius < 0) {
    return { hit: true, adjustX: radius - x, adjustY: 0 };
  }
  if (x + radius > CITY_SIZE) {
    return { hit: true, adjustX: CITY_SIZE - radius - x, adjustY: 0 };
  }
  if (y - radius < 0) {
    return { hit: true, adjustX: 0, adjustY: radius - y };
  }
  if (y + radius > CITY_SIZE) {
    return { hit: true, adjustX: 0, adjustY: CITY_SIZE - radius - y };
  }

  // Check each building bounds
  for (let i = 0; i < CITY_BUILDINGS.length; i++) {
    const b = CITY_BUILDINGS[i];
    
    // Find closest point on building to player circle
    const closestX = Math.max(b.x, Math.min(x, b.x + b.width));
    const closestY = Math.max(b.y, Math.min(y, b.y + b.height));

    const distanceX = x - closestX;
    const distanceY = y - closestY;
    const distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);

    if (distanceSquared < radius * radius) {
      hit = true;
      buildingType = b.type;
      
      const distance = Math.sqrt(distanceSquared);
      if (distance === 0) {
        // Center precisely inside - push out horizontally based on building center
        const centerX = b.x + b.width / 2;
        const pushDir = x < centerX ? -1 : 1;
        adjustX = pushDir * radius;
      } else {
        const overlap = radius - distance;
        adjustX = (distanceX / distance) * overlap;
        adjustY = (distanceY / distance) * overlap;
      }
      break; // Resolve one collision at a time (standard arcade resolution)
    }
  }

  return { hit, adjustX, adjustY, buildingType };
}

// Checks if a coordinate is inside a road zone (for clean spawning of cars or missions)
export function isOnRoad(x: number, y: number): boolean {
  // Check horizontal roads
  for (const ry of ROAD_AVENUES_H) {
    if (Math.abs(y - ry) <= ROAD_WIDTH / 2) return true;
  }
  // Check vertical roads
  for (const rx of ROAD_AVENUES_V) {
    if (Math.abs(x - rx) <= ROAD_WIDTH / 2) return true;
  }
  return false;
}

// Find a random point on open road streets (ideal for spawning cars, pedestrians, drug deals, pickups)
export function getRandomRoadPoint(): { x: number; y: number } {
  const isHorizontal = Math.random() > 0.5;
  if (isHorizontal) {
    const ry = ROAD_AVENUES_H[Math.floor(Math.random() * ROAD_AVENUES_H.length)];
    const rx = Math.floor(Math.random() * (CITY_SIZE - 200)) + 100;
    return { x: rx, y: ry + (Math.random() * 20 - 10) };
  } else {
    const rx = ROAD_AVENUES_V[Math.floor(Math.random() * ROAD_AVENUES_V.length)];
    const ry = Math.floor(Math.random() * (CITY_SIZE - 200)) + 100;
    return { x: rx + (Math.random() * 20 - 10), y: ry };
  }
}
