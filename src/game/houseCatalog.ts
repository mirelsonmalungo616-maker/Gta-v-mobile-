import { HouseCatalogItem } from './types';

export const HOUSE_CATALOG: HouseCatalogItem[] = [
  {
    id: 'studio_downtown',
    name: 'Kitnet no Centro',
    price: 25000,
    x: 450,
    y: 850,
    width: 60,
    height: 60,
    color: '#84cc16', // Lime green trim
    description: 'Um flat simples no centro da cidade. Ótimo ponto de partida, seguro e barato.',
  },
  {
    id: 'suburban_villa',
    name: 'Casa com Garagem',
    price: 150000,
    x: 1250,
    y: 350,
    width: 80,
    height: 70,
    color: '#f97316', // Orange trim
    description: 'Casa confortável na zona residencial com gramado e uma vaga de spawn de carro rápido.',
  },
  {
    id: 'sunset_beach_condo',
    name: 'Condomínio Vista do Mar',
    price: 450000,
    x: 250,
    y: 250,
    width: 80,
    height: 80,
    color: '#06b6d4', // Cyan
    description: 'Apartamento luxuoso no extremo oeste com vista exuberante da praia e spa privativo.',
  },
  {
    id: 'penthouse_maze',
    name: 'Cobertura Suprema Maze Bank',
    price: 1500000,
    x: 950,
    y: 950,
    width: 100,
    height: 100,
    color: '#a855f7', // Purple Luxury
    description: 'O topo da cidade! Cobertura com heliponto exclusivo, jacuzzi, blindagem total e recarga automática de armas.',
  }
];

export function getHouse(id: string): HouseCatalogItem | undefined {
  return HOUSE_CATALOG.find(h => h.id === id);
}
