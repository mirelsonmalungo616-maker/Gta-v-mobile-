/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useEffect, useRef } from 'react';
import { 
  Phone, Home, Car, Shield, Zap, Award, Volume2, VolumeX, 
  Skull, Sparkles, Plus, Heart, MapPin, Coins, Target, X, 
  ChevronUp, RefreshCw, KeyRound, Swords, Compass
} from 'lucide-react';

import { WeaponType, Weapon, GamePlayer, GameCar, Pedestrian, Bullet, Explosion, Particle, Mission } from './game/types';
import { CAR_CATALOG } from './game/carCatalog';
import { HOUSE_CATALOG } from './game/houseCatalog';
import { sound } from './game/sound';
import { 
  CITY_BUILDINGS, ROAD_AVENUES_H, ROAD_AVENUES_V, ROAD_WIDTH, 
  checkBuildingCollision, getRandomRoadPoint, CITY_SIZE, 
  AMMU_NATION_LOCATION, CAR_DEALER_LOCATION 
} from './game/city';
import { drawCityBase, drawBuildings3D, drawCars, drawVfx, drawMiniMap } from './game/canvas';
import { generateProceduralMission } from './game/missions';

export default function App() {
  // --- React UI Mirror States ---
  const [cash, setCash] = useState(12500);
  const [xp, setXp] = useState(0);
  const [level, setLevel] = useState(1);
  const [health, setHealth] = useState(100);
  const [armor, setArmor] = useState(50);
  const [wantedLevel, setWantedLevel] = useState(0);
  const [activeWeapon, setActiveWeapon] = useState<WeaponType>(WeaponType.FISTS);
  const [unlockedWeapons, setUnlockedWeapons] = useState<WeaponType[]>([WeaponType.FISTS]);
  const [weaponsAmmo, setWeaponsAmmo] = useState<Record<WeaponType, number>>({
    [WeaponType.FISTS]: 0,
    [WeaponType.PISTOL]: 36,
    [WeaponType.SMG]: 90,
    [WeaponType.SHOTGUN]: 12,
    [WeaponType.RPG]: 3,
  });

  const [inCar, setInCar] = useState(false);
  const [ownedCars, setOwnedCars] = useState<string[]>([]);
  const [ownedHouses, setOwnedHouses] = useState<string[]>([]);
  const [activeMission, setActiveMission] = useState<Mission | null>(null);
  const [playTime, setPlayTime] = useState(0);

  // Sound Config
  const [soundEnabled, setSoundEnabled] = useState(true);

  // Phone / Smartphone Overlay Menu
  const [phoneOpen, setPhoneOpen] = useState(false);
  const [phoneApp, setPhoneApp] = useState<'home' | 'missions' | 'dealer' | 'weapons' | 'safehouse' | 'cheats'>('home');
  const [cheatCode, setCheatCode] = useState('');
  const [cheatMessage, setCheatMessage] = useState('');

  // Big Map modal
  const [bigMapOpen, setBigMapOpen] = useState(false);

  // Alert system (GTA waste overlay)
  const [screenOverlay, setScreenOverlay] = useState<'none' | 'wasted' | 'busted' | 'vitoria'>('none');
  const [hudAlert, setHudAlert] = useState<string | null>(null);

  // --- Core Game Engine Refs (For Zero-Lag Animation Physics) ---
  const mainCanvasRef = useRef<HTMLCanvasElement | null>(null);
  const miniMapCanvasRef = useRef<HTMLCanvasElement | null>(null);

  // Physics states updated immediately inside the loops
  const playerRef = useRef<GamePlayer>({
    x: 900,
    y: 1100,
    angle: 0,
    speed: 0,
    health: 100,
    armor: 50,
    cash: 12500,
    xp: 0,
    level: 1,
    playTime: 0,
    activeWeapon: WeaponType.FISTS,
    weapons: {} as any,
    inCar: false,
    currentCarId: null,
    ownedCars: [],
    ownedHouses: [],
    wantedLevel: 0,
    wantedPoints: 0,
    isDead: false,
    activeMissionId: null,
    missionProgress: 0,
  });

  const carsRef = useRef<GameCar[]>([]);
  const pedestriansRef = useRef<Pedestrian[]>([]);
  const bulletsRef = useRef<Bullet[]>([]);
  const explosionsRef = useRef<Explosion[]>([]);
  const particlesRef = useRef<Particle[]>([]);
  const keysPressedRef = useRef<Record<string, boolean>>({});
  const mousePosRef = useRef({ x: 0, y: 0 });

  // Joystick state (for mobile controls)
  const joystickRef = useRef({ active: false, startX: 0, startY: 0, curX: 0, curY: 0 });
  const [isMobile, setIsMobile] = useState(false);

  // --- INITIALIZATION ---
  useEffect(() => {
    // Detect mobile touch capability
    const touchCheck = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    setIsMobile(touchCheck);

    // Populate Initial Sidewalk Pedestrians
    const initialPeds: Pedestrian[] = [];
    for (let i = 0; i < 15; i++) {
      const roadPt = getRandomRoadPoint();
      initialPeds.push({
        id: `ped_${i}_${Date.now()}`,
        x: roadPt.x + (Math.random() * 40 - 20),
        y: roadPt.y + (Math.random() * 40 - 20),
        angle: Math.random() * Math.PI * 2,
        speed: 1.0 + Math.random() * 0.8,
        health: 40,
        color: ['#f87171', '#60a5fa', '#34d399', '#fbbf24', '#c084fc', '#f472b6'][Math.floor(Math.random() * 6)],
        state: 'walking',
        fleeTimer: 0,
        targetX: roadPt.x,
        targetY: roadPt.y,
      });
    }
    pedestriansRef.current = initialPeds;

    // Populate Initial Traffic Vehicles
    const initialCars: GameCar[] = [];
    const models = CAR_CATALOG;
    for (let i = 0; i < 7; i++) {
      const roadPt = getRandomRoadPoint();
      const model = models[Math.floor(Math.random() * models.length)];
      initialCars.push({
        id: `car_${i}_${Date.now()}`,
        catalogId: model.id,
        name: model.name,
        brand: model.brand,
        realLifeCounterpart: model.realLifeCounterpart,
        x: roadPt.x,
        y: roadPt.y,
        angle: Math.random() > 0.5 ? 0 : Math.PI / 2,
        speed: 1.0 + Math.random() * 1.5,
        health: 100,
        maxHealth: 100,
        color: model.color,
        maxSpeed: model.maxSpeed,
        acceleration: model.acceleration,
        handling: model.handling,
        isPlayerOwned: false,
        isPolice: false,
      });
    }
    carsRef.current = initialCars;

    // Set up Keyboard handlers
    const handleKeyDown = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase();
      keysPressedRef.current[key] = true;

      // Numeric Quick Weapon Swaps
      if (['1', '2', '3', '4', '5'].includes(key)) {
        const indexMapping = [WeaponType.FISTS, WeaponType.PISTOL, WeaponType.SMG, WeaponType.SHOTGUN, WeaponType.RPG];
        const selected = indexMapping[parseInt(key) - 1];
        if (playerRef.current.weapons[selected]?.unlocked || selected === WeaponType.FISTS) {
          playerRef.current.activeWeapon = selected;
          setActiveWeapon(selected);
        }
      }

      // Enter/Exit Car (Key F)
      if (key === 'f') {
        toggleCarEntrance();
      }

      // Phone trigger shortcut (Arrow Up / Key P)
      if (key === 'p' || key === 'arrowup') {
        setPhoneOpen(prev => !prev);
      }

      // Open visual big map (Key M)
      if (key === 'm') {
        setBigMapOpen(prev => !prev);
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase();
      keysPressedRef.current[key] = false;
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    // Playtime Timer Incrementor
    const playTimer = setInterval(() => {
      playerRef.current.playTime += 1;
      setPlayTime(playerRef.current.playTime);

      // Natural wanted level decay
      const p = playerRef.current;
      if (p.wantedLevel > 0 && p.wantedPoints > 0) {
        p.wantedPoints -= 0.5;
        const newStars = Math.max(0, Math.floor(p.wantedPoints / 15));
        if (newStars !== p.wantedLevel) {
          p.wantedLevel = newStars;
          setWantedLevel(newStars);
        }
      }
    }, 1000);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
      clearInterval(playTimer);
    };
  }, []);

  // Sync initial weapon lists and types
  useEffect(() => {
    const fullWeaponsList: Record<WeaponType, Weapon> = {
      [WeaponType.FISTS]: { type: WeaponType.FISTS, name: 'Soco Inglês 👊', damage: 10, fireRate: 400, ammo: 0, maxAmmo: 0, unlocked: true, price: 0, color: '#f8fafc' },
      [WeaponType.PISTOL]: { type: WeaponType.PISTOL, name: 'Pistola Colt M1911 🔫', damage: 25, fireRate: 350, ammo: 36, maxAmmo: 120, unlocked: false, price: 1500, color: '#94a3b8' },
      [WeaponType.SMG]: { type: WeaponType.SMG, name: 'SMG Uzi Tática 🖲️', damage: 18, fireRate: 110, ammo: 90, maxAmmo: 300, unlocked: false, price: 4500, color: '#38bdf8' },
      [WeaponType.SHOTGUN]: { type: WeaponType.SHOTGUN, name: 'Shotgun Escopeta Cal.12 💥', damage: 70, fireRate: 850, ammo: 12, maxAmmo: 48, unlocked: false, price: 8000, color: '#fb923c' },
      [WeaponType.RPG]: { type: WeaponType.RPG, name: 'RPG Lança-Foguete 🚀', damage: 150, fireRate: 1800, ammo: 3, maxAmmo: 10, unlocked: false, price: 20000, color: '#f43f5e' },
    };
    playerRef.current.weapons = fullWeaponsList;
  }, []);

  // Sound enable hook listener
  useEffect(() => {
    sound.toggle(soundEnabled);
  }, [soundEnabled]);

  // Alert dismisser
  const showAlert = (msg: string) => {
    setHudAlert(msg);
    setTimeout(() => setHudAlert(null), 4500);
  };

  // --- DYNAMIC MISSION TRIGGERS & CHEATS ----
  const startProceduralMission = () => {
    if (activeMission) {
      showAlert('Você já possui uma missão em progresso!');
      return;
    }
    const p = playerRef.current;
    const proc = generateProceduralMission(p.x, p.y, p.level, p.playTime);
    setActiveMission({ ...proc, status: 'active' });
    p.activeMissionId = proc.id;
    sound.playCash();
    showAlert(`Missão Iniciada: ${proc.title}! Cheque o Mini-mapa.`);
    setPhoneOpen(false);

    // If chase type, generate the target host vehicle down the block
    if (proc.type === 'chase' && proc.targetX && proc.targetY) {
      const activeRef = carsRef.current;
      // Spawn target supercar
      activeRef.push({
        id: 'mission_target_car',
        catalogId: 'supra',
        name: 'Toyota Supra MK4 (FUGITIVO)',
        brand: 'Toyota',
        realLifeCounterpart: 'Supra MK4',
        x: proc.targetX,
        y: proc.targetY,
        angle: Math.random() * Math.PI,
        speed: 2.5,
        health: 220, // armored
        maxHealth: 220,
        color: '#dc2626', // Bright red
        maxSpeed: 7.0,
        acceleration: 0.18,
        handling: 0.05,
        isPlayerOwned: false,
        isPolice: false,
      });
    }
  };

  const handleDeclineMission = () => {
    setActiveMission(null);
    playerRef.current.activeMissionId = null;
    showAlert('Missão recusada pelo telefone.');
    setPhoneOpen(false);
  };

  // Weapon Ammu-Nation Buying App
  const buyWeapon = (type: WeaponType, cost: number) => {
    const p = playerRef.current;
    if (p.cash < cost) {
      showAlert('Saldo Insuficiente na Carteira!');
      return;
    }
    const wep = p.weapons[type];
    if (!wep.unlocked) {
      wep.unlocked = true;
      p.cash -= cost;
      wep.ammo = Math.floor(wep.maxAmmo * 0.5) || 20;
      setUnlockedWeapons(prev => [...prev, type]);
      setCash(p.cash);
      setWeaponsAmmo({ ...weaponsAmmo, [type]: wep.ammo });
      sound.playCash();
      showAlert(`${wep.name} Desbloqueada com Sucesso!`);
    } else {
      // already unlocked: buy ammunition
      const ammoCost = Math.round(cost * 0.2);
      if (p.cash < ammoCost) {
        showAlert('Saldo Insuficiente para Munição!');
        return;
      }
      if (wep.ammo >= wep.maxAmmo) {
        showAlert('Munição já está no Máximo!');
        return;
      }
      p.cash -= ammoCost;
      wep.ammo = Math.min(wep.maxAmmo, wep.ammo + Math.round(wep.maxAmmo * 0.35));
      setCash(p.cash);
      setWeaponsAmmo({ ...weaponsAmmo, [type]: wep.ammo });
      sound.playCash();
      showAlert(`Munição comprada para ${wep.name}`);
    }
  };

  // Simeon Dealership Cars App
  const buyCar = (catalogId: string, cost: number) => {
    const p = playerRef.current;
    if (p.cash < cost) {
      showAlert('Saldo Insuficiente na Concessionária!');
      return;
    }
    p.cash -= cost;
    p.ownedCars.push(catalogId);
    setCash(p.cash);
    setOwnedCars([...p.ownedCars]);
    sound.playCash();
    showAlert(`Carro de Luxo Comprado! Adicionado na garagem.`);

    // Spawn bought vehicle next to Dealer Simeon
    const chosen = CAR_CATALOG.find(c => c.id === catalogId);
    if (chosen) {
      carsRef.current.push({
        id: `spawned_${Date.now()}`,
        catalogId: chosen.id,
        name: chosen.name,
        brand: chosen.brand,
        realLifeCounterpart: chosen.realLifeCounterpart,
        x: CAR_DEALER_LOCATION.x + 50 + (Math.random() * 40),
        y: CAR_DEALER_LOCATION.y + 110 + (Math.random() * 40),
        angle: 0,
        speed: 0,
        health: 120,
        maxHealth: 120,
        color: chosen.color,
        maxSpeed: chosen.maxSpeed,
        acceleration: chosen.acceleration,
        handling: chosen.handling,
        isPlayerOwned: true,
        isPolice: false,
      });
    }
  };

  // Buy Real-Estate App
  const buyRealEstate = (houseId: string, cost: number) => {
    const p = playerRef.current;
    if (p.cash < cost) {
      showAlert('Dinheiro insuficiente para comprar esta Safehouse!');
      return;
    }
    if (p.ownedHouses.includes(houseId)) {
      showAlert('Você já é dono desta propriedade!');
      return;
    }
    p.cash -= cost;
    p.ownedHouses.push(houseId);
    setCash(p.cash);
    setOwnedHouses([...p.ownedHouses]);
    sound.playCash();
    showAlert(`Safehouse adquirida! Curta sua nova garagem e cure-se.`);
  };

  // Cheat code execution Drawer
  const submitCheatCode = () => {
    const p = playerRef.current;
    const clean = cheatCode.toLowerCase().trim();
    setCheatCode('');

    if (clean === 'viva') {
      p.health = 100;
      p.armor = 100;
      p.wantedLevel = 0;
      p.wantedPoints = 0;
      setHealth(100);
      setArmor(100);
      setWantedLevel(0);
      if (p.inCar && p.currentCarId) {
        const myCar = carsRef.current.find(v => v.id === p.currentCarId);
        if (myCar) myCar.health = myCar.maxHealth;
      }
      sound.playCash();
      setCheatMessage('CHEAT ATIVADO: Vida, colete e veículo totalmente reparados e limpos!');
    } else if (clean === 'gta5') {
      p.cash += 90000;
      p.level += 3;
      p.xp = 0;
      // Unlock all weapons
      const updatedList = [WeaponType.FISTS, WeaponType.PISTOL, WeaponType.SMG, WeaponType.SHOTGUN, WeaponType.RPG];
      updatedList.forEach(w => {
        p.weapons[w].unlocked = true;
        p.weapons[w].ammo = p.weapons[w].maxAmmo;
      });
      setCash(p.cash);
      setLevel(p.level);
      setXp(0);
      setUnlockedWeapons(updatedList);
      setWeaponsAmmo({
        [WeaponType.FISTS]: 0,
        [WeaponType.PISTOL]: 120,
        [WeaponType.SMG]: 300,
        [WeaponType.SHOTGUN]: 48,
        [WeaponType.RPG]: 10,
      });
      sound.playCash();
      setCheatMessage('SUPER CHEAT ATIVADO: +90 mil dólares, +3 níveis e arsenal tático destramado!');
    } else if (clean === 'supermaquina') {
      // Spawns Aventador supercar instantly near player
      const rawC = CAR_CATALOG.find(x => x.id === 'aventador')!;
      carsRef.current.push({
        id: `spawned_cheat_${Date.now()}`,
        catalogId: 'aventador',
        name: rawC.name,
        brand: rawC.brand,
        realLifeCounterpart: rawC.realLifeCounterpart,
        x: p.x + 50 * Math.cos(p.angle),
        y: p.y + 50 * Math.sin(p.angle),
        angle: p.angle,
        speed: 0,
        health: 120,
        maxHealth: 120,
        color: '#f97316',
        maxSpeed: rawC.maxSpeed,
        acceleration: rawC.acceleration,
        handling: rawC.handling,
        isPlayerOwned: true,
        isPolice: false,
      });
      sound.playCash();
      setCheatMessage('CHEAT ATIVADO: Spawmado Lamborghini Aventador SVJ laranja em sua frente!');
    } else if (clean === 'policia') {
      p.wantedLevel = 3;
      p.wantedPoints = 45;
      setWantedLevel(3);
      setCheatMessage('CHEAT ATIVADO: Nível de procura de 3 estrelas iniciado! A polícia está a caminho.');
    } else {
      setCheatMessage('Senha de Trapaça Incorreta! Tente: VIVA, GTA5, SUPERMAQUINA, POLICIA');
    }

    setTimeout(() => setCheatMessage(''), 6000);
  };

  // --- ENGINE CAR INTERACTIONS ---
  const toggleCarEntrance = () => {
    const p = playerRef.current;
    if (p.inCar) {
      // Exit active vehicle
      p.inCar = false;
      setInCar(false);
      sound.stopContinuousEngine();

      const activeCar = carsRef.current.find(c => c.id === p.currentCarId);
      if (activeCar) {
        activeCar.speed = 0; // stop vehicle
        // Eject slightly next to vehicle door side
        p.x = activeCar.x + 22 * Math.cos(activeCar.angle - Math.PI / 2);
        p.y = activeCar.y + 22 * Math.sin(activeCar.angle - Math.PI / 2);
      }
      p.currentCarId = null;
      showAlert('Você saiu do veículo.');
    } else {
      // Look for the closest car inside a radius of 45 pixels
      let closestCar: GameCar | null = null;
      let minDist = 45;

      for (const car of carsRef.current) {
        const dx = car.x - p.x;
        const dy = car.y - p.y;
        const d = Math.sqrt(dx * dx + dy * dy);
        if (d < minDist) {
          minDist = d;
          closestCar = car;
        }
      }

      if (closestCar) {
        p.inCar = true;
        p.currentCarId = closestCar.id;
        setInCar(true);
        sound.startContinuousEngine();
        showAlert(`Entrou no veículo: ${closestCar.brand} ${closestCar.realLifeCounterpart}!`);

        // If cop car, turn on lights
        if (closestCar.isPolice) {
          showAlert('Viatura policial requisitada! Sirenes ativadas automaticamente.');
        }
      } else {
        showAlert('Nenhum carro por perto! Aproxime-se de um veículo e aperte F.');
      }
    }
  };

  // --- ANIMATED WEB GAME PHYSICS LOOP ---
  useEffect(() => {
    let animId: number;
    let lastTime = performance.now();

    const updateGameFrame = (time: number) => {
      const dt = Math.min(100, time - lastTime) / 16.666; // Normalized frame friction
      lastTime = time;

      const p = playerRef.current;
      const cars = carsRef.current;
      const peds = pedestriansRef.current;
      const bullets = bulletsRef.current;
      const explosions = explosionsRef.current;
      const particles = particlesRef.current;

      // Handle screen overlay lockouts
      if (p.health <= 0) {
        if (screenOverlay === 'none') {
          p.isDead = true;
          sound.playMissionFail();
          sound.stopContinuousEngine();
          setScreenOverlay('wasted');
          setTimeout(() => {
            // Respawn safehouse
            p.health = 100;
            p.armor = 20;
            p.x = 450; 
            p.y = 850; // Studio downtown respawn checkpoint
            p.isDead = false;
            p.inCar = false;
            p.currentCarId = null;
            p.wantedLevel = 0;
            p.wantedPoints = 0;
            setHealth(100);
            setArmor(20);
            setWantedLevel(0);
            setInCar(false);
            setScreenOverlay('none');
          }, 3500);
        }
        return;
      }

      // --- PLAYER ON-FOOT MOVEMENT PHYSICS ---
      if (!p.inCar) {
        let dx = 0;
        let dy = 0;

        if (keysPressedRef.current['w'] || keysPressedRef.current['arrowup']) dy = -1;
        if (keysPressedRef.current['s'] || keysPressedRef.current['arrowdown']) dy = 1;
        if (keysPressedRef.current['a'] || keysPressedRef.current['arrowleft']) dx = -1;
        if (keysPressedRef.current['d'] || keysPressedRef.current['arrowright']) dx = 1;

        // Joystick mapping (optional overlay checks)
        if (joystickRef.current.active) {
          const stick = joystickRef.current;
          const sx = stick.curX - stick.startX;
          const sy = stick.curY - stick.startY;
          const len = Math.sqrt(sx * sx + sy * sy);
          if (len > 5) {
            dx = sx / len;
            dy = sy / len;
          }
        }

        if (dx !== 0 || dy !== 0) {
          const moveSpeed = 3.2 * dt;
          const length = Math.sqrt(dx * dx + dy * dy);
          
          let nextX = p.x + (dx / length) * moveSpeed;
          let nextY = p.y + (dy / length) * moveSpeed;

          // Check obstacles building collisions
          const collideX = checkBuildingCollision(nextX, p.y, 8);
          if (!collideX.hit) {
            p.x = nextX;
          }
          const collideY = checkBuildingCollision(p.x, nextY, 8);
          if (!collideY.hit) {
            p.y = nextY;
          }

          // Orient walking angle
          p.angle = Math.atan2(dy, dx);

          // Spawn neat foot trail dirt particles occasionally
          if (Math.random() < 0.08) {
            particles.push({
              id: `${Math.random()}`,
              x: p.x,
              y: p.y,
              vx: -dx * 0.5,
              vy: -dy * 0.5,
              color: 'rgba(200, 200, 200, 0.25)',
              size: 2,
              life: 1.0,
              decay: 0.05,
            });
          }
        }
      } 
      
      // --- PLAYER DRIVING PHYSICS ---
      else if (p.inCar && p.currentCarId) {
        const activeCar = cars.find(car => car.id === p.currentCarId);
        if (activeCar) {
          // Sync coordinates
          p.x = activeCar.x;
          p.y = activeCar.y;
          p.angle = activeCar.angle;

          // Process drives keys
          let driveForce = 0;
          let steer = 0;

          if (keysPressedRef.current['w']) driveForce = 1;
          if (keysPressedRef.current['s']) driveForce = -0.52; // Brake/reversing
          if (keysPressedRef.current['a']) steer = -1;
          if (keysPressedRef.current['d']) steer = 1;

          // Mobile touch steering check
          if (joystickRef.current.active) {
            const sx = joystickRef.current.curX - joystickRef.current.startX;
            const sy = joystickRef.current.curY - joystickRef.current.startY;
            if (Math.abs(sx) > 8) steer = sx > 0 ? 1 : -1;
            if (sy < -8) driveForce = 1;
            if (sy > 8) driveForce = -0.5;
          }

          // Accelerate vehicle speed scalar
          if (driveForce > 0) {
            activeCar.speed += activeCar.acceleration * driveForce * dt;
            if (activeCar.speed > activeCar.maxSpeed) activeCar.speed = activeCar.maxSpeed;
          } else if (driveForce < 0) {
            activeCar.speed += activeCar.acceleration * driveForce * dt; // brake/reversing decelerator
            if (activeCar.speed < -activeCar.maxSpeed * 0.35) activeCar.speed = -activeCar.maxSpeed * 0.35;
            
            // Sound effects for brake tire screech
            if (activeCar.speed > 1.5 && Math.random() < 0.2) {
              sound.playTireScreech();
            }
          } else {
            // Neutral motor friction drag
            activeCar.speed *= Math.pow(0.965, dt);
            if (Math.abs(activeCar.speed) < 0.1) activeCar.speed = 0;
          }

          // Steer angles (only works if moving!)
          if (Math.abs(activeCar.speed) > 0.25) {
            const direction = activeCar.speed > 0 ? 1 : -1;
            const steeringRadian = activeCar.handling * steer * direction * dt;
            activeCar.angle += steeringRadian;

            // Generate tire track soot drifting lines when steering at speeds
            const isDrifting = keysPressedRef.current[' '] || Math.abs(steer) > 0.8 && activeCar.speed > 4.5;
            if (isDrifting) {
              activeCar.speed *= Math.pow(0.97, dt); // handbrake slide reduces swift velocity
              sound.playTireScreech();

              // Spawn exhaust black soot smokes
              particles.push({
                id: `${Math.random()}`,
                x: activeCar.x - 14 * Math.cos(activeCar.angle),
                y: activeCar.y - 14 * Math.sin(activeCar.angle),
                vx: -activeCar.speed * 0.1 * Math.cos(activeCar.angle) + (Math.random() * 0.4 - 0.2),
                vy: -activeCar.speed * 0.1 * Math.sin(activeCar.angle) + (Math.random() * 0.4 - 0.2),
                color: 'rgba(50, 50, 50, 0.4)',
                size: 3 + Math.random() * 3,
                life: 0.82,
                decay: 0.04,
              });
            }
          }

          // Calculate displacement trajectory
          const speedVector = activeCar.speed * dt;
          const nx = activeCar.x + Math.cos(activeCar.angle) * speedVector;
          const ny = activeCar.y + Math.sin(activeCar.angle) * speedVector;

          // Check buildings collisions
          const collideX = checkBuildingCollision(nx, activeCar.y, 14);
          if (!collideX.hit) {
            activeCar.x = nx;
          } else {
            // Crash vehicle and halt speed
            if (Math.abs(activeCar.speed) > 2.0) {
              activeCar.health -= Math.abs(activeCar.speed) * 4;
              sound.playExplosion(); // mini thud
              // Spark particles
              for (let k = 0; k < 6; k++) {
                particles.push({
                  id: `${Math.random()}`,
                  x: activeCar.x,
                  y: activeCar.y,
                  vx: (Math.random() * 4 - 2),
                  vy: (Math.random() * 4 - 2),
                  color: '#fbbf24',
                  size: 2,
                  life: 0.9,
                  decay: 0.07,
                });
              }
            }
            activeCar.speed = -activeCar.speed * 0.25; // bounce slightly backward
          }

          const collideY = checkBuildingCollision(activeCar.x, ny, 14);
          if (!collideY.hit) {
            activeCar.y = ny;
          } else {
            if (Math.abs(activeCar.speed) > 2.0) {
              activeCar.health -= Math.abs(activeCar.speed) * 4;
              sound.playExplosion();
            }
            activeCar.speed = -activeCar.speed * 0.25;
          }

          // Update continuous synchronized motor hum pitches
          const ratio = Math.abs(activeCar.speed) / activeCar.maxSpeed;
          sound.updateContinuousEngine(ratio);

          // If vehicle blows up
          if (activeCar.health <= 0) {
            p.health -= 60; // large damage penalty to passenger
            sound.playExplosion();
            // Trigger explosion VFX
            explosions.push({ id: `exp_${Date.now()}`, x: activeCar.x, y: activeCar.y, radius: 45, life: 1.0 });
            // Remove vehicle from index
            carsRef.current = cars.filter(v => v.id !== activeCar.id);
            p.inCar = false;
            setInCar(false);
            p.currentCarId = null;
            showAlert('Seu veículo explodiu! Fuja rápido!');
          }
        }
      }

      // --- BULLETS AND FIREBALLS SIMULATOR LOOP ---
      for (let i = bullets.length - 1; i >= 0; i--) {
        const b = bullets[i];
        b.x += b.vx * dt;
        b.y += b.vy * dt;
        b.rangeRemaining -= dt;

        // Wall collisions
        const overlap = checkBuildingCollision(b.x, b.y, 2);
        let impact = false;

        if (overlap.hit || b.rangeRemaining <= 0) {
          impact = true;
        } else {
          // Check hits against pedestrians
          for (const ped of peds) {
            if (ped.state !== 'dead') {
              const dx = ped.x - b.x;
              const dy = ped.y - b.y;
              if (dx * dx + dy * dy < 8 * 8) {
                ped.health -= b.damage;
                ped.state = 'fleeing';
                ped.fleeTimer = 180;
                impact = true;

                // Spawn blood crimson particles
                for (let k = 0; k < 4; k++) {
                  particles.push({
                    id: `${Math.random()}`,
                    x: ped.x,
                    y: ped.y,
                    vx: (Math.random() * 2 - 1),
                    vy: (Math.random() * 2 - 1),
                    color: '#ef4444',
                    size: 2,
                    life: 0.8,
                    decay: 0.05,
                  });
                }

                // Give player wanted points if they attacked innocent people civs
                if (b.firedByPlayer) {
                  p.wantedPoints += 12;
                  const newStars = Math.min(5, Math.floor(p.wantedPoints / 15));
                  if (newStars !== p.wantedLevel) {
                    p.wantedLevel = newStars;
                    setWantedLevel(newStars);
                  }
                }
                break;
              }
            }
          }

          // Check hits against passenger vehicles
          for (const car of cars) {
            if (car.id !== p.currentCarId) {
              const dx = car.x - b.x;
              const dy = car.y - b.y;
              if (dx * dx + dy * dy < 20 * 20) {
                car.health -= b.damage;
                impact = true;

                // Spark sparks
                for (let k = 0; k < 3; k++) {
                  particles.push({
                    id: `${Math.random()}`,
                    x: b.x,
                    y: b.y,
                    vx: (Math.random() * 2 - 1),
                    vy: (Math.random() * 2 - 1),
                    color: '#fbbf24',
                    size: 1.5,
                    life: 0.6,
                    decay: 0.08,
                  });
                }
                break;
              }
            }
          }

          // Check hits against the player (fired by hostile gang wave or police officers)
          if (!b.firedByPlayer) {
            const dx = p.x - b.x;
            const dy = p.y - b.y;
            if (dx * dx + dy * dy < 10 * 10) {
              const dmg = b.damage;
              if (p.armor > 0) {
                p.armor = Math.max(0, p.armor - dmg);
              } else {
                p.health = Math.max(0, p.health - dmg);
              }
              impact = true;
              break;
            }
          }
        }

        if (impact) {
          bullets.splice(i, 1);
        }
      }

      // --- EXPLOSIONS VECTOR ACCELERATOR ---
      for (let i = explosions.length - 1; i >= 0; i--) {
        const exp = explosions[i];
        exp.life -= 0.05 * dt;

        // Damage everything nearby on peak blast
        if (exp.life > 0.8) {
          // Check player proximity
          const pdx = p.x - exp.x;
          const pdy = p.y - exp.y;
          const pdist = Math.sqrt(pdx * pdx + pdy * pdy);
          if (pdist < exp.radius) {
            const factor = (exp.radius - pdist) / exp.radius;
            const finalDmg = Math.round(110 * factor);
            if (p.armor > 0) {
              p.armor = Math.max(0, p.armor - finalDmg);
            } else {
              p.health = Math.max(0, p.health - finalDmg);
            }
          }

          // Check vehicles
          for (const car of cars) {
            const cdx = car.x - exp.x;
            const cdy = car.y - exp.y;
            const cdist = Math.sqrt(cdx * cdx + cdy * cdy);
            if (cdist < exp.radius) {
              car.health -= 120; // high combust explosive damage
            }
          }

          // Check pedestrians
          for (const ped of peds) {
            const cdx = ped.x - exp.x;
            const cdy = ped.y - exp.y;
            const cdist = Math.sqrt(cdx * cdx + cdy * cdy);
            if (cdist < exp.radius) {
              ped.health -= 150;
            }
          }
        }

        if (exp.life <= 0) {
          explosions.splice(i, 1);
        }
      }

      // --- PARTICLES ENGINE ---
      for (let i = particles.length - 1; i >= 0; i--) {
        const part = particles[i];
        part.x += part.vx * dt;
        part.y += part.vy * dt;
        part.life -= part.decay * dt;
        if (part.life <= 0) {
          particles.splice(i, 1);
        }
      }

      // --- PEDESTRIANS AI COGNITION ---
      for (const ped of peds) {
        if (ped.state === 'dead') continue;

        // Fleeing timer decayer
        if (ped.fleeTimer > 0) {
          ped.fleeTimer -= dt;
          if (ped.fleeTimer <= 0) ped.state = 'walking';
        }

        let speed = ped.speed;
        let pAngle = ped.angle;

        if (ped.state === 'fleeing') {
          // Run quickly away from player coordinate
          const dx = ped.x - p.x;
          const dy = ped.y - p.y;
          pAngle = Math.atan2(dy, dx);
          speed = ped.speed * 2.2;
        } else {
          // Head toward sidewalk checkpoints procedurally
          const dx = ped.targetX - ped.x;
          const dy = ped.targetY - ped.y;
          const d = Math.sqrt(dx * dx + dy * dy);
          if (d < 15) {
            const nextNode = getRandomRoadPoint();
            ped.targetX = nextNode.x + (Math.random() * 30 - 15);
            ped.targetY = nextNode.y + (Math.random() * 30 - 15);
          }
          pAngle = Math.atan2(dy, dx);
        }

        ped.angle = pAngle;
        const dX = Math.cos(pAngle) * speed * dt;
        const dY = Math.sin(pAngle) * speed * dt;

        const collideX = checkBuildingCollision(ped.x + dX, ped.y, 4);
        if (!collideX.hit) ped.x += dX;

        const collideY = checkBuildingCollision(ped.x, ped.y + dY, 4);
        if (!collideY.hit) ped.y += dY;

        // Death conversion
        if (ped.health <= 0) {
          ped.state = 'dead';
          p.cash += 80; // pick up loose dollar bills!
          p.xp += 15;
          setCash(p.cash);
          showAlert('Inocente derrubado! Você recolheu $80 soltos no asfalto.');
          
          // Check level calculations
          checkLevelUps();
        }
      }

      // --- STREETS TRAFFIC CARS AUTOMATIC AI ---
      for (const car of cars) {
        if (car.id === p.currentCarId) continue; // passenger car driven

        if (car.isPolice) {
          // Police pursue AI: steers aggressively directly towards player's position
          const dx = p.x - car.x;
          const dy = p.y - car.y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          car.angle = Math.atan2(dy, dx);
          car.speed = 5.2; // faster cop speed

          const mx = car.x + Math.cos(car.angle) * car.speed * dt;
          const my = car.y + Math.sin(car.angle) * car.speed * dt;

          const collideX = checkBuildingCollision(mx, car.y, 14);
          if (!collideX.hit) car.x = mx;

          const collideY = checkBuildingCollision(car.x, my, 14);
          if (!collideY.hit) car.y = my;

          // Ram vehicle damage logic
          if (dist < 25) {
            p.health -= 5; // blunt body micro impact cost
            car.health -= 10;
            sound.playExplosion(); // thud

            if (p.inCar && p.currentCarId) {
              const myVehicle = cars.find(v => v.id === p.currentCarId);
              if (myVehicle) myVehicle.health -= 25;
            }

            // Reposition cops slightly backward
            car.x -= Math.cos(car.angle) * 15;
            car.y -= Math.sin(car.angle) * 15;
          }

          // Cop shoots at player if nearby and player is on foot
          if (dist > 50 && dist < 220 && Math.random() < 0.04) {
            const radAngle = Math.atan2(p.y - car.y, p.x - car.x);
            bullets.push({
              id: `${Math.random()}`,
              x: car.x,
              y: car.y,
              vx: Math.cos(radAngle) * 9,
              vy: Math.sin(radAngle) * 9,
              damage: 6,
              firedByPlayer: false,
              rangeRemaining: 24,
            });
            sound.playShoot(WeaponType.PISTOL);
          }
          continue;
        }

        // Standard Civilian drive pattern AI
        let curSpeed = car.speed;
        const dx = Math.cos(car.angle) * curSpeed * dt;
        const dy = Math.sin(car.angle) * curSpeed * dt;

        const collideX = checkBuildingCollision(car.x + dx, car.y, 14);
        if (!collideX.hit) {
          car.x += dx;
        } else {
          // turn around or back-off
          car.angle += Math.PI / 2;
        }

        const collideY = checkBuildingCollision(car.x, car.y + dy, 14);
        if (!collideY.hit) {
          car.y += dy;
        } else {
          car.angle += Math.PI / 2;
        }

        // Limit city size boundaries so they do not fall off limits
        if (car.x < 20 || car.x > CITY_SIZE - 20 || car.y < 20 || car.y > CITY_SIZE - 20) {
          car.angle += Math.PI;
        }
      }

      // --- POLICE FORCE SPAWN CHANNELS ---
      // If wanted level is active, spawn police car occasionally near player
      if (p.wantedLevel > 0 && cars.filter(c => c.isPolice).length < p.wantedLevel) {
        if (Math.random() < 0.01) { // 1% chance per frame
          const spawnPt = getRandomRoadPoint();
          // Verify cop car is generated outside immediate sight player box
          const dis = Math.sqrt(Math.pow(spawnPt.x - p.x, 2) + Math.pow(spawnPt.y - p.y, 2));
          if (dis > 250) {
            cars.push({
              id: `cop_car_${Date.now()}`,
              catalogId: 'wrangler',
              name: 'Ford Crown Victoria Viatura',
              brand: 'POLÍCIA L.S.',
              realLifeCounterpart: 'Police Interceptor',
              x: spawnPt.x,
              y: spawnPt.y,
              angle: 0,
              speed: 0,
              health: 120,
              maxHealth: 120,
              color: '#020617', // slick tactical dark black cop coat
              maxSpeed: 6.2,
              acceleration: 0.22,
              handling: 0.065,
              isPlayerOwned: false,
              isPolice: true,
            });
            showAlert('SINAIS RECONHECIDOS: Viaturas da polícia despachadas em sua direção!');
          }
        }
      }

      // --- ACTIVE PROCURAL MISSION COGNITION TRACKERS ---
      if (activeMission && activeMission.status === 'active') {
        const m = activeMission;

        // Substract counting limit timer on timed levels
        if (m.timeRemaining !== undefined) {
          m.timeRemaining -= dt * 0.01666; // seconds decayer
          if (m.timeRemaining <= 0) {
            // Unsuccessful mission fail state
            sound.playMissionFail();
            m.status = 'failed';
            setActiveMission({ ...m });
            p.activeMissionId = null;
            showAlert('Péssimas Notícias! O tempo da missão esgotou e você falhou.');
          }
        }

        // Mode delivery (contrabando) tracker
        if (m.type === 'delivery' && m.checkpointX && m.checkpointY) {
          if (!m.hasPickedUp) {
            const dis = Math.sqrt(Math.pow(p.x - m.checkpointX, 2) + Math.pow(p.y - m.checkpointY, 2));
            if (dis < 40) {
              m.hasPickedUp = true;
              sound.playCash();
              showAlert('Carga de Contrabando Coletada! Siga até o Porto Amarelo para finalizar!');
              setActiveMission({ ...m });
            }
          } else if (m.targetX && m.targetY) {
            const dis = Math.sqrt(Math.pow(p.x - m.targetX, 2) + Math.pow(p.y - m.targetY, 2));
            if (dis < 40) {
              // Completed!
              completeActiveMission();
            }
          }
        }

        // Mode item_pickup courier tracker
        else if (m.type === 'item_pickup' && m.checkpointX && m.checkpointY) {
          // Pickup 3 dynamic letters: on high-fidelity, let's treat the pre-checkpoint as a sub stage
          const dis = Math.sqrt(Math.pow(p.x - m.checkpointX, 2) + Math.pow(p.y - m.checkpointY, 2));
          if (!m.hasPickedUp && dis < 35) {
            m.hasPickedUp = true;
            sound.playCash();
            showAlert('Sub-pacotes recolhidos nos becos! Entregue ao receptor no círculo amarelo.');
            setActiveMission({ ...m });
          } else if (m.hasPickedUp && m.targetX && m.targetY) {
            const disFinal = Math.sqrt(Math.pow(p.x - m.targetX, 2) + Math.pow(p.y - m.targetY, 2));
            if (disFinal < 40) {
              completeActiveMission();
            }
          }
        }

        // Mode high-speed fugitive car chase tracker
        else if (m.type === 'chase') {
          // Find target supercar coordinates
          const targetVehicle = cars.find(v => v.id === 'mission_target_car');
          if (targetVehicle) {
            // Update tracking pointers on map dynamically
            m.targetX = targetVehicle.x;
            m.targetY = targetVehicle.y;
            
            // If target vehicle health is <= 0 (destroyed)
            if (targetVehicle.health <= 0) {
              completeActiveMission();
            }
          } else {
            // Target was already removed means player successfully vaporized it
            completeActiveMission();
          }
        }

        // Mode turf ally alley survival waves tracker
        else if (m.type === 'survival' && m.targetX && m.targetY) {
          const mX = m.targetX;
          const mY = m.targetY;

          // Play active location distance check
          const distArea = Math.sqrt(Math.pow(p.x - mX, 2) + Math.pow(p.y - mY, 2));
          if (distArea < 160) {
            // Spawn hostile attackers inside boundary ring if under capacity
            const activeShooters = peds.filter(x => x.state === 'fleeing' && x.color === '#ef4444');
            if (activeShooters.length < 3 && Math.random() < 0.05) {
              // Generate attacker moving explicitly to player
              const pPtAngle = Math.random() * Math.PI * 2;
              peds.push({
                id: `hostile_${Date.now()}_${Math.random()}`,
                x: mX + 110 * Math.cos(pPtAngle),
                y: mY + 110 * Math.sin(pPtAngle),
                angle: 0,
                speed: 1.5,
                health: 50,
                color: '#ef4444', // Dark Hostile Red
                state: 'fleeing', // run/shoot AI state
                fleeTimer: 999,
                targetX: p.x,
                targetY: p.y,
              });
              
              // Shoot bullet towards player
              bullets.push({
                id: `${Math.random()}`,
                x: mX + 110 * Math.cos(pPtAngle),
                y: mY + 110 * Math.sin(pPtAngle),
                vx: Math.cos(pPtAngle + Math.PI) * 5.0,
                vy: Math.sin(pPtAngle + Math.PI) * 5.0,
                damage: 5,
                firedByPlayer: false,
                rangeRemaining: 35,
              });
              sound.playShoot(WeaponType.PISTOL);
            }

            // Check if timer runs out in turf area
            if (m.timeRemaining !== undefined && m.timeRemaining < 3) {
              completeActiveMission();
            }
          }
        }
      }

      // --- RENDER DOUBLE GRAPHICS ---
      const canvas = mainCanvasRef.current;
      if (canvas) {
        const ctx = canvas.getContext('2d');
        if (ctx) {
          // Camera follow centers on player smooth damping
          const cameraX = p.x - canvas.width / 2;
          const cameraY = p.y - canvas.height / 2;

          drawCityBase(ctx, canvas.width, canvas.height, cameraX, cameraY, ROAD_AVENUES_H, ROAD_AVENUES_V, ROAD_WIDTH);
          
          // Draw drift skid road footprint trails persistence
          // Render buildings
          drawBuildings3D(ctx, CITY_BUILDINGS, cameraX, cameraY, p.x, p.y);

          // Draw walking pedestrians
          ctx.save();
          ctx.translate(-cameraX, -cameraY);
          for (const ped of peds) {
            ctx.save();
            ctx.translate(ped.x, ped.y);
            ctx.rotate(ped.angle);

            // Draw feet
            ctx.fillStyle = '#0f172a';
            if (ped.state !== 'dead') {
              const runOsc = Math.sin(Date.now() * 0.015) * 4;
              ctx.fillRect(-3, -5 + runOsc, 4, 3);
              ctx.fillRect(-3, 3 - runOsc, 4, 3);
            }

            // draw body coat
            ctx.fillStyle = ped.state === 'dead' ? '#991b1b' : ped.color;
            ctx.beginPath();
            ctx.arc(0, 0, 8, 0, Math.PI * 2);
            ctx.fill();

            // Head crown
            ctx.fillStyle = '#fca5a5';
            ctx.beginPath();
            ctx.arc(0, 0, 4, 0, Math.PI * 2);
            ctx.fill();

            ctx.restore();
          }
          ctx.restore();

          // Render active vehicles
          drawCars(ctx, cars, cameraX, cameraY);

          // Render player (on foot)
          if (!p.inCar) {
            ctx.save();
            ctx.translate(-cameraX, -cameraY);
            ctx.translate(p.x, p.y);
            ctx.rotate(p.angle);

            // Feet walk animations
            const isMoving = keysPressedRef.current['w'] || keysPressedRef.current['s'] || keysPressedRef.current['a'] || keysPressedRef.current['d'];
            if (isMoving) {
              const swing = Math.sin(Date.now() * 0.018) * 5;
              ctx.fillStyle = '#000000';
              ctx.fillRect(-4, -6 + swing, 5, 3);
              ctx.fillRect(-4, 3 - swing, 5, 3);
            }

            // Tactical military bullet proof vest
            ctx.fillStyle = '#1e293b'; // slate dark vest belt
            ctx.beginPath();
            ctx.arc(-2, 0, 10, 0, Math.PI * 2);
            ctx.fill();

            // Skin coat hands
            ctx.fillStyle = '#fbcfe8'; // peach
            // Hands holding current select fire weapon
            ctx.beginPath();
            ctx.arc(6, -6, 3, 0, Math.PI * 2);
            ctx.arc(6, 6, 3, 0, Math.PI * 2);
            ctx.fill();

            // Head outline crown
            ctx.fillStyle = '#fbcfe8';
            ctx.beginPath();
            ctx.arc(0, 0, 5, 0, Math.PI * 2);
            ctx.fill();

            // Render weapon barrel extensions pointing forward
            if (p.activeWeapon !== WeaponType.FISTS) {
              ctx.fillStyle = '#475569';
              // Draw barrel extension matching gun classes
              const barrelLen = p.activeWeapon === WeaponType.RPG ? 22 : p.activeWeapon === WeaponType.SHOTGUN ? 18 : 12;
              const barrelThick = p.activeWeapon === WeaponType.RPG ? 4.5 : p.activeWeapon === WeaponType.SMG ? 2 : 3;
              ctx.fillRect(4, -2, barrelLen, barrelThick);
            }

            // Damage impact red circle overlay
            if (p.health < 40 && Math.floor(Date.now() / 200) % 2 === 0) {
              ctx.strokeStyle = '#ef4444';
              ctx.lineWidth = 2;
              ctx.strokeRect(-12, -12, 24, 24);
            }

            ctx.restore();
          }

          // Active delivery/waypoint target overlays on main view
          if (activeMission && activeMission.status === 'active') {
            ctx.save();
            ctx.translate(-cameraX, -cameraY);
            
            // Draw floating green/yellow circle on streets
            if (activeMission.type === 'delivery' || activeMission.type === 'item_pickup') {
              const showChk = !activeMission.hasPickedUp && activeMission.checkpointX;
              const tx = showChk ? activeMission.checkpointX! : activeMission.targetX!;
              const ty = showChk ? activeMission.checkpointY! : activeMission.targetY!;

              const pulseSize = 35 + Math.sin(Date.now() * 0.01) * 6;
              ctx.strokeStyle = showChk ? '#22c55e' : '#eab308';
              ctx.lineWidth = 3;
              ctx.beginPath();
              ctx.arc(tx, ty, pulseSize, 0, Math.PI * 2);
              ctx.stroke();

              // Inner glowing fill
              ctx.fillStyle = showChk ? 'rgba(34,197,94,0.15)' : 'rgba(234,179,8,0.15)';
              ctx.beginPath();
              ctx.arc(tx, ty, pulseSize, 0, Math.PI * 2);
              ctx.fill();

              // Icon letters indicators
              ctx.fillStyle = '#ffffff';
              ctx.font = 'bold 11px sans-serif';
              ctx.textAlign = 'center';
              ctx.fillText(showChk ? 'RECOLHA 📦' : 'ENTREGA 🏁', tx, ty - pulseSize - 8);
            }
            ctx.restore();
          }

          // Render muzzle flashes, RPG trails, and explosions
          drawVfx(ctx, bullets, explosions, particles, cameraX, cameraY);
        }
      }

      // --- SYNC MINI MAP RADAR HUD CANVAS ---
      const miniMap = miniMapCanvasRef.current;
      if (miniMap) {
        drawMiniMap(miniMap, p, CITY_BUILDINGS, cars, activeMission);
      }

      // --- COPY PERFORMANCE REF STATS TO REACT STATE (LIMIT RE-RENDER OVERHEAD) ---
      if (Math.round(performance.now()) % 11 === 0) {
        setHealth(p.health);
        setArmor(p.armor);
        setCash(p.cash);
        setXp(p.xp);
        setLevel(p.level);
      }

      animId = requestAnimationFrame(updateGameFrame);
    };

    animId = requestAnimationFrame(updateGameFrame);
    return () => cancelAnimationFrame(animId);
  }, [activeMission, screenOverlay]);

  // Level Up criteria calculator
  const checkLevelUps = () => {
    const p = playerRef.current;
    const requiredXp = p.level * 1000;
    if (p.xp >= requiredXp) {
      p.xp -= requiredXp;
      p.level += 1;
      p.cash += p.level * 2500; // Large dollar gift
      setCash(p.cash);
      setLevel(p.level);
      setXp(p.xp);
      sound.playMissionSuccess();
      showAlert(`PARABÉNS! Você subiu para o NÍVEL ${p.level} e ganhou Bônus de $${(p.level * 2500).toLocaleString()}! 🌟`);
    }
  };

  const completeActiveMission = () => {
    if (!activeMission) return;
    const p = playerRef.current;
    
    // Gain bonuses cash and experience points
    p.cash += activeMission.reward;
    p.xp += activeMission.xpReward;
    
    setCash(p.cash);
    setXp(p.xp);
    
    sound.playMissionSuccess();
    showAlert(`MISSÃO CUMPRIDA! +$${activeMission.reward.toLocaleString()} • +${activeMission.xpReward} XP! 🎉`);

    activeMission.status = 'completed';
    setActiveMission(null);
    p.activeMissionId = null;

    // Eliminate hostiles from map
    pedestriansRef.current = pedestriansRef.current.filter(x => x.color !== '#ef4444');
    
    // Check level calculations
    checkLevelUps();
  };

  // --- PLAYER TRIGGER DISCHARGES SHOTS ---
  const handleWeaponDischarge = (canvasClickEvent: React.MouseEvent<HTMLCanvasElement>) => {
    const p = playerRef.current;
    if (p.health <= 0 || screenOverlay !== 'none') return;

    sound.init(); // lazy load Audio on click

    // Grab target board coordinate coordinates matching mouse screen coordinate
    const canvas = mainCanvasRef.current;
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const mouseX = canvasClickEvent.clientX - rect.left;
    const mouseY = canvasClickEvent.clientY - rect.top;

    // Translate click to absolute world coordinate
    const cameraX = p.x - canvas.width / 2;
    const cameraY = p.y - canvas.height / 2;
    const absoluteTargetX = mouseX + cameraX;
    const absoluteTargetY = mouseY + cameraY;

    // Calculate angle towards mouse vector
    const shotAngle = Math.atan2(absoluteTargetY - p.y, absoluteTargetX - p.x);
    p.angle = shotAngle; // Face target

    const wep = p.weapons[p.activeWeapon];

    if (p.activeWeapon !== WeaponType.FISTS) {
      if (wep.ammo <= 0) {
        showAlert('SEM MUNIÇÃO! Abra seu Smartphone para comprar mais cartuchos!');
        return;
      }
      // Decrement ammo
      wep.ammo -= 1;
      setWeaponsAmmo({ ...weaponsAmmo, [p.activeWeapon]: wep.ammo });
    }

    // Trigger synthetic audio
    sound.playShoot(p.activeWeapon);

    // Bullet Spawns
    const bRef = bulletsRef.current;
    const bulletSpeed = 15;

    if (p.activeWeapon === WeaponType.FISTS) {
      // Small melee hit box
      bRef.push({
        id: `fist_${Date.now()}`,
        x: p.x + 12 * Math.cos(shotAngle),
        y: p.y + 12 * Math.sin(shotAngle),
        vx: Math.cos(shotAngle) * 5,
        vy: Math.sin(shotAngle) * 5,
        damage: 10,
        firedByPlayer: true,
        rangeRemaining: 3, // very short range
      });
    } 
    
    else if (p.activeWeapon === WeaponType.PISTOL) {
      bRef.push({
        id: `pist_${Date.now()}`,
        x: p.x + 16 * Math.cos(shotAngle),
        y: p.y + 16 * Math.sin(shotAngle),
        vx: Math.cos(shotAngle) * bulletSpeed,
        vy: Math.sin(shotAngle) * bulletSpeed,
        damage: 28,
        firedByPlayer: true,
        rangeRemaining: 20,
      });
    } 
    
    else if (p.activeWeapon === WeaponType.SMG) {
      // Rapid multiple bullets with slight spread inaccuracy
      for (let k = 0; k < 2; k++) {
        const spread = (Math.random() * 0.15 - 0.07);
        bRef.push({
          id: `smg_${Date.now()}_${k}`,
          x: p.x + 16 * Math.cos(shotAngle),
          y: p.y + 16 * Math.sin(shotAngle),
          vx: Math.cos(shotAngle + spread) * (bulletSpeed + 2),
          vy: Math.sin(shotAngle + spread) * (bulletSpeed + 2),
          damage: 16,
          firedByPlayer: true,
          rangeRemaining: 18,
        });
      }
    } 
    
    else if (p.activeWeapon === WeaponType.SHOTGUN) {
      // 5 spread out bullets representing cluster buckshots
      for (let j = 0; j < 5; j++) {
        const spread = (j - 2) * 0.12 + (Math.random() * 0.05 - 0.025);
        bRef.push({
          id: `shot_${Date.now()}_${j}`,
          x: p.x + 16 * Math.cos(shotAngle),
          y: p.y + 16 * Math.sin(shotAngle),
          vx: Math.cos(shotAngle + spread) * (bulletSpeed - 3),
          vy: Math.sin(shotAngle + spread) * (bulletSpeed - 3),
          damage: 20, // 20 * 5 pellets = 100 max damage
          firedByPlayer: true,
          rangeRemaining: 12, // short scatter range
        });
      }
    } 
    
    else if (p.activeWeapon === WeaponType.RPG) {
      // Spawns slow moving explosive rocket bullet
      bRef.push({
        id: `rpg_${Date.now()}`,
        x: p.x + 20 * Math.cos(shotAngle),
        y: p.y + 20 * Math.sin(shotAngle),
        vx: Math.cos(shotAngle) * 7.5,
        vy: Math.sin(shotAngle) * 7.5,
        damage: 120,
        firedByPlayer: true,
        rangeRemaining: 30, // medium range before exploding
      });

      // Detonate and register explosion when bullet expires
      setTimeout(() => {
        // Trigger rocket visual blast
        const targetX = p.x + Math.cos(shotAngle) * 225;
        const targetY = p.y + Math.sin(shotAngle) * 225;
        explosionsRef.current.push({
          id: `rpg_exp_${Date.now()}`,
          x: targetX,
          y: targetY,
          radius: 65,
          life: 1.0,
        });
        sound.playExplosion();
      }, 750);
    }

    // Incriminate player Wanted Points upon discharge inside city roads
    p.wantedPoints += 2.5;
    const calculatedStars = Math.min(5, Math.floor(p.wantedPoints / 15));
    if (calculatedStars !== p.wantedLevel) {
      p.wantedLevel = calculatedStars;
      setWantedLevel(calculatedStars);
    }
  };

  // Virtual touch-joystick tracker dragging functions
  const handleJoystickStart = (e: React.TouchEvent) => {
    const t = e.touches[0];
    joystickRef.current = {
      active: true,
      startX: t.clientX,
      startY: t.clientY,
      curX: t.clientX,
      curY: t.clientY,
    };
  };

  const handleJoystickMove = (e: React.TouchEvent) => {
    if (!joystickRef.current.active) return;
    const t = e.touches[0];
    joystickRef.current.curX = t.clientX;
    joystickRef.current.curY = t.clientY;
  };

  const handleJoystickEnd = () => {
    joystickRef.current.active = false;
  };

  return (
    <div className="relative w-screen h-screen overflow-hidden bg-slate-950 font-sans text-slate-100 flex flex-col md:flex-row select-none">
      {/* --- HUD HEADER BAR / MAIN LEADERBOARD --- */}
      <div className="absolute top-4 right-4 z-20 flex flex-col items-end gap-2 text-right">
        {/* Cash balance display */}
        <div className="bg-slate-900/90 border border-slate-700 px-4 py-2 rounded-lg shadow-xl flex items-center gap-2">
          <Coins className="w-5 h-5 text-emerald-400 animate-pulse" />
          <span className="text-xl font-extrabold tracking-wide text-emerald-300">${cash.toLocaleString()}</span>
        </div>

        {/* Level and XP bar */}
        <div className="bg-slate-900/90 border border-slate-700 px-3 py-1.5 rounded-lg shadow-lg w-48 text-xs">
          <div className="flex justify-between font-bold mb-1">
            <span className="text-amber-400 flex items-center gap-1">
              <Award className="w-3.5 h-3.5" /> NV. {level}
            </span>
            <span className="text-slate-400">{xp} / {(level * 1000)} XP</span>
          </div>
          <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
            <div 
              className="h-full bg-amber-400 transition-all duration-300"
              style={{ width: `${Math.min(100, (xp / (level * 1000)) * 100)}%` }}
            ></div>
          </div>
        </div>

        {/* Weapon Quick HUD Wheel */}
        <div className="bg-slate-900/95 border border-slate-700/80 p-2.5 rounded-lg shadow-lg flex items-center gap-1.5">
          <span className="text-[10px] text-slate-400 mr-2 font-mono">1-5 KEYS</span>
          {([WeaponType.FISTS, WeaponType.PISTOL, WeaponType.SMG, WeaponType.SHOTGUN, WeaponType.RPG] as WeaponType[]).map((wType) => {
            const isSelected = activeWeapon === wType;
            const isUnlocked = unlockedWeapons.includes(wType) || wType === WeaponType.FISTS;
            return (
              <button
                key={wType}
                onClick={() => {
                  if (isUnlocked) {
                    playerRef.current.activeWeapon = wType;
                    setActiveWeapon(wType);
                  } else {
                    showAlert('Arma Bloqueada! Compre-a em seu Smartphone.');
                  }
                }}
                className={`relative w-10 h-10 rounded border flex flex-col items-center justify-center transition-all ${
                  isSelected 
                    ? 'bg-blue-600/35 border-blue-400 shadow-lg text-blue-100 scale-105' 
                    : isUnlocked 
                      ? 'bg-slate-800/80 border-slate-600/70 text-slate-300 hover:bg-slate-700' 
                      : 'bg-slate-900/40 border-slate-800 text-slate-600/50 cursor-not-allowed'
                }`}
              >
                <span className="text-base">
                  {wType === WeaponType.FISTS ? '👊' : wType === WeaponType.PISTOL ? '🔫' : wType === WeaponType.SMG ? '🖲️' : wType === WeaponType.SHOTGUN ? '💥' : '🚀'}
                </span>
                {wType !== WeaponType.FISTS && isUnlocked && (
                  <span className="absolute bottom-0.5 right-0.5 text-[7px] font-mono font-bold text-slate-400">
                    {weaponsAmmo[wType]}
                  </span>
                )}
                {!isUnlocked && <span className="absolute text-[8px] text-red-500 font-bold">🔒</span>}
              </button>
            );
          })}
        </div>

        {/* Wanted Level Stars (Glowing stars) */}
        {wantedLevel > 0 && (
          <div className="flex gap-1 bg-red-950/80 border border-red-800/60 px-3 py-1 rounded-md shadow-lg animate-bounce">
            {Array.from({ length: 5 }).map((_, idx) => (
              <span 
                key={idx} 
                className={`text-lg font-bold ${idx < wantedLevel ? 'text-yellow-400 drop-shadow-[0_0_5px_#facc15]' : 'text-slate-700'}`}
              >
                ★
              </span>
            ))}
          </div>
        )}
      </div>

      {/* --- HUD LEFT CORNER PANEL - MINI-MAP & HEALTH CORE --- */}
      <div className="absolute top-4 left-4 z-20 flex flex-col gap-2.5">
        {/* The radar mini-map container */}
        <div className="relative border-4 border-slate-900 bg-slate-950 rounded-xl overflow-hidden shadow-2xl w-36 h-36 md:w-44 md:h-44">
          <canvas 
            ref={miniMapCanvasRef} 
            width={176} 
            height={176} 
            className="w-full h-full"
          />
          <div className="absolute bottom-1 right-1 bg-black/75 text-[8px] text-slate-300 border border-slate-700 px-1 py-0.5 rounded font-mono uppercase font-bold text-xxs">
            L.S. Radar
          </div>
        </div>

        {/* Status bars (HP & AP) */}
        <div className="bg-slate-900/95 border border-slate-700/80 p-2.5 rounded-xl shadow-xl w-36 md:w-44 flex flex-col gap-1.5 text-xs font-bold">
          {/* Health Gauge */}
          <div className="flex items-center gap-2">
            <Heart className="w-3.5 h-3.5 text-red-500 fill-red-500" />
            <div className="flex-1 h-3 bg-slate-800 roundedoverflow-hidden relative">
              <div 
                className="h-full bg-red-600 transition-all duration-150"
                style={{ width: `${Math.min(100, health)}%` }}
              ></div>
              <span className="absolute inset-0 text-[8px] flex items-center justify-center text-slate-50">{health}% HP</span>
            </div>
          </div>
          {/* Armor Gauge */}
          <div className="flex items-center gap-2">
            <Shield className="w-3.5 h-3.5 text-blue-500 fill-blue-500" />
            <div className="flex-1 h-3 bg-slate-800 rounded overflow-hidden relative">
              <div 
                className="h-full bg-blue-600 transition-all duration-150"
                style={{ width: `${Math.min(100, armor)}%` }}
              ></div>
              <span className="absolute inset-0 text-[8px] flex items-center justify-center text-slate-50">{armor}% Colete</span>
            </div>
          </div>
        </div>
      </div>

      {/* --- HUD BOTTOM CORNER CONTROL BAR --- */}
      <div className="absolute bottom-4 left-4 z-20 flex items-center gap-2.5">
        {/* Toggle big city map floating overlay */}
        <button 
          onClick={() => setBigMapOpen(prev => !prev)}
          className="bg-slate-900/90 border border-slate-700 text-slate-300 hover:text-white px-3 py-2 rounded-lg text-xs font-bold shadow-xl flex items-center gap-1.5 transition-colors"
        >
          <Compass className="w-4 h-4 text-sky-400" /> Mapa Completo (M)
        </button>

        {/* Audio Mute controller trigger */}
        <button 
          onClick={() => setSoundEnabled(!soundEnabled)}
          className={`px-3 py-2 rounded-lg text-xs font-bold shadow-xl border flex items-center gap-1.5 transition-colors ${
            soundEnabled 
              ? 'bg-slate-900/90 border-slate-700 text-slate-300 hover:text-white' 
              : 'bg-red-950/85 border-red-800 text-red-300'
          }`}
        >
          {soundEnabled ? (
            <>
              <Volume2 className="w-4 h-4 text-emerald-400 animate-bounce" /> Som Ativado
            </>
          ) : (
            <>
              <VolumeX className="w-4 h-4 text-red-500" /> Som Mutado
            </>
          )}
        </button>

        {/* Free vehicle jack help tag */}
        <button 
          onClick={toggleCarEntrance}
          className="bg-slate-900/90 border border-slate-700 text-slate-300 hover:text-white px-3 py-2 rounded-lg text-xs font-bold shadow-xl flex items-center gap-1.5 transition-colors"
        >
          <Car className="w-4 h-4 text-amber-400" /> {inCar ? 'Sair do Carro (F)' : 'Pegar Carro (F)'}
        </button>
      </div>

      {/* --- FLOATING ALERTS AND NOTIFICATIONS --- */}
      {hudAlert && (
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 z-30 bg-slate-900/95 border-2 border-yellow-500/80 px-5 py-3 rounded-xl shadow-2xl w-80 text-center animate-bounce">
          <div className="text-yellow-400 font-extrabold text-sm mb-1">NOTIFICAÇÃO TELEFONE</div>
          <p className="text-xs text-slate-100 font-semibold leading-relaxed whitespace-pre-line">{hudAlert}</p>
        </div>
      )}

      {/* Active on-going mission status tracking ticker card */}
      {activeMission && (
        <div className="absolute top-4 left-1/2 -translate-x-1/2 z-20 bg-slate-900/95 border-l-4 border-l-yellow-500 border-slate-700 p-3 rounded-lg shadow-2xl w-72 text-xs">
          <div className="flex justify-between items-center font-bold mb-1">
            <span className="text-yellow-400 uppercase tracking-widest text-[9px] flex items-center gap-1">
              <Target className="w-3 h-3 text-red-500" /> TRABALHO ATIVO
            </span>
            {activeMission.timeRemaining !== undefined && (
              <span className={`font-mono text-xs px-1.5 py-0.5 rounded ${activeMission.timeRemaining < 10 ? 'bg-red-950 text-red-400 animate-pulse' : 'bg-slate-800 text-slate-300'}`}>
                ⏱️ {Math.round(activeMission.timeRemaining)}s
              </span>
            )}
          </div>
          <div className="font-extrabold text-slate-100 text-[11px] mb-1">{activeMission.title}</div>
          <p className="text-slate-400 text-[10px] leading-tight mb-2">{activeMission.details}</p>
          <div className="h-1 bg-slate-800 rounded-full overflow-hidden mb-1">
            <div className={`h-full ${activeMission.hasPickedUp ? 'bg-yellow-400' : 'bg-blue-500'} animate-pulse w-1/2`} />
          </div>
          {activeMission.type === 'delivery' && (
            <div className="text-[9px] font-bold text-slate-300 flex justify-between">
              <span>Etapa:</span>
              <span className={activeMission.hasPickedUp ? 'text-yellow-400' : 'text-blue-400'}>
                {activeMission.hasPickedUp ? 'Leve até o ponto verde/amarelo!' : 'Vá recolher a carga verde!'}
              </span>
            </div>
          )}
          {activeMission.type === 'item_pickup' && (
            <div className="text-[9px] font-bold text-slate-300 flex justify-between">
              <span>Encomendas:</span>
              <span className="text-blue-400">{activeMission.hasPickedUp ? 'Pronto! Entregue ao comprador.' : 'Recolha encomendas no mini-mapa!'}</span>
            </div>
          )}
          {activeMission.type === 'chase' && (
            <div className="text-[9px] font-bold text-slate-300 flex justify-between">
              <span>Alvo:</span>
              <span className="text-red-400 font-extrabold animate-pulse">Abata o veículo em fuga no mapa!</span>
            </div>
          )}
          {activeMission.type === 'survival' && (
            <div className="text-[9px] font-bold text-slate-200 flex justify-between bg-red-950/60 p-1 rounded border border-red-800/40">
              <span>Ficando Seguro:</span>
              <span className="text-red-400 animate-pulse">Permaneça vivo contra ondas!</span>
            </div>
          )}
        </div>
      )}

      {/* --- WASTED DEATH SCREENS FULL SCREEN OVERLAYS --- */}
      {screenOverlay !== 'none' && (
        <div className={`absolute inset-0 z-50 flex flex-col items-center justify-center backdrop-blur-sm transition-all duration-700 ${
          screenOverlay === 'wasted' 
            ? 'bg-rose-950/85' 
            : screenOverlay === 'busted' 
              ? 'bg-blue-950/85' 
              : 'bg-emerald-950/85'
        }`}>
          <div className="text-center transform scale-110 duration-500">
            {screenOverlay === 'wasted' && (
              <>
                <Skull className="w-16 h-16 text-rose-500 mx-auto mb-4 animate-bounce" />
                <h1 className="text-7xl font-black tracking-widest text-[red] italic font-serif opacity-90 drop-shadow-[0_4px_10px_rgba(0,0,0,0.9)] animate-pulse">SE FUDEU</h1>
                <p className="text-slate-300 text-sm mt-3 uppercase tracking-wider font-bold">Você foi hospitalizado ao hospital central de Los Santos</p>
                <p className="text-xs text-rose-400 font-bold mt-1">Custo médico: -$2,000</p>
              </>
            )}
            {screenOverlay === 'busted' && (
              <>
                <Shield className="w-16 h-16 text-blue-500 mx-auto mb-4 animate-bounce" />
                <h1 className="text-6xl font-black tracking-widest text-[#1e40af] italic font-serif opacity-90 drop-shadow-[0_4px_10px_rgba(0,0,0,0.9)]">PRESO</h1>
                <p className="text-slate-300 text-sm mt-3 uppercase tracking-wider font-bold">A polícia local acabou com seus crimes</p>
                <p className="text-xs text-blue-400 font-bold mt-1">Sua fiança custou: -$3,500</p>
              </>
            )}
          </div>
        </div>
      )}

      {/* --- MAIN GAME CANVAS CORE FIELD --- */}
      <div className="flex-1 relative h-full flex items-center justify-center bg-slate-900 border-2 border-slate-900/50">
        <canvas
          ref={mainCanvasRef}
          width={800}
          height={600}
          onClick={handleWeaponDischarge}
          className="w-full h-full object-cover rounded shadow-2xl cursor-crosshair bg-slate-100"
          style={{ maxHeight: 'calc(100vh - 4rem)', minHeight: '300px' }}
        />

        {/* Mobile controls on-screen virtual joysticks help */}
        {isMobile && (
          <div className="absolute inset-0 z-20 pointer-events-none flex select-none">
            {/* Joystick stick on bottom-left */}
            <div 
              className="absolute bottom-12 left-12 w-32 h-32 bg-slate-950/30 border border-slate-700 rounded-full flex items-center justify-center pointer-events-auto shadow-2xl touch-none"
              onTouchStart={handleJoystickStart}
              onTouchMove={handleJoystickMove}
              onTouchEnd={handleJoystickEnd}
            >
              <div 
                className="w-12 h-12 bg-blue-500 rounded-full shadow-lg border border-slate-400 transition-transform duration-75 pointer-events-none"
                style={{
                  transform: joystickRef.current.active 
                    ? `translate(${Math.max(-40, Math.min(40, joystickRef.current.curX - joystickRef.current.startX))}px, ${Math.max(-40, Math.min(40, joystickRef.current.curY - joystickRef.current.startY))}px)`
                    : 'translate(0px, 0px)'
                }}
              />
            </div>

            {/* Quick Virtual Shoot/Ação buttons on bottom-right */}
            <div className="absolute bottom-12 right-24 flex flex-col gap-2 pointer-events-auto">
              <button 
                onTouchStart={(e) => {
                  e.preventDefault();
                  // Fake virtual click in center
                  const canvas = mainCanvasRef.current;
                  if (canvas) {
                    const rect = canvas.getBoundingClientRect();
                    const centerX = rect.left + rect.width / 2;
                    const centerY = rect.top + rect.height / 2;
                    
                    const simulatedEvent = {
                      clientX: centerX + 50 * Math.cos(playerRef.current.angle),
                      clientY: centerY + 50 * Math.sin(playerRef.current.angle),
                    } as any;
                    
                    handleWeaponDischarge(simulatedEvent);
                  }
                }}
                className="w-16 h-16 bg-red-600/90 hover:bg-red-500 border border-slate-300 rounded-full flex items-center justify-center shadow-2xl text-slate-50 font-black text-xs cursor-pointer select-none active:scale-95"
              >
                ATIRAR 🔫
              </button>
            </div>
          </div>
        )}
      </div>

      {/* --- GTA V SMARTPHONE DYNAMIC MENU (CRAWLS BOTTOM RIGHT) --- */}
      <div className={`relative ${phoneOpen ? 'w-full md:w-80' : 'w-full md:w-16 md:flex md:items-end'} bg-slate-900 border-t md:border-t-0 md:border-l border-slate-800 transition-all duration-300 flex flex-col z-40 p-4 shrink-0`}>
        {!phoneOpen ? (
          <button 
            onClick={() => setPhoneOpen(true)}
            className="w-full h-11 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-slate-100 font-extrabold rounded-lg flex items-center justify-center gap-2 shadow-xl animate-bounce"
          >
            <Phone className="w-4 h-4 text-emerald-400" /> Celular L.S. (P) ^
          </button>
        ) : (
          <div className="flex-1 flex flex-col h-full bg-[#0b0f19] border-2 border-slate-700 rounded-2xl overflow-hidden shadow-2xl text-xs relative">
            {/* Phone header speaker line */}
            <div className="h-6 bg-slate-950 flex items-center justify-between px-3 text-slate-500 font-mono text-[9px] border-b border-slate-800 select-none">
              <span>LOS SANTOS NET</span>
              <div className="w-12 h-3 bg-slate-900 rounded-full border border-slate-700 text-slate-600 text-center text-[7px] leading-3">SPEAKER</div>
              <span>📶 🔋 100%</span>
            </div>

            {/* Simulated Phone content */}
            <div className="flex-1 overflow-y-auto p-3">
              {phoneApp === 'home' && (
                <div className="flex flex-col h-full justify-between">
                  <div>
                    <div className="text-center py-4 bg-slate-900/60 rounded-xl mb-4 border border-slate-800/50">
                      <h2 className="text-slate-400 text-[10px] uppercase font-bold tracking-widest mb-1">HORÁRIO DIGITAL</h2>
                      <p className="text-2xl font-black text-indigo-400 tracking-wider">08:22 AM</p>
                      <p className="text-[9px] text-slate-500 uppercase mt-0.5">Sintonizado com Los Santos</p>
                    </div>

                    <h3 className="font-extrabold text-[#94a3b8] uppercase text-[10px] tracking-wider mb-2.5">Aplicativos</h3>
                    
                    {/* App Grid */}
                    <div className="grid grid-cols-2 gap-2.5">
                      <button 
                        onClick={() => setPhoneApp('missions')}
                        className="p-3 bg-gradient-to-br from-amber-600/20 to-amber-950/10 border border-amber-800/40 rounded-xl flex flex-col items-center justify-center text-center gap-1.5 hover:bg-slate-800/80"
                      >
                        <Phone className="w-5 h-5 text-amber-400" />
                        <span className="font-bold text-[10px] text-amber-200">Lester Missões</span>
                      </button>

                      <button 
                        onClick={() => setPhoneApp('dealer')}
                        className="p-3 bg-gradient-to-br from-blue-600/20 to-blue-950/10 border border-blue-800/40 rounded-xl flex flex-col items-center justify-center text-center gap-1.5 hover:bg-slate-800/80"
                      >
                        <Car className="w-5 h-5 text-blue-400" />
                        <span className="font-bold text-[10px] text-blue-200">Simeon Autos</span>
                      </button>

                      <button 
                        onClick={() => setPhoneApp('weapons')}
                        className="p-3 bg-gradient-to-br from-rose-600/20 to-rose-950/10 border border-rose-800/40 rounded-xl flex flex-col items-center justify-center text-center gap-1.5 hover:bg-slate-800/80"
                      >
                        <Swords className="w-5 h-5 text-rose-400" />
                        <span className="font-bold text-[10px] text-rose-200">Ammu-Nation</span>
                      </button>

                      <button 
                        onClick={() => setPhoneApp('safehouse')}
                        className="p-3 bg-gradient-to-br from-cyan-600/20 to-cyan-950/10 border border-cyan-800/40 rounded-xl flex flex-col items-center justify-center text-center gap-1.5 hover:bg-slate-800/80"
                      >
                        <Home className="w-5 h-5 text-cyan-400" />
                        <span className="font-bold text-[10px] text-cyan-200">Imobiliária</span>
                      </button>
                    </div>
                  </div>

                  <button 
                    onClick={() => setPhoneApp('cheats')}
                    className="w-full mt-4 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700/80 text-slate-300 font-bold rounded-lg flex items-center justify-center gap-1.5"
                  >
                    <KeyRound className="w-3.5 h-3.5 text-yellow-500" /> Painel de Cheats/Senha
                  </button>
                </div>
              )}

              {/* LESTER PROCEDURAL MISSIONS APP */}
              {phoneApp === 'missions' && (
                <div className="flex flex-col gap-3">
                  <div className="font-extrabold text-sm text-yellow-400 flex items-center gap-1 border-b border-slate-800 pb-1 w-full justify-between">
                    <span>📱 Lester Telefonia</span>
                    <button onClick={() => setPhoneApp('home')} className="text-slate-500 hover:text-white">✕</button>
                  </div>

                  {activeMission ? (
                    <div className="bg-slate-900 border border-slate-800 p-3 rounded-lg text-slate-300">
                      <div className="text-[10px] text-amber-500 font-bold mb-1 uppercase tracking-wider">Trabalho em Progresso:</div>
                      <h4 className="font-extrabold text-slate-100 text-xs">{activeMission.title}</h4>
                      <p className="text-[9px] text-slate-400 mt-1 leading-normal">{activeMission.description}</p>
                      <button 
                        onClick={handleDeclineMission}
                        className="w-full mt-3 py-1.5 bg-red-950 hover:bg-red-900 text-red-300 font-bold rounded-md"
                      >
                        Cancelar Missão Ativa
                      </button>
                    </div>
                  ) : (
                    <div className="flex flex-col gap-2.5">
                      <div className="text-[10px] text-slate-400 leading-normal">
                        O Lester tem novos contratos prontinhos! Eles são gerados proceduralmente baseados em sua localização e nível atual. Deixe o celular bolar uma tarefa do crime pra você.
                      </div>
                      <button 
                        onClick={startProceduralMission}
                        className="w-full py-2.5 bg-gradient-to-r from-emerald-600 to-green-600 hover:from-emerald-500 text-white font-extrabold rounded-lg flex items-center justify-center gap-1.5"
                      >
                        <Plus className="w-4 h-4" /> Bolar Nova Missão Rápida
                      </button>
                    </div>
                  )}

                  <button 
                    onClick={() => setPhoneApp('home')}
                    className="w-full text-center text-[10px] text-slate-500 font-bold uppercase mt-2 hover:text-slate-300"
                  >
                    Voltar ao Menu
                  </button>
                </div>
              )}

              {/* SIMEON AUTO APP */}
              {phoneApp === 'dealer' && (
                <div className="flex flex-col gap-2.5">
                  <div className="font-extrabold text-sm text-blue-400 flex items-center gap-1 border-b border-slate-800 pb-1 justify-between">
                    <span>🚗 Simeon Dealership</span>
                    <button onClick={() => setPhoneApp('home')} className="text-slate-500 hover:text-white">✕</button>
                  </div>

                  <p className="text-[10px] text-slate-400 mb-1 leading-tight">
                    Compre supercarros inspirados na vida real. Eles serão spawnados na concessionária ao lado do Ammu-Nation de Los Santos!
                  </p>

                  <div className="flex flex-col gap-2 max-h-64 overflow-y-auto">
                    {CAR_CATALOG.map(c => {
                      const owned = ownedCars.includes(c.id);
                      return (
                        <div key={c.id} className="bg-slate-900 border border-slate-800 p-2 rounded-lg flex flex-col gap-1.5">
                          <div className="flex justify-between items-start">
                            <div>
                              <div className="font-extrabold text-slate-200">{c.brand} {c.realLifeCounterpart}</div>
                              <div className="text-[9px] text-slate-500">Gênero GTA: {c.name}</div>
                            </div>
                            <span className="font-mono text-emerald-400 font-bold">${c.price.toLocaleString()}</span>
                          </div>
                          
                          <div className="flex gap-2 text-[9px] text-slate-400 font-mono">
                            <span>Velo: {c.maxSpeed}</span>
                            <span>Acel: {c.acceleration}</span>
                            <span>Curva: {c.handling}</span>
                          </div>

                          <button 
                            onClick={() => buyCar(c.id, c.price)}
                            className="w-full py-1 bg-blue-900 hover:bg-blue-800 text-blue-100 font-bold rounded"
                          >
                            Spawnar / Comprar 💳
                          </button>
                        </div>
                      );
                    })}
                  </div>

                  <button 
                    onClick={() => setPhoneApp('home')}
                    className="w-full text-center text-[10px] text-slate-500 font-bold uppercase mt-1 hover:text-slate-300"
                  >
                    Voltar ao Menu
                  </button>
                </div>
              )}

              {/* AMMU NATION WEAPONS SHOP APP */}
              {phoneApp === 'weapons' && (
                <div className="flex flex-col gap-2.5">
                  <div className="font-extrabold text-sm text-rose-400 flex items-center gap-1 border-b border-slate-800 pb-1 justify-between">
                    <span>🔫 Ammu-Nation Digital</span>
                    <button onClick={() => setPhoneApp('home')} className="text-slate-500 hover:text-white">✕</button>
                  </div>

                  <div className="flex flex-col gap-2 max-h-64 overflow-y-auto">
                    {([WeaponType.PISTOL, WeaponType.SMG, WeaponType.SHOTGUN, WeaponType.RPG] as WeaponType[]).map(type => {
                      const wep = playerRef.current.weapons[type];
                      if (!wep) return null;
                      const unlocked = unlockedWeapons.includes(type);
                      return (
                        <div key={type} className="bg-slate-900 border border-slate-800 p-2 rounded-lg flex justify-between items-center">
                          <div>
                            <div className="font-extrabold text-slate-200">{wep.name}</div>
                            <div className="text-[9px] text-slate-500">
                              Dano: {wep.damage} • Cadência: {wep.fireRate}ms
                            </div>
                            <div className="text-[10px] text-slate-400 mt-1 font-mono">
                              Estado: {unlocked ? `Possui (${weaponsAmmo[type]} muns)` : 'Bloqueada'}
                            </div>
                          </div>

                          <button 
                            onClick={() => buyWeapon(type, wep.price)}
                            className={`px-2.5 py-1.5 rounded font-black text-[10px] text-slate-100 ${unlocked ? 'bg-indigo-900/60 hover:bg-indigo-800 text-indigo-200' : 'bg-rose-900 hover:bg-rose-800'}`}
                          >
                            {unlocked ? `MUNIÇÃO: $${Math.round(wep.price * 0.2)}` : `COMPRAR: $${wep.price}`}
                          </button>
                        </div>
                      );
                    })}
                  </div>

                  <button 
                    onClick={() => setPhoneApp('home')}
                    className="w-full text-center text-[10px] text-slate-500 font-bold uppercase mt-1 hover:text-slate-300"
                  >
                    Voltar ao Menu
                  </button>
                </div>
              )}

              {/* IMOBILIÁRIA SAFEHOUSES APP */}
              {phoneApp === 'safehouse' && (
                <div className="flex flex-col gap-2.5">
                  <div className="font-extrabold text-sm text-cyan-400 flex items-center gap-1 border-b border-slate-800 pb-1 justify-between">
                    <span>🏢 Imobiliária L.S.</span>
                    <button onClick={() => setPhoneApp('home')} className="text-slate-500 hover:text-white">✕</button>
                  </div>

                  <p className="text-[10px] text-slate-400 mb-1 leading-tight">
                    Adquira safehouses exclusivas na cidade para garantir novos pontos de spawn e reabastecer seus níveis de vida/colete de graça!
                  </p>

                  <div className="flex flex-col gap-2 max-h-64 overflow-y-auto">
                    {HOUSE_CATALOG.map(h => {
                      const owned = ownedHouses.includes(h.id);
                      return (
                        <div key={h.id} className="bg-slate-900 border border-slate-800 p-2 rounded-lg flex flex-col gap-1.5">
                          <div className="flex justify-between items-start">
                            <span className="font-extrabold text-slate-200">{h.name}</span>
                            <span className="font-mono text-cyan-400 font-bold">${h.price.toLocaleString()}</span>
                          </div>
                          <p className="text-[9px] text-slate-400 leading-tight">{h.description}</p>
                          <button 
                            disabled={owned}
                            onClick={() => buyRealEstate(h.id, h.price)}
                            className={`w-full py-1 font-bold rounded ${owned ? 'bg-emerald-950 text-emerald-400 cursor-not-allowed' : 'bg-cyan-900 hover:bg-cyan-800 text-cyan-100'}`}
                          >
                            {owned ? '✓ PROPRIEDADE SUA' : 'COMPRAR CASA'}
                          </button>
                        </div>
                      );
                    })}
                  </div>

                  <button 
                    onClick={() => setPhoneApp('home')}
                    className="w-full text-center text-[10px] text-slate-500 font-bold uppercase mt-1 hover:text-slate-300"
                  >
                    Voltar ao Menu
                  </button>
                </div>
              )}

              {/* CHEAT CODES COGNITION LAB */}
              {phoneApp === 'cheats' && (
                <div className="flex flex-col gap-3">
                  <div className="font-extrabold text-sm text-yellow-400 flex items-center gap-1 border-b border-slate-800 pb-1 justify-between">
                    <span>🗝️ Gaveta de Traquitas</span>
                    <button onClick={() => setPhoneApp('home')} className="text-slate-500 hover:text-white">✕</button>
                  </div>

                  <div className="text-[10px] text-slate-400 leading-normal">
                    Digite senhas especiais para testar a mecânica do jogo instantaneamente!
                    <ul className="list-disc pl-4 mt-1 flex flex-col gap-0.5 text-slate-300">
                      <li><b>gta5</b> - Ganhe +$90,000, desbloqueia todo arsenal</li>
                      <li><b>viva</b> - Cura HP, repara veículos, limpa wanted</li>
                      <li><b>supermaquina</b> - Spawna Aventador esportivo grátis</li>
                      <li><b>policia</b> - Dá 3 estrelas de procurado de imediato</li>
                    </ul>
                  </div>

                  <div className="flex flex-col gap-1.5 mt-1">
                    <input 
                      type="text"
                      placeholder="Senha do Golpe..."
                      value={cheatCode}
                      onChange={(e) => setCheatCode(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-700 p-2 rounded text-slate-100 font-mono focus:outline-none focus:border-yellow-500 text-xs text-center"
                    />
                    <button 
                      onClick={submitCheatCode}
                      className="w-full py-1.5 bg-yellow-600 hover:bg-yellow-500 text-slate-950 font-black rounded uppercase text-[10px]"
                    >
                      Processar Código 🔒
                    </button>
                  </div>

                  {cheatMessage && (
                    <div className="p-2 border border-blue-900/40 bg-blue-950/20 text-blue-300 rounded text-[10px] text-center leading-tight">
                      {cheatMessage}
                    </div>
                  )}

                  <button 
                    onClick={() => setPhoneApp('home')}
                    className="w-full text-center text-[10px] text-slate-500 font-bold uppercase mt-1 hover:text-slate-300"
                  >
                    Voltar ao Menu
                  </button>
                </div>
              )}
            </div>

            {/* Smart virtual back button */}
            <div className="h-9 bg-slate-950 border-t border-slate-800 flex items-center justify-center">
              <button 
                onClick={() => setPhoneOpen(false)}
                className="w-20 h-1.5 bg-slate-700 rounded-full hover:bg-slate-500 active:scale-95"
              />
            </div>
          </div>
        )}
      </div>

      {/* --- FLOATING DETAILED MAP DIALOG POPUP --- */}
      {bigMapOpen && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85">
          <div className="bg-slate-900 border-2 border-slate-700 rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl flex flex-col h-5/6">
            <div className="bg-slate-950 px-4 py-3 border-b border-slate-800 flex justify-between items-center select-none">
              <div className="flex items-center gap-2">
                <Compass className="w-5 h-5 text-sky-400 animate-spin" />
                <h2 className="font-extrabold text-sm tracking-wide text-slate-200">MAPA TURÍSTICO COMPLETO - LOS SANTOS</h2>
              </div>
              <button 
                onClick={() => setBigMapOpen(false)}
                className="p-1 px-3 bg-red-950 text-red-400 hover:bg-red-900 border border-red-800 text-xs font-black rounded-lg"
              >
                ✕ VOLTAR AO JOGO
              </button>
            </div>

            <div className="flex-1 p-4 overflow-y-auto text-xs grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="md:col-span-2 relative bg-slate-950 border border-slate-800 rounded-xl overflow-hidden flex items-center justify-center min-h-[250px]">
                {/* Micro drawing representing scale of city sizes */}
                <div className="text-center p-4">
                  <MapPin className="w-8 h-8 text-rose-500 mx-auto mb-2 animate-bounce" />
                  <p className="font-extrabold text-slate-300 text-sm">Escala da Cidade: 2000 x 2000px</p>
                  <p className="text-[11px] text-slate-500 mt-1 max-w-sm">
                    Utilize o Mini-mapa radar no canto de sua tela para navegar as rotas. Siga os pontos Verdes (recolha de cargas) e Amarelos (entregar cargas) para as missões para faturar muito dinheiro!
                  </p>
                  <div className="mt-4 inline-flex flex-wrap gap-2 justify-center text-[10px]">
                    <span className="px-2 py-0.5 rounded bg-[#10b981] text-xs font-bold">Ammu-Nation: (850, 450)</span>
                    <span className="px-2 py-0.5 rounded bg-[#3b82f6] text-xs font-bold">Simeon Autos: (1250, 750)</span>
                  </div>
                </div>
              </div>

              {/* Legends list */}
              <div className="flex flex-col gap-3 justify-between bg-slate-950/60 p-4 border border-slate-850 rounded-xl">
                <div>
                  <h3 className="font-extrabold text-indigo-400 mb-2 uppercase tracking-widest text-[10px]">Legendas de Navegação</h3>
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2">
                      <span className="w-3.5 h-3.5 rounded bg-amber-400 block" />
                      <span><b>Recompensas / Entregas:</b> Finalizadores</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="w-3.5 h-3.5 rounded bg-[#10b981] block" />
                      <span><b>Ammu-Nation:</b> Compra de Armas</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="w-3.5 h-3.5 rounded bg-[#3b82f6] block" />
                      <span><b>Simeon Autos:</b> Carros da Vida Real</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="w-3.5 h-3.5 rounded bg-[#f59e0b] block" />
                      <span><b>Ícone Laranja:</b> Safehouses Compra</span>
                    </div>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-800 text-[10px] text-slate-500 italic">
                  *Aperte a tecla [M] para fechar ou abrir este mapa a qualquer momento.
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
