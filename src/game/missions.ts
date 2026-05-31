import { Mission } from './types';
import { getRandomRoadPoint } from './city';

// Simple distance helper
function getDistance(x1: number, y1: number, x2: number, y2: number): number {
  return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
}

// Procedural titles and descriptions based on mission types
const MISSION_TEMPLATES = {
  delivery: [
    {
      title: 'Contrabando Urgente 📦',
      details: 'Pegue a carga de contrabando secreta no ponto indicado e entregue rapidamente antes do tempo se esgotar!',
    },
    {
      title: 'Remessa de Diamantes Raros 💎',
      details: 'Esconda o pacote de diamantes no seu porta-malas e escape até o porto seguro sem chamar atenção do helicóptero.',
    },
    {
      title: 'Carga de Armas de Elite 🔫',
      details: 'Transporte a munição pesada para a facção aliada. Dirija rápido pelas avenidas e evite bater o carro!',
    }
  ],
  chase: [
    {
      title: 'Alvo em Alta Velocidade 🏎️',
      details: 'Um informante da polícia está tentando fugir da cidade em um carro esportivo. Vá atrás dele, bata ou atire até pará-lo!',
    },
    {
      title: 'Vingança em Movimento 💀',
      details: 'Membros da gangue rival roubaram um Mustang. Encontre o motorista na rodovia e elimine o veículo com força total.',
    },
    {
      title: 'Carro-Forte Fora de Rota 💰',
      details: 'O motorista traidor fugiu com a maleta financeira. Intercepte o carro em fuga para reaver nossos fundos.',
    }
  ],
  item_pickup: [
    {
      title: 'Recuperador de Maletas 💼',
      details: 'Diversas maletas de arquivos confidenciais caíram nas ruas. Recupere as provas espalhadas no mini-mapa!',
    },
    {
      title: 'Serviço de Courier Express ⚡',
      details: 'O chefe precisa que você recolha 3 sub-pacotes de chaves de criptografia e os leve até a estação central.',
    },
    {
      title: 'Drogas Ocultas no Beco 💊',
      details: 'Recolha as encomendas ocultas espalhadas pelos becos da zona central antes que a patrulha tome as pistas.',
    }
  ],
  survival: [
    {
      title: 'Emboscada no Calçadão 🩸',
      details: 'Vá até o ponto de encontro. Atenção: fomos delatados! Sobreviva à emboscada enfrentando levas de atiradores armados.',
    },
    {
      title: 'Defesa do Território ⛺',
      details: 'Inimigos estão invadindo nossa garagem central. Fique no local e resista o ataque supremo dos rivais!',
    }
  ]
};

/**
 * Procedurally generates a mission based on the player's core stats.
 * Customizes locations so they are scaled reasonably around the player.
 */
export function generateProceduralMission(
  playerX: number,
  playerY: number,
  playerLevel: number,
  playTime: number
): Mission {
  const id = `proc_mission_${Date.now()}`;
  
  // Decide difficulty based on player level & total playtime
  let difficulty: 'easy' | 'medium' | 'hard' = 'easy';
  if (playerLevel >= 6 || playTime > 600) { // 10 minutes play
    difficulty = 'hard';
  } else if (playerLevel >= 3 || playTime > 240) { // 4 minutes play
    difficulty = 'medium';
  }

  // Pick mission type randomly
  const types: Array<'delivery' | 'chase' | 'item_pickup' | 'survival'> = [
    'delivery',
    'chase',
    'item_pickup',
    'survival',
  ];
  const type = types[Math.floor(Math.random() * types.length)];

  // Select a text template
  const templates = MISSION_TEMPLATES[type];
  const template = templates[Math.floor(Math.random() * templates.length)];

  // Scale cash and XP rewards matching level + difficulty
  const diffMultiplier = difficulty === 'hard' ? 2.0 : difficulty === 'medium' ? 1.4 : 1.0;
  const baseReward = 2000 + (playerLevel * 750);
  const reward = Math.round(baseReward * diffMultiplier);
  const xpReward = Math.round((450 + (playerLevel * 100)) * diffMultiplier);

  // Spatially place checkpoints and goals nearby but challenging
  // Lower level = spawns closer to player to make it approachable
  const minDist = 350;
  const maxDist = difficulty === 'hard' ? 1200 : difficulty === 'medium' ? 800 : 500;

  let attempt = 0;
  let targetPoint = getRandomRoadPoint();
  while (attempt < 15 && (getDistance(playerX, playerY, targetPoint.x, targetPoint.y) < minDist || getDistance(playerX, playerY, targetPoint.x, targetPoint.y) > maxDist)) {
    targetPoint = getRandomRoadPoint();
    attempt++;
  }

  let checkpointPoint = getRandomRoadPoint();
  attempt = 0;
  while (attempt < 15 && (getDistance(playerX, playerY, checkpointPoint.x, checkpointPoint.y) < 150 || getDistance(targetPoint.x, targetPoint.y, checkpointPoint.x, checkpointPoint.y) < 200)) {
    checkpointPoint = getRandomRoadPoint();
    attempt++;
  }

  // Setup time limit scaled dynamically
  const distance = getDistance(playerX, playerY, targetPoint.x, targetPoint.y);
  // Give about 10 pixels per 0.5s + base 15-30s padding
  const baseTimeLimit = Math.max(25, Math.round(distance / 12) + 20);
  const timeLimit = Math.round(baseTimeLimit * (difficulty === 'hard' ? 0.8 : difficulty === 'medium' ? 0.9 : 1.1));

  let finalDesc = `${template.details}\n\nDificuldade: ${difficulty.toUpperCase()}\nRecompensa: $${reward.toLocaleString()} • Experiência: +${xpReward} XP`;

  return {
    id,
    title: template.title,
    description: finalDesc,
    reward,
    xpReward,
    type,
    status: 'available',
    difficulty,
    targetX: targetPoint.x,
    targetY: targetPoint.y,
    checkpointX: checkpointPoint.x,
    checkpointY: checkpointPoint.y,
    hasPickedUp: false,
    timeLimit,
    timeRemaining: timeLimit,
    details: template.details,
  };
}
