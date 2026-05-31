export enum WeaponType {
  FISTS = 'FISTS',
  PISTOL = 'PISTOL',
  SMG = 'SMG',
  SHOTGUN = 'SHOTGUN',
  RPG = 'RPG',
}

export interface Weapon {
  type: WeaponType;
  name: string;
  damage: number;
  fireRate: number; // in ms
  ammo: number;
  maxAmmo: number;
  unlocked: boolean;
  price: number;
  color: string;
}

export interface CarCatalogItem {
  id: string;
  name: string;
  brand: string;
  price: number;
  maxSpeed: number;
  acceleration: number;
  handling: number;
  color: string;
  realLifeCounterpart: string;
}

export interface HouseCatalogItem {
  id: string;
  name: string;
  price: number;
  x: number; // City position
  y: number;
  width: number;
  height: number;
  color: string;
  description: string;
}

export interface GamePlayer {
  x: number;
  y: number;
  angle: number;
  speed: number;
  health: number;
  armor: number;
  cash: number;
  xp: number;
  level: number;
  playTime: number; // in seconds
  activeWeapon: WeaponType;
  weapons: Record<WeaponType, Weapon>;
  inCar: boolean;
  currentCarId: string | null; // ID of the car entity being driven
  ownedCars: string[]; // List of catalog IDs
  ownedHouses: string[]; // List of catalog IDs
  wantedLevel: number; // 0 to 5 stars
  wantedPoints: number; // For fine-grained increase
  isDead: boolean;
  activeMissionId: string | null;
  missionProgress: number;
}

export interface GameCar {
  id: string;
  catalogId: string;
  name: string;
  brand: string;
  realLifeCounterpart: string;
  x: number;
  y: number;
  angle: number;
  speed: number;
  health: number;
  maxHealth: number;
  color: string;
  maxSpeed: number;
  acceleration: number;
  handling: number;
  isPlayerOwned: boolean;
  isPolice: boolean;
}

export interface Pedestrian {
  id: string;
  x: number;
  y: number;
  angle: number;
  speed: number;
  health: number;
  color: string;
  state: 'walking' | 'fleeing' | 'dead';
  fleeTimer: number;
  targetX: number;
  targetY: number;
}

export interface Bullet {
  id: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
  damage: number;
  firedByPlayer: boolean;
  rangeRemaining: number;
}

export interface Explosion {
  id: string;
  x: number;
  y: number;
  radius: number;
  life: number; // 0 to 1
}

export interface Particle {
  id: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
  color: string;
  size: number;
  life: number; // 0 to 1
  decay: number;
}

export interface TireTrack {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  alpha: number;
}

export interface CityBuilding {
  id: string;
  x: number;
  y: number;
  width: number;
  height: number;
  type: 'sky' | 'office' | 'residential' | 'store' | 'garage' | 'park';
  color: string;
  roofColor: string;
  name?: string;
}

export interface Mission {
  id: string;
  title: string;
  description: string;
  reward: number;
  xpReward: number;
  type: 'delivery' | 'chase' | 'item_pickup' | 'survival';
  status: 'available' | 'active' | 'completed' | 'failed';
  difficulty: 'easy' | 'medium' | 'hard';
  targetX?: number;
  targetY?: number;
  targetId?: string; // used for target vehicle or person
  checkpointX?: number; // midpoints or starting pick-ups
  checkpointY?: number;
  hasPickedUp?: boolean; // tracking delivery stage
  timeLimit?: number; // total time in seconds
  timeRemaining?: number; // counting down in seconds
  details?: string;
}
