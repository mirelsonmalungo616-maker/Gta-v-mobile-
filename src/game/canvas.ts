/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { CityBuilding, GameCar, GamePlayer, Pedestrian, Bullet, Explosion, Particle, TileTrack, Mission } from './types';
import { HOUSE_CATALOG } from './houseCatalog';
import { AMMU_NATION_LOCATION, CAR_DEALER_LOCATION, CITY_SIZE } from './city';

// Draw roads, lines, and sidewalks
export function drawCityBase(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  cameraX: number,
  cameraY: number,
  roadAvenuesH: number[],
  roadAvenuesV: number[],
  roadWidth: number
) {
  // Clear to dark asphalt/ground
  ctx.fillStyle = '#1e293b'; // slate ground background
  ctx.fillRect(0, 0, width, height);

  ctx.save();
  ctx.translate(-cameraX, -cameraY);

  // 1. Draw Green Parks & Out-of-bounds Grass
  ctx.fillStyle = '#059669'; // Pleasant emerald green grass for edges
  // Left border
  ctx.fillRect(-1000, -1000, 1000, CITY_SIZE + 2000);
  // Right border
  ctx.fillRect(CITY_SIZE, -1000, 1000, CITY_SIZE + 2000);
  // Top
  ctx.fillRect(-1000, -1000, CITY_SIZE + 2000, 1000);
  // Bottom
  ctx.fillRect(-1000, CITY_SIZE, CITY_SIZE + 2000, 1000);

  // 2. Draw Sidewalk blocks (background of block filling)
  ctx.fillStyle = '#334155'; // Dark sidewalk grey
  ctx.fillRect(0, 0, CITY_SIZE, CITY_SIZE);

  // 3. Draw Asphalt Road corridors
  ctx.fillStyle = '#0f172a'; // Deep asphalt dark gray

  // Draw Horizontal avenues
  for (const ry of roadAvenuesH) {
    ctx.fillRect(0, ry - roadWidth / 2, CITY_SIZE, roadWidth);
  }
  // Draw Vertical avenues
  for (const rx of roadAvenuesV) {
    ctx.fillRect(rx - roadWidth / 2, 0, roadWidth, CITY_SIZE);
  }

  // 4. Draw Center Lane road stripes (Yellow and white dashed lines)
  ctx.strokeStyle = '#eab308'; // Glowing yellow separator lines
  ctx.lineWidth = 2;
  
  // Horizontal Centerlines
  for (const ry of roadAvenuesH) {
    ctx.beginPath();
    ctx.setLineDash([15, 15]);
    ctx.moveTo(0, ry);
    ctx.lineTo(CITY_SIZE, ry);
    ctx.stroke();
    ctx.setLineDash([]);
  }

  // Vertical Centerlines
  for (const rx of roadAvenuesV) {
    ctx.beginPath();
    ctx.setLineDash([15, 15]);
    ctx.moveTo(rx, 0);
    ctx.lineTo(rx, CITY_SIZE);
    ctx.stroke();
    ctx.setLineDash([]);
  }

  ctx.restore();
}

// Draw 3D projected buildings
export function drawBuildings3D(
  ctx: CanvasRenderingContext2D,
  buildings: CityBuilding[],
  cameraX: number,
  cameraY: number,
  playerX: number,
  playerY: number
) {
  ctx.save();
  ctx.translate(-cameraX, -cameraY);

  for (const b of buildings) {
    // Basic Frustum culling: draw only if on-screen
    const bRight = b.x + b.width;
    const bBottom = b.y + b.height;

    // Base layout coordinates
    ctx.fillStyle = b.color;
    ctx.fillRect(b.x, b.y, b.width, b.height);

    // 3D projection: project building roofs slightly away from player center!
    // Creates a gorgeous parallex 3D top-down perspective like classic Grand Theft Auto!
    const projScale = 0.12; // building heights ratio
    const roofX = b.x + (b.x + b.width / 2 - playerX) * projScale;
    const roofY = b.y + (b.y + b.height / 2 - playerY) * projScale;

    // Draw connecting projection walls (3D facets)
    ctx.fillStyle = b.roofColor; // slightly darker for walls
    ctx.beginPath();
    ctx.moveTo(b.x, b.y);
    ctx.lineTo(roofX, roofY);
    ctx.lineTo(roofX + b.width, roofY);
    ctx.lineTo(b.x + b.width, b.y);
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(b.x + b.width, b.y + b.height);
    ctx.lineTo(roofX + b.width, roofY + b.height);
    ctx.lineTo(roofX + b.width, roofY);
    ctx.lineTo(b.x + b.width, b.y);
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(b.x, b.y + b.height);
    ctx.lineTo(roofX, roofY + b.height);
    ctx.lineTo(roofX + b.width, roofY + b.height);
    ctx.lineTo(b.x + b.width, b.y + b.height);
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(b.x, b.y);
    ctx.lineTo(roofX, roofY);
    ctx.lineTo(roofX, roofY + b.height);
    ctx.lineTo(b.x, b.y + b.height);
    ctx.fill();

    // Draw Roof cover
    ctx.fillStyle = b.roofColor;
    ctx.fillRect(roofX, roofY, b.width, b.height);

    // Subtle roof borders
    ctx.strokeStyle = 'rgba(255,255,255,0.1)';
    ctx.lineWidth = 1;
    ctx.strokeRect(roofX, roofY, b.width, b.height);

    // Label stores
    if (b.name) {
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 9px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(b.name, roofX + b.width / 2, roofY + b.height / 2 + 3);
    }
  }

  // Draw house real estate purchase markers
  for (const h of HOUSE_CATALOG) {
    // Draw home zone boundary glowing
    ctx.strokeStyle = h.color;
    ctx.lineWidth = 3;
    ctx.strokeRect(h.x, h.y, h.width, h.height);

    // Draw lawn yard
    ctx.fillStyle = 'rgba(250,204,21,0.15)';
    ctx.fillRect(h.x, h.y, h.width, h.height);

    // Visual building block inside
    const bx = h.x + 10;
    const by = h.y + 10;
    const bw = h.width - 20;
    const bh = h.height - 20;

    ctx.fillStyle = '#1e1b4b'; // dark royal indigo base
    ctx.fillRect(bx, by, bw, bh);

    ctx.fillStyle = h.color;
    ctx.font = 'bold 10px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('GARAGEM', h.x + h.width / 2, h.y + h.height / 2 - 2);
    ctx.font = '8px sans-serif';
    ctx.fillStyle = '#94a3b8';
    ctx.fillText(`$${h.price.toLocaleString()}`, h.x + h.width / 2, h.y + h.height / 2 + 10);
  }

  ctx.restore();
}

// Draw cars on street with headlight glow and braking tail glowing
export function drawCars(
  ctx: CanvasRenderingContext2D,
  cars: GameCar[],
  cameraX: number,
  cameraY: number
) {
  ctx.save();
  ctx.translate(-cameraX, -cameraY);

  for (const c of cars) {
    ctx.save();
    ctx.translate(c.x, c.y);
    ctx.rotate(c.angle);

    // Tire footprints
    ctx.fillStyle = '#000000';
    // Front tires
    ctx.fillRect(10, -11, 6, 3);
    ctx.fillRect(10, 8, 6, 3);
    // Rear tires
    ctx.fillRect(-12, -11, 7, 3);
    ctx.fillRect(-12, 8, 7, 3);

    // Metal Body bounding box
    ctx.fillStyle = c.color;
    ctx.fillRect(-15, -9, 30, 18);

    // Glass windshield & side windows
    ctx.fillStyle = 'rgba(15,23,42,0.85)';
    ctx.fillRect(-3, -7, 10, 14); // Front glass
    ctx.fillRect(-10, -7, 4, 14); // Rear glass

    // Highlight detail line
    ctx.strokeStyle = 'rgba(255,255,255,0.2)';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(3, -9);
    ctx.lineTo(-12, -9);
    ctx.lineTo(-12, 9);
    ctx.lineTo(3, 9);
    ctx.stroke();

    // Red brake taillights (glow strong if decelerating/braking)
    const isBraking = c.speed < 0 || (c.speed > 0.5 && Math.random() < 0.15);
    ctx.fillStyle = isBraking ? '#ef4444' : '#991b1b'; // neon-red vs dark-red
    ctx.fillRect(-16, -7, 2, 3);
    ctx.fillRect(-16, 4, 2, 3);

    // Glowing front headlights
    ctx.fillStyle = '#fef08a'; // bright warm yellow
    ctx.fillRect(15, -7, 2, 3);
    ctx.fillRect(15, 4, 2, 3);

    // Emit light beam vector
    const grad = ctx.createLinearGradient(16, 0, 120, 0);
    grad.addColorStop(0, 'rgba(253, 224, 71, 0.35)');
    grad.addColorStop(1, 'rgba(253, 224, 71, 0.0)');
    ctx.fillStyle = grad;

    ctx.beginPath();
    ctx.moveTo(16, -6);
    ctx.lineTo(110, -45);
    ctx.lineTo(110, 45);
    ctx.lineTo(16, 6);
    ctx.closePath();
    ctx.fill();

    // Cop lightbar (if police car)
    if (c.isPolice) {
      const state = Math.floor(Date.now() / 150) % 2;
      ctx.fillStyle = state === 0 ? '#3b82f6' : '#ef4444'; // Flashing Blue and Red
      ctx.fillRect(-4, -5, 6, 10);
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(-2, -1, 3, 2);
    }

    // Owner text helper
    if (c.isPlayerOwned) {
      ctx.rotate(-c.angle);
      ctx.font = 'bold 8px sans-serif';
      ctx.fillStyle = '#10b981';
      ctx.textAlign = 'center';
      ctx.fillText('(SEV)', 0, -14);
    } else {
      // Just print vehicle model name nicely
      ctx.rotate(-c.angle);
      ctx.font = '7px sans-serif';
      ctx.fillStyle = '#94a3b8';
      ctx.textAlign = 'center';
      ctx.fillText(c.name, 0, -14);
    }

    ctx.restore();
  }

  ctx.restore();
}

// Draw bullets, blood grids, fire explosions
export function drawVfx(
  ctx: CanvasRenderingContext2D,
  bullets: Bullet[],
  explosions: Explosion[],
  particles: Particle[],
  cameraX: number,
  cameraY: number
) {
  ctx.save();
  ctx.translate(-cameraX, -cameraY);

  // 1. Draw bullet trails
  for (const b of bullets) {
    ctx.strokeStyle = b.firedByPlayer ? '#fca5a5' : '#fed7aa'; // light pink vs gold orange
    ctx.lineWidth = 1.5;
    ctx.shadowBlur = 4;
    ctx.shadowColor = '#dc2626';

    const dx = b.vx * 0.4;
    const dy = b.vy * 0.4;
    ctx.beginPath();
    ctx.moveTo(b.x - dx, b.y - dy);
    ctx.lineTo(b.x, b.y);
    ctx.stroke();

    ctx.shadowBlur = 0; // reset
  }

  // 2. Draw active particles
  for (const p of particles) {
    ctx.fillStyle = p.color;
    ctx.globalAlpha = p.life;
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.globalAlpha = 1.0; // reset

  // 3. Draw dramatic fire explosions
  for (const exp of explosions) {
    const scaleRadius = exp.radius * (1 - Math.pow(exp.life - 1, 2));

    // Outer flame circle
    const grad = ctx.createRadialGradient(exp.x, exp.y, 1, exp.x, exp.y, scaleRadius);
    grad.addColorStop(0, 'rgba(255, 255, 255, 0.95)');
    grad.addColorStop(0.3, 'rgba(249, 115, 22, 0.85)'); // vibrant orange
    grad.addColorStop(0.8, 'rgba(220, 38, 38, 0.4)'); // deep crimson
    grad.addColorStop(1.0, 'rgba(0, 0, 0, 0)');

    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.arc(exp.x, exp.y, scaleRadius, 0, Math.PI * 2);
    ctx.fill();
  }

  ctx.restore();
}

// Compute and draw high-fidelity static radar mini-map
export function drawMiniMap(
  canvas: HTMLCanvasElement,
  player: GamePlayer,
  buildings: CityBuilding[],
  cars: GameCar[],
  activeMission: Mission | null
) {
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  const w = canvas.width;
  const h = canvas.height;
  ctx.clearRect(0, 0, w, h);

  // Center mini-map precisely on the player's world coordinate
  const zoom = 0.12; // zoom level
  const centerX = w / 2;
  const centerY = h / 2;

  ctx.save();
  ctx.translate(centerX, centerY);
  ctx.scale(zoom, zoom);
  ctx.translate(-player.x, -player.y);

  // --- DRAW ROADS AT MAP ---
  ctx.fillStyle = '#334155'; // Background sidewalk of map
  ctx.fillRect(player.x - 700 / zoom, player.y - 700 / zoom, 1400 / zoom, 1400 / zoom);

  // Horizontal roads in map
  ctx.fillStyle = '#0f172a';
  const rWidth = 80;
  // Let's draw road rectangles
  ctx.fillRect(0, 100 - rWidth / 2, CITY_SIZE, rWidth);
  ctx.fillRect(0, 400 - rWidth / 2, CITY_SIZE, rWidth);
  ctx.fillRect(0, 700 - rWidth / 2, CITY_SIZE, rWidth);
  ctx.fillRect(0, 1100 - rWidth / 2, CITY_SIZE, rWidth);
  ctx.fillRect(0, 1500 - rWidth / 2, CITY_SIZE, rWidth);
  ctx.fillRect(0, 1900 - rWidth / 2, CITY_SIZE, rWidth);

  // Vertical roads in map
  ctx.fillRect(100 - rWidth / 2, 0, rWidth, CITY_SIZE);
  ctx.fillRect(500 - rWidth / 2, 0, rWidth, CITY_SIZE);
  ctx.fillRect(900 - rWidth / 2, 0, rWidth, CITY_SIZE);
  ctx.fillRect(1300 - rWidth / 2, 0, rWidth, CITY_SIZE);
  ctx.fillRect(1700 - rWidth / 2, 0, rWidth, CITY_SIZE);
  ctx.fillRect(1900 - rWidth / 2, 0, rWidth, CITY_SIZE);

  // --- DRAW BUILDINGS AT MAP ---
  for (const b of buildings) {
    ctx.fillStyle = b.roofColor;
    ctx.fillRect(b.x, b.y, b.width, b.height);
  }

  // --- DRAW STORE SPOTS AT MAP ---
  // Ammu nation gun icon spot (Neon green)
  ctx.fillStyle = '#10b981';
  ctx.fillRect(AMMU_NATION_LOCATION.x, AMMU_NATION_LOCATION.y, 80, 60);

  // Simeon Dealership spot (Neon blue)
  ctx.fillStyle = '#3b82f6';
  ctx.fillRect(CAR_DEALER_LOCATION.x, CAR_DEALER_LOCATION.y, 90, 70);

  // --- HOUSE MARKET SPOTS ON MAP ---
  for (const house of HOUSE_CATALOG) {
    const owned = player.ownedHouses.includes(house.id);
    ctx.fillStyle = owned ? '#10b981' : '#f59e0b'; // green if owned, orange if buyable
    ctx.beginPath();
    ctx.arc(house.x + house.width / 2, house.y + house.height / 2, 25, 0, Math.PI * 2);
    ctx.fill();
  }

  // --- ACTIVE VEHICLES ON MAP ---
  for (const c of cars) {
    ctx.fillStyle = c.isPolice ? '#ef4444' : '#ffffff';
    ctx.fillRect(c.x - 12, c.y - 12, 24, 24);
  }

  // --- MISSION MARKERS AT MAP ---
  if (activeMission && activeMission.status === 'active') {
    // Green Checkpoint marker (recolha)
    if (activeMission.type === 'delivery' || activeMission.type === 'item_pickup') {
      const showCheckpoint = !activeMission.hasPickedUp && activeMission.checkpointX;
      const targetX = showCheckpoint ? activeMission.checkpointX! : activeMission.targetX!;
      const targetY = showCheckpoint ? activeMission.checkpointY! : activeMission.targetY!;
      
      ctx.fillStyle = showCheckpoint ? '#22c55e' : '#eab308'; // Lime Green vs Bright Yellow
      ctx.beginPath();
      ctx.arc(targetX, targetY, 40, 0, Math.PI * 2);
      ctx.fill();
    }
    // High-speed car chase vehicle marker (Red alert target flashing)
    else if (activeMission.type === 'chase' && activeMission.targetX) {
      ctx.fillStyle = (Math.floor(Date.now() / 250) % 2 === 0) ? '#dc2626' : '#ffffff';
      ctx.beginPath();
      ctx.arc(activeMission.targetX, activeMission.targetY, 45, 0, Math.PI * 2);
      ctx.fill();
    }
    // Survival lock-zone area (Orange crosshairs)
    else if (activeMission.type === 'survival' && activeMission.targetX) {
      ctx.fillStyle = 'rgba(234,88,12,0.3)';
      ctx.beginPath();
      ctx.arc(activeMission.targetX, activeMission.targetY, 150, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#ea580c';
      ctx.lineWidth = 10;
      ctx.stroke();
    }
  }

  ctx.restore();

  // --- DRAW CENTERED PLAYER INDICATOR ICON ---
  ctx.save();
  ctx.translate(centerX, centerY);
  ctx.rotate(player.angle);

  // Drawn as a sleek, highly visible tactical directional triangle arrow
  ctx.fillStyle = '#3b82f6'; // Bright electric blue arrow
  ctx.strokeStyle = '#ffffff';
  ctx.lineWidth = 1.5;

  ctx.beginPath();
  ctx.moveTo(11, 0); // Nose point
  ctx.lineTo(-7, -8); // Left tail wing
  ctx.lineTo(-3, 0); // Center indentation
  ctx.lineTo(-7, 8); // Right tail wing
  ctx.closePath();
  ctx.fill();
  ctx.stroke();

  ctx.restore();
}
