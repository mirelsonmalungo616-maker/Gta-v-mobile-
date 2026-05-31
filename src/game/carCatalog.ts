import { CarCatalogItem } from './types';

export const CAR_CATALOG: CarCatalogItem[] = [
  {
    id: 'wrangler',
    brand: 'Jeep',
    name: 'Wrangler Rubicon',
    price: 35000,
    maxSpeed: 4.5,
    acceleration: 0.12,
    handling: 0.04,
    color: '#1d4ed8', // Blue
    realLifeCounterpart: 'Jeep Wrangler Rubicon',
  },
  {
    id: 'mustang',
    brand: 'Ford',
    name: 'Mustang Shelby GT500',
    price: 75000,
    maxSpeed: 6.5,
    acceleration: 0.18,
    handling: 0.05,
    color: '#dc2626', // Red
    realLifeCounterpart: 'Shelby GT500',
  },
  {
    id: 'supra',
    brand: 'Toyota',
    name: 'Supra MK4 Drift',
    price: 90000,
    maxSpeed: 6.8,
    acceleration: 0.20,
    handling: 0.055,
    color: '#ea580c', // Orange
    realLifeCounterpart: 'Toyota Supra MK4',
  },
  {
    id: 'model_s',
    brand: 'Tesla',
    name: 'Model S Plaid',
    price: 120000,
    maxSpeed: 7.2,
    acceleration: 0.32, // Ludicrous electric acceleration!
    handling: 0.06,
    color: '#ffffff', // White
    realLifeCounterpart: 'Tesla Model S Plaid',
  },
  {
    id: '911_turbo',
    brand: 'Porsche',
    name: '911 Turbo S',
    price: 210000,
    maxSpeed: 7.8,
    acceleration: 0.28,
    handling: 0.08, // Precision cornering
    color: '#0284c7', // Sky Blue
    realLifeCounterpart: 'Porsche 911 Turbo S',
  },
  {
    id: 'aventador',
    brand: 'Lamborghini',
    name: 'Aventador SVJ',
    price: 450000,
    maxSpeed: 8.5,
    acceleration: 0.26,
    handling: 0.065,
    color: '#eab308', // Neon Yellow
    realLifeCounterpart: 'Lamborghini Aventador SVJ',
  },
  {
    id: 'laferrari',
    brand: 'Ferrari',
    name: 'LaFerrari Aperta',
    price: 1200000,
    maxSpeed: 9.2, // Ultra speed
    acceleration: 0.30,
    handling: 0.075,
    color: '#e11d48', // Crimson Red
    realLifeCounterpart: 'Ferrari LaFerrari',
  }
];

export function getCarStats(id: string): CarCatalogItem | undefined {
  return CAR_CATALOG.find(c => c.id === id);
}
