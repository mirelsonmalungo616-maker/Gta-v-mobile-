package com.example.game

import androidx.compose.ui.graphics.Color

enum class WeaponType(val displayName: String, val baseDamage: Float, val price: Int, val maxAmmo: Int) {
    FISTS("Punhos", 10f, 0, 0),
    PISTOL("Pistola (M1911)", 20f, 500, 150),
    SMG("Submetralhadora (MP5)", 15f, 2000, 300),
    SHOTGUN("Escopeta (Remington)", 45f, 5000, 60),
    RPG("Lança-Mísseis (RPG-7)", 150f, 25000, 5)
}

data class Weapon(
    val type: WeaponType,
    var unlocked: Boolean = false,
    var ammo: Int = 0
)

enum class VehicleType(val displayName: String) {
    CAR("Carro"),
    MOTORCYCLE("Moto"),
    AIRPLANE("Avião"),
    BOAT("Barco")
}

// Vehicle custom specifications
data class CarTuning(
    var primaryColor: String = "#FF0000",
    var secondaryColor: String = "#333333",
    var paintFinish: String = "Brilhante", // "Brilhante", "Fosco", "Metálico", "Cromado"
    var wheelType: String = "Esportivo",  // "Esportivo", "Off-Road", "Luxo", "Corrida"
    var engineLevel: Int = 1,        // 1 to 4
    var brakesLevel: Int = 1,        // 1 to 4
    var suspensionLevel: Int = 1     // 1 to 4
)

data class CarCatalogItem(
    val id: String,
    val name: String,
    val brand: String,
    val realLifeCounterpart: String,
    val price: Int,
    val baseMaxSpeed: Float,
    val baseAcceleration: Float,
    val baseHandling: Float,
    val defaultColor: String,
    val type: VehicleType = VehicleType.CAR
)

val CAR_CATALOG = listOf(
    // CARS
    CarCatalogItem("wrangler", "Wrangler Rubicon", "Jeep", "Jeep Wrangler", 35000, 4.0f, 0.10f, 0.04f, "#1d4ed8", VehicleType.CAR),
    CarCatalogItem("mustang", "Mustang Shelby GT500", "Ford", "Ford Mustang GT", 75000, 6.0f, 0.18f, 0.05f, "#dc2626", VehicleType.CAR),
    CarCatalogItem("supra", "Supra MK4 Drift", "Toyota", "Toyota Supra", 90000, 6.4f, 0.20f, 0.06f, "#ea580c", VehicleType.CAR),
    CarCatalogItem("model_s", "Model S Plaid", "Tesla", "Tesla Model S Plaid", 120000, 7.0f, 0.32f, 0.05f, "#ffffff", VehicleType.CAR),
    CarCatalogItem("911_turbo", "911 Turbo S", "Porsche", "Porsche 911 Turbo S", 210000, 7.8f, 0.28f, 0.08f, "#0284c7", VehicleType.CAR),
    CarCatalogItem("aventador", "Aventador SVJ", "Lamborghini", "Lamborghini Aventador", 450000, 8.5f, 0.25f, 0.065f, "#eab308", VehicleType.CAR),
    CarCatalogItem("laferrari", "LaFerrari Aperta", "Ferrari", "Ferrari LaFerrari", 1200000, 9.2f, 0.30f, 0.075f, "#e11d48", VehicleType.CAR),

    // MOTORCYCLES
    CarCatalogItem("akuma", "CBR 1000RR Fireblade", "Honda", "Honda CBR 1000RR", 15000, 6.5f, 0.22f, 0.08f, "#ea580c", VehicleType.MOTORCYCLE),
    CarCatalogItem("bati801", "848 Superbike EVO", "Ducati", "Ducati 848 Superbike", 22000, 7.2f, 0.24f, 0.09f, "#ffffff", VehicleType.MOTORCYCLE),
    CarCatalogItem("sanchez", "KX250 Dirt Edition", "Kawasaki", "Kawasaki KX250 Motocross", 8500, 4.8f, 0.15f, 0.07f, "#22c55e", VehicleType.MOTORCYCLE),

    // AIRPLANES
    CarCatalogItem("duster", "Crop Duster Biplane", "Cessna", "Crop Duster Biplane", 180000, 5.5f, 0.12f, 0.04f, "#eab308", VehicleType.AIRPLANE),
    CarCatalogItem("luxor", "Luxor Executive Jet", "Learjet", "Learjet 45 Executive", 800000, 9.8f, 0.35f, 0.04f, "#facc15", VehicleType.AIRPLANE),
    CarCatalogItem("stunt", "300LX Acrobático", "Extra", "Stunt Plane Extra 300", 250000, 8.0f, 0.25f, 0.10f, "#f97316", VehicleType.AIRPLANE),

    // BOATS
    CarCatalogItem("seashark", "RXT Jet-Ski", "Sea-Doo", "Sea-Doo RXT JetSki", 12000, 5.0f, 0.18f, 0.10f, "#06b6d4", VehicleType.BOAT),
    CarCatalogItem("marquis", "Oceanis Luxury Yacht", "Beneteau", "Yacht Beneteau Oceanis", 350000, 3.5f, 0.08f, 0.03f, "#ffffff", VehicleType.BOAT),
    CarCatalogItem("toro", "Aquarama Super Speedboat", "Riva", "Riva Aquarama Speedboat", 140000, 6.2f, 0.22f, 0.06f, "#78350f", VehicleType.BOAT)
)

data class GameCar(
    val id: String,
    val catalogId: String,
    val name: String,
    val brand: String,
    val realLifeCounterpart: String,
    var x: Float,
    var y: Float,
    var angle: Float,
    var speed: Float,
    var health: Float,
    var maxHealth: Float = 100f,
    var isPlayerOwned: Boolean = false,
    var isPolice: Boolean = false,
    var color: String = "#ffffff",
    var tuning: CarTuning = CarTuning(primaryColor = "#dc2626")
) {
    // Dynamic stats upgraded by LSC shop tuning
    fun getMaxSpeed(catalog: CarCatalogItem): Float = catalog.baseMaxSpeed * (1f + (tuning.engineLevel - 1) * 0.15f)
    fun getAcceleration(catalog: CarCatalogItem): Float = catalog.baseAcceleration * (1f + (tuning.engineLevel - 1) * 0.20f)
    fun getHandling(catalog: CarCatalogItem): Float = catalog.baseHandling * (1f + (tuning.suspensionLevel - 1) * 0.15f)
}

data class HouseCatalogItem(
    val id: String,
    val name: String,
    val price: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: String,
    val description: String
)

val HOUSE_CATALOG = listOf(
    HouseCatalogItem(
        "studio_downtown", 
        "Kitnet no Centro", 
        25000, 
        450f, 
        850f, 
        60f, 
        60f, 
        "#84cc16", 
        "Um flat simples e bem localizado no centro da cidade. Ótimo ponto de partida, seguro e barato."
    ),
    HouseCatalogItem(
        "suburban_villa", 
        "Casa com Garagem", 
        150000, 
        1250f, 
        350f, 
        80f, 
        70f, 
        "#f97316", 
        "Casa confortável com gramado na zona residencial e uma vaga de spawn de carro rápido."
    ),
    HouseCatalogItem(
        "sunset_beach_condo", 
        "Apartamento Vista do Mar", 
        450000, 
        250f, 
        250f, 
        80f, 
        80f, 
        "#06b6d4", 
        "Apartamento luxuoso no extremo oeste com vista exuberante da praia e spa privativo."
    ),
    HouseCatalogItem(
        "villa_rockford", 
        "Mansão Rockford Hills", 
        750000, 
        1550f, 
        1150f, 
        100f, 
        100f, 
        "#3b82f6", 
        "Mansão enorme com piscina clássica, colunas gregas, portão elétrico duplo e vaga rápida de helicóptero."
    ),
    HouseCatalogItem(
        "penthouse_maze", 
        "Cobertura Suprema Maze Bank", 
        1500000, 
        950f, 
        950f, 
        100f, 
        100f, 
        "#a855f7", 
        "O topo do mundo! Cobertura com heliponto exclusivo, jacuzzi, seguro reforçado e munição grátis."
    ),
    HouseCatalogItem(
        "mansion_hills", 
        "Super Mansão Vinewood", 
        2500000, 
        650f, 
        150f, 
        110f, 
        110f, 
        "#ec4899", 
        "O ápice da ostentação! Heliponto privativo com vista incrível, jacuzzi panorâmica e cofre subterrâneo."
    )
)

data class GamePlayer(
    var name: String = "Miraldino Malungo",
    var x: Float = 500f,
    var y: Float = 500f,
    var angle: Float = 0f,
    var speed: Float = 0f,
    var health: Float = 100f,
    var armor: Float = 100f,
    var cash: Int = 15000, // starting clean amount so player can feel progression
    var xp: Int = 0,
    var level: Int = 1,
    var playTime: Float = 0f, // in seconds
    var activeWeapon: WeaponType = WeaponType.FISTS,
    val weapons: MutableMap<WeaponType, Weapon> = mutableMapOf(
        WeaponType.FISTS to Weapon(WeaponType.FISTS, true, 0),
        WeaponType.PISTOL to Weapon(WeaponType.PISTOL, false, 0),
        WeaponType.SMG to Weapon(WeaponType.SMG, false, 0),
        WeaponType.SHOTGUN to Weapon(WeaponType.SHOTGUN, false, 0),
        WeaponType.RPG to Weapon(WeaponType.RPG, false, 0)
    ),
    var inCar: Boolean = false,
    var currentCarId: String? = null,
    val ownedCars: MutableList<String> = mutableListOf(), // catalog IDs
    val ownedHouses: MutableList<String> = mutableListOf(), // house IDs
    var wantedLevel: Int = 0, // 0 to 5 stars
    var wantedPoints: Float = 0f,
    var isDead: Boolean = false,
    var activeMission: Mission? = null,
    var activeOutfitId: String = "default",
    val ownedOutfits: MutableList<String> = mutableListOf("default")
)

data class ClothingOutfit(
    val id: String,
    val name: String,
    val price: Int,
    val jacketColor: String, // hex color
    val pantsColor: String,  // hex color
    val description: String
)

val CLOTHING_OUTFITS = listOf(
    ClothingOutfit("default", "Jaqueta Clássica de Luanda", 0, "#191E29", "#2563EB", "Estilo clássico do Miraldino: jaqueta preta de couro e calças jeans."),
    ClothingOutfit("sunset", "Visual Estilo Sambizanga", 650, "#EA580C", "#1E293B", "Estilo radiante com blusão laranja pôr do sol e calça preta urbana."),
    ClothingOutfit("casual", "Jeans Cassenda Esportivo", 1200, "#0284C7", "#D1D5DB", "Blusão azul celeste leve e calças de algodão cinza super confortáveis."),
    ClothingOutfit("mafia", "Terno de Luxo Maianga", 5000, "#111827", "#111827", "Terno completo preto estilo executivo de elite para causar respeito."),
    ClothingOutfit("afro", "Estampa Tradicional Samakaka", 12000, "#EAB308", "#B91C1C", "Visual festivo com cores tradicionais vivas de Angola, pura ostentação cultural!")
)

enum class MissionType(val displayName: String) {
    DELIVERY("Entrega Expresso"),
    CHASE("Perseguição de Procurado"),
    ITEM_PICKUP("Recuperação de Cargas"),
    SURVIVAL("Sobrevivência sob Emboscada"),
    BANK_HEIST("O Grande Assalto ao Banco")
}

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val type: MissionType,
    val rewardCash: Int,
    val rewardXp: Int,
    var status: String = "available", // "available", "active", "completed", "failed"
    val difficulty: String = "easy", // "easy", "medium", "hard"
    // Coordinates
    val startX: Float,
    val startY: Float,
    val checkpointX: Float? = null,
    val checkpointY: Float? = null,
    val targetX: Float,
    val targetY: Float,
    val targetId: String? = null, // for chase targets
    // State indicators
    var hasPickedUpCargo: Boolean = false,
    val timeLimit: Int = 60, // in seconds
    var timeRemaining: Float = 60f
)

data class CityBuilding(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: String, // "sky", "office", "residential", "store", "customs", "park"
    val color: String,
    val roofColor: String,
    val name: String? = null
)

data class Pedestrian(
    val id: String,
    var x: Float,
    var y: Float,
    var angle: Float,
    var speed: Float,
    var health: Float,
    var color: Color,
    var state: String = "walking", // "walking", "fleeing", "dead"
    var targetX: Float,
    var targetY: Float,
    var fleeTimer: Float = 0f
)

data class Bullet(
    val id: String,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val firedByPlayer: Boolean,
    var rangeRemaining: Float = 400f
)

data class Explosion(
    val id: String,
    val x: Float,
    val y: Float,
    val maxRadius: Float = 80f,
    var life: Float = 1.0f // decays 1.0 to 0.0
)

data class Particle(
    val id: String,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    var life: Float = 1.0f,
    val decay: Float = 0.05f
)
