package com.example.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.*

enum class PhoneApp {
    HOME,
    CONTACTS,
    CHAT,
    VEHICLE_SHOP,
    DYNASTY8,
    INFO
}

data class PhoneMessage(
    val sender: String,
    val text: String,
    val isFromPlayer: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class GameViewModel : ViewModel() {

    // Main Game State Variables
    var player by mutableStateOf(GamePlayer())
    val cars = mutableStateListOf<GameCar>()
    val pedestrians = mutableStateListOf<Pedestrian>()
    val bullets = mutableStateListOf<Bullet>()
    val explosions = mutableStateListOf<Explosion>()
    val particles = mutableStateListOf<Particle>()

    // Phone UI Integration States
    var isPhoneOpen by mutableStateOf(false)
    var currentPhoneApp by mutableStateOf(PhoneApp.HOME)
    var clickedVehicleToBuy by mutableStateOf<CarCatalogItem?>(null)
    var showDeliveryDestinationMenu by mutableStateOf(false)
    
    // Message database/chat simulation
    var activeContactId by mutableStateOf("lamar") // "lamar", "lester", "mecanico", "simeon"
    val chatHistory = mutableStateMapOf<String, MutableList<PhoneMessage>>()

    // SMS Pop-Up toasts
    var activeSmsSender by mutableStateOf<String?>(null)
    var activeSmsText by mutableStateOf<String?>(null)
    var smsDisplayTimer by mutableStateOf(0f)

    // Game loop control
    private var gameJob: Job? = null
    var isPaused by mutableStateOf(false)

    // Virtual Joysticks & Controls State
    var leftJoystickX by mutableStateOf(0f)
    var leftJoystickY by mutableStateOf(0f)
    var throttleInput by mutableStateOf(0f) // 1f forward, -1f reverse, 0f neutral

    // UI Interactive panels
    var showCarDealerMenu by mutableStateOf(false)
    var showLscTuningMenu by mutableStateOf(false)
    var showAmmuNationMenu by mutableStateOf(false)
    var showHousePurchaseMenu by mutableStateOf(false)
    var activeHouseToBuy by mutableStateOf<HouseCatalogItem?>(null)

    // Interaction Prompt Message
    var interactionPrompt by mutableStateOf<String?>(null)

    // Flashing UI states
    var copChaseOverlayActive by mutableStateOf(false)
    var screenShakeAmount by mutableStateOf(0f)
    var bankLootCountdown by mutableStateOf(15f)

    // Large GPS map and Custom Waypoint System
    var isLargeMapOpen by mutableStateOf(false)
    var customWaypointX by mutableStateOf<Float?>(null)
    var customWaypointY by mutableStateOf<Float?>(null)

    // Clothes Boutique virtual shop UI state
    var showClothesMenu by mutableStateOf(false)

    init {
        resetGame()
        startGameLoop()
    }

    fun resetGame() {
        // Clear lists
        cars.clear()
        pedestrians.clear()
        bullets.clear()
        explosions.clear()
        particles.clear()

        // Create player
        player = GamePlayer(
            x = 500f,
            y = 500f,
            cash = 25000, // starting funds to buy something instantly!
            xp = 0,
            level = 1
        )

        // Spawn some random world cars on the streets
        for (i in 1..8) {
            val pt = CityGenerator.getRandomRoadPoint()
            val cat = CAR_CATALOG.random()
            cars.add(
                GameCar(
                    id = "traffic_$i",
                    catalogId = cat.id,
                    name = cat.name,
                    brand = cat.brand,
                    realLifeCounterpart = cat.realLifeCounterpart,
                    x = pt.first,
                    y = pt.second,
                    angle = (Math.random().toFloat() * PI.toFloat() * 2f),
                    speed = 0f,
                    health = 100f,
                    color = listOf("#dc2626", "#2563eb", "#10b981", "#eab308", "#ffffff", "#4b5563").random(),
                    tuning = CarTuning(primaryColor = cat.defaultColor)
                )
            )
        }

        // Spawn civilians
        for (i in 1..15) {
            val pt = CityGenerator.getRandomRoadPoint()
            val (tx, ty) = CityGenerator.getRandomRoadPoint()
            pedestrians.add(
                Pedestrian(
                    id = "ped_$i",
                    x = pt.first,
                    y = pt.second,
                    angle = 0f,
                    speed = 1.2f,
                    health = 40f,
                    color = Color(
                        (100..255).random(),
                        (100..255).random(),
                        (100..255).random()
                    ),
                    targetX = tx,
                    targetY = ty
                )
            )
        }

        isPaused = false
        setupChatDemo()
    }

    fun setupChatDemo() {
        chatHistory["lamar"] = mutableStateListOf(
            PhoneMessage("Lamar", "E aí Miraldino Malungo, meu mambo! Tá tudo calmo na província de Luanda? Já passaste na Loja de Roupas em Luanda Outlet? Qualquer confusão com a polícia, me dá um toque!", false)
        )
        chatHistory["lester"] = mutableStateListOf(
            PhoneMessage("Lester", "Fala Miraldino! Estou monitorando as frequências da polícia de Luanda. Se os tiras estiverem na sua cola, me pague $500 por estrela e eu limpo seu nome hackeando o satélite gps.", false)
        )
        chatHistory["simeon"] = mutableStateListOf(
            PhoneMessage("Simeon", "Grande Miraldino Malungo! Os carros mais incríveis estão disponíveis para entrega em Luanda! Compre os melhores no seu telemóvel e nós entregaremos na sua garagem!", false)
        )
        chatHistory["mecanico"] = mutableStateListOf(
            PhoneMessage("Mecânico", "Grande chefe! Pode solicitar qualquer viatura da sua coleção para ser entregue aqui mesmo do seu lado em Luanda! Sempre ao seu serviço!", false)
        )
    }

    fun sendPlayerSms(contactId: String, text: String, replyText: String, cost: Int = 0, onSuccess: () -> Unit = {}) {
        if (player.cash < cost) {
            triggerSmsToast("Operadora", "Saldo insuficiente no telemóvel para realizar esta ação!")
            return
        }
        
        if (cost > 0) {
            player.cash -= cost
            GameSound.playCash()
        }

        val history = chatHistory.getOrPut(contactId) { mutableStateListOf() }
        history.add(PhoneMessage("Você", text, true))

        onSuccess()

        viewModelScope.launch {
            delay(1000)
            history.add(PhoneMessage(getContactName(contactId), replyText, false))
            triggerSmsToast(getContactName(contactId), replyText)
        }
    }

    fun getContactName(id: String): String {
        return when (id) {
            "lamar" -> "Lamar Davis"
            "lester" -> "Lester Crest"
            "simeon" -> "Simeon Yetarian"
            "mecanico" -> "Mecânico"
            else -> "Contato"
        }
    }

    fun triggerSmsToast(sender: String, text: String) {
        activeSmsSender = sender
        activeSmsText = text
        smsDisplayTimer = 5.0f
    }

    fun selectVehicleToBuyFromPhone(item: CarCatalogItem) {
        if (player.cash < item.price) {
            triggerSmsToast("Banco Maze", "Compra recusada! Saldo imobiliário de $${player.cash.toLocaleString()} é insuficiente para pagar $${item.price.toLocaleString()}.")
            return
        }
        clickedVehicleToBuy = item
        showDeliveryDestinationMenu = true
    }

    fun deliverPurchasedVehicle(houseId: String) {
        val item = clickedVehicleToBuy ?: return
        if (player.cash >= item.price) {
            player.cash -= item.price
            player.ownedCars.add(item.id)
            GameSound.playCash()

            val house = HOUSE_CATALOG.find { it.id == houseId }
            val spawnX: Float
            val spawnY: Float
            val locationName: String

            if (house != null) {
                spawnX = house.x + house.width / 2f
                spawnY = house.y + house.height / 2f
                locationName = house.name
            } else {
                spawnX = player.x + 60f
                spawnY = player.y + 60f
                locationName = "Sua Posição Atual"
            }

            // Spawn GameCar
            val carId = "player_spawn_${UUID.randomUUID()}"
            cars.add(
                GameCar(
                    id = carId,
                    catalogId = item.id,
                    name = item.name,
                    brand = item.brand,
                    realLifeCounterpart = item.realLifeCounterpart,
                    x = spawnX,
                    y = spawnY,
                    angle = 0f,
                    speed = 0f,
                    health = 100f,
                    isPlayerOwned = true,
                    color = item.defaultColor,
                    tuning = CarTuning(primaryColor = item.defaultColor)
                )
            )

            val msgText = "Olá patrão! Seu ${item.brand} ${item.name} foi entregue com sucesso em: $locationName. Use o telemóvel para chamar o mecânico se quiser trazê-lo até você!"
            val history = chatHistory.getOrPut("mecanico") { mutableStateListOf() }
            history.add(PhoneMessage("Mecânico", msgText, false))
            triggerSmsToast("Mecânico", "Veículo entregue em ${locationName}!")

            showDeliveryDestinationMenu = false
            clickedVehicleToBuy = null
            isPhoneOpen = false
        }
    }

    fun buyMansionFromPhone(house: HouseCatalogItem) {
        if (player.ownedHouses.contains(house.id)) {
            triggerSmsToast("Dynasty 8", "Você já possui a propriedade ${house.name}!")
            return
        }
        if (player.cash >= house.price) {
            player.cash -= house.price
            player.ownedHouses.add(house.id)
            GameSound.playCash()
            val history = chatHistory.getOrPut("simeon") { mutableStateListOf() }
            history.add(PhoneMessage("Simeon", "Parabéns amigo! Compra da propriedade ${house.name} aprovada! Que belo investimento!", false))
            triggerSmsToast("Dynasty 8", "Propriedade ${house.name} adquirida com sucesso!")
        } else {
            triggerSmsToast("Dynasty 8", "Saldo de $${player.cash.toLocaleString()} insuficiente para pagar $${house.price.toLocaleString()}!")
        }
    }

    private fun startGameLoop() {
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                if (!isPaused && !player.isDead) {
                    val now = System.currentTimeMillis()
                    val dt = (now - lastTime) / 1000f
                    updateGame(dt)
                    lastTime = now
                } else {
                    lastTime = System.currentTimeMillis()
                }
                delay(16) // ~60fps tick
            }
        }
    }

    private fun updateGame(dt: Float) {
        player.playTime += dt

        // Decrement Screen Shake
        if (screenShakeAmount > 0f) {
            screenShakeAmount = max(0f, screenShakeAmount - dt * 20f)
        }

        // Decrement SMS Toast Display
        if (smsDisplayTimer > 0f) {
            smsDisplayTimer = max(0f, smsDisplayTimer - dt)
            if (smsDisplayTimer == 0f) {
                activeSmsSender = null
                activeSmsText = null
            }
        }

        updatePlayerPhysics(dt)
        updateTrafficCarsAndPolice(dt)
        updatePedestrians(dt)
        updateBulletsAndVfx(dt)
        updateMissionProgress(dt)
        updateWantedEscaping(dt)
        evaluateInteractionContext()

        // Smart Waypoint Destination Clearing
        if (customWaypointX != null && customWaypointY != null) {
            val distW = sqrt((player.x - customWaypointX!!) * (player.x - customWaypointX!!) + (player.y - customWaypointY!!) * (player.y - customWaypointY!!))
            if (distW < 45f) {
                customWaypointX = null
                customWaypointY = null
                triggerSmsToast("Navegador GPS 📍", "Você chegou ao destino marcado!")
                GameSound.playCash()
            }
        }
    }

    private fun updatePlayerPhysics(dt: Float) {
        val radius = 18f

        if (!player.inCar) {
            // Player is ON FOOT
            // Calculate movement from Left Joystick
            val inputMag = sqrt(leftJoystickX * leftJoystickX + leftJoystickY * leftJoystickY)
            if (inputMag > 0.1f) {
                val moveAngle = atan2(leftJoystickY, leftJoystickX)
                player.angle = moveAngle

                // Speed factor
                player.speed = 3.5f * min(1f, inputMag)
                player.x += cos(moveAngle) * player.speed
                player.y += sin(moveAngle) * player.speed

                // Spawn footsteps dust
                if (Math.random() < 0.15) {
                    spawnSmoke(player.x, player.y)
                }
            } else {
                player.speed = 0f
            }

            // Check collision with buildings
            val coll = CityGenerator.checkCollision(player.x, player.y, radius)
            if (coll.hit) {
                player.x += coll.adjustX
                player.y += coll.adjustY
            }
        } else {
            // Player is IN A CAR
            val activeCar = cars.find { it.id == player.currentCarId }
            if (activeCar != null) {
                val cat = CAR_CATALOG.find { it.id == activeCar.catalogId } ?: CAR_CATALOG[0]

                // Maximum speed and accel factored with customization upgrades
                val maxS = activeCar.getMaxSpeed(cat)
                val accel = activeCar.getAcceleration(cat)
                val handling = activeCar.getHandling(cat)

                // 1. Handle Throttle Acceleration / Brake input
                if (throttleInput > 0.1f) {
                    // Accel
                    activeCar.speed = min(maxS, activeCar.speed + accel)
                } else if (throttleInput < -0.1f) {
                    // Reverse or Brake
                    activeCar.speed = max(-maxS * 0.4f, activeCar.speed - accel * 1.5f)
                    if (activeCar.speed > 0f && Math.random() < 0.2) {
                        spawnSkidMarkParticles(activeCar)
                    }
                } else {
                    // Friction damping
                    if (activeCar.speed > 0f) {
                        activeCar.speed = max(0f, activeCar.speed - 0.08f)
                    } else if (activeCar.speed < 0f) {
                        activeCar.speed = min(0f, activeCar.speed + 0.12f)
                    }
                }

                // 2. Handle Steering via Left Joystick
                if (abs(leftJoystickX) > 0.1f) {
                    // Turning is relative to speed
                    val turnMult = min(1f, abs(activeCar.speed) / 1.5f)
                    val turnDir = if (activeCar.speed >= 0) 1f else -1f
                    activeCar.angle += leftJoystickX * handling * turnMult * turnDir

                    // Drift slide sound and smoke if speed is high
                    if (abs(activeCar.speed) > maxS * 0.5f && abs(leftJoystickX) > 0.7f) {
                        spawnSkidMarkParticles(activeCar)
                        if (Math.random() < 0.1) {
                            GameSound.playCarHorn()
                        }
                    }
                }

                // 3. Translate Position
                activeCar.x += cos(activeCar.angle) * activeCar.speed
                activeCar.y += sin(activeCar.angle) * activeCar.speed

                // Lock Player coordinate to driving car center
                player.x = activeCar.x
                player.y = activeCar.y
                player.angle = activeCar.angle
                player.speed = activeCar.speed

                // 4. Car Collision with City Buildings
                val carRad = 22f
                val coll = CityGenerator.checkCollision(activeCar.x, activeCar.y, carRad)
                if (coll.hit) {
                    // Bounce
                    if (abs(activeCar.speed) > 2f) {
                        screenShakeAmount = min(12f, abs(activeCar.speed) * 1.5f)
                        activeCar.health = max(0f, activeCar.health - abs(activeCar.speed) * 3f)
                        spawnCrashParticles(activeCar.x, activeCar.y)
                        // Trigger police search point rise on crashes near them
                        increaseWantedPoints(2f)
                    }
                    activeCar.speed = -activeCar.speed * 0.35f
                    activeCar.x += coll.adjustX
                    activeCar.y += coll.adjustY
                    player.x = activeCar.x
                    player.y = activeCar.y

                    // Blew up?
                    if (activeCar.health <= 0f) {
                        triggerCarExplosion(activeCar)
                    }
                }
            }
        }
    }

    private fun updateTrafficCarsAndPolice(dt: Float) {
        val toRemove = mutableListOf<GameCar>()
        
        // Spawn active police interceptors if wanted
        copChaseOverlayActive = (player.wantedLevel > 0)
        if (player.wantedLevel > 0 && cars.count { it.isPolice } < player.wantedLevel + 1) {
            if (Math.random() < 0.015) {
                // Procedural cop spawns ahead of the player path
                val angleOffset = player.angle + (Math.random().toFloat() * 1.2f - 0.6f)
                val spawnDist = 450f
                val cx = player.x + cos(angleOffset) * spawnDist
                val cy = player.y + sin(angleOffset) * spawnDist

                // Ensure it's in a safe collision zone
                val coll = CityGenerator.checkCollision(cx, cy, 25f)
                if (!coll.hit && cx > 0 && cx < CityGenerator.CITY_SIZE && cy > 0 && cy < CityGenerator.CITY_SIZE) {
                    cars.add(
                        GameCar(
                            id = "police_${UUID.randomUUID()}",
                            catalogId = "mustang", // fast pursuit Mustang cruiser
                            name = "Polícia Interceptadora",
                            brand = "Vapid",
                            realLifeCounterpart = "Cop Mustang GT",
                            x = cx,
                            y = cy,
                            angle = (Math.random().toFloat() * PI.toFloat() * 2f),
                            speed = 0f,
                            health = 120f,
                            color = "#0f172a", // Police black
                            isPolice = true
                        )
                    )
                }
            }
        }

        cars.forEach { car ->
            if (car.id == player.currentCarId) return@forEach // Player vehicle handled differently

            if (car.isPolice) {
                // --- POLICE AI CHASE LOGIC ---
                val dx = player.x - car.x
                val dy = player.y - car.y
                val dist = sqrt(dx * dx + dy * dy)

                // Turn towards player
                val targetAngle = atan2(dy, dx)
                val angleDiff = normalizeAngle(targetAngle - car.angle)
                car.angle += angleDiff * 0.08f // turning speed

                // Accelerate towards player
                val chaseSpeed = 5.8f + (player.wantedLevel * 0.4f)
                car.speed = min(chaseSpeed, car.speed + 0.15f)
                car.x += cos(car.angle) * car.speed
                car.y += sin(car.angle) * car.speed

                // Police ram player
                if (dist < 40f) {
                    if (player.inCar) {
                        val activeCar = cars.find { it.id == player.currentCarId }
                        activeCar?.let { pCar ->
                            pCar.speed *= 0.7f
                            pCar.health = max(0f, pCar.health - (car.speed * 1.5f))
                            car.speed *= -0.4f
                            spawnCrashParticles(car.x, car.y)
                            screenShakeAmount = 8f
                            if (pCar.health <= 0f) {
                                triggerCarExplosion(pCar)
                            }
                        }
                    } else {
                        // Cruel cop running over person on foot!
                        player.health = max(0f, player.health - (car.speed * 6f))
                        car.speed *= -0.2f
                        spawnBloodParticles(player.x, player.y)
                        screenShakeAmount = 14f
                        checkDeathStatus()
                    }
                }

                // Shoot at player if close enough
                if (dist < 250f && Math.random() < 0.03) {
                    val bulletVx = cos(car.angle) * 12f
                    val bulletVy = sin(car.angle) * 12f
                    bullets.add(
                        Bullet(
                            id = "cop_bullet_${System.currentTimeMillis()}",
                            x = car.x + cos(car.angle) * 20f,
                            y = car.y + sin(car.angle) * 20f,
                            vx = bulletVx,
                            vy = bulletVy,
                            damage = 8f * player.wantedLevel, // scales by stars
                            firedByPlayer = false
                        )
                    )
                }

            } else {
                // --- STANDARD TRAFFIC AUTO-DRIVING ---
                // Slowly drive forward, turning randomly at crossroads
                car.speed = min(2.5f, car.speed + 0.05f)
                car.x += cos(car.angle) * car.speed
                car.y += sin(car.angle) * car.speed

                if (Math.random() < 0.005) {
                    car.angle += (Math.random().toFloat() * 1.5f - 0.75f)
                }

                // Avoid crashing on screen edges by rotating back
                if (car.x < 50f || car.x > CityGenerator.CITY_SIZE - 50f ||
                    car.y < 50f || car.y > CityGenerator.CITY_SIZE - 50f) {
                    car.angle += PI.toFloat()
                    car.speed *= 0.5f
                }
            }

            // Resolve traffic car collision with city buildings
            val coll = CityGenerator.checkCollision(car.x, car.y, 20f)
            if (coll.hit) {
                car.x += coll.adjustX
                car.y += coll.adjustY
                car.angle += (PI.toFloat() / 2f)
                car.speed = -car.speed * 0.2f
                if (car.health <= 0) {
                    toRemove.add(car)
                }
            }
        }

        cars.removeAll(toRemove)
    }

    private fun updatePedestrians(dt: Float) {
        val deadToRemove = mutableListOf<Pedestrian>()

        pedestrians.forEach { ped ->
            if (ped.state == "dead") {
                ped.fleeTimer += dt
                if (ped.fleeTimer > 8f) {
                    deadToRemove.add(ped)
                }
                return@forEach
            }

            val dx = ped.targetX - ped.x
            val dy = ped.targetY - ped.y
            val dist = sqrt(dx * dx + dy * dy)

            // Switch path once goal achieved
            if (dist < 20f) {
                val newPoint = CityGenerator.getRandomRoadPoint()
                ped.targetX = newPoint.first
                ped.targetY = newPoint.second
            }

            // Move
            val angle = atan2(dy, dx)
            ped.angle = angle

            val speedFactor = if (ped.state == "fleeing") 3.0f else 1.2f
            ped.x += cos(angle) * ped.speed * speedFactor
            ped.y += sin(angle) * ped.speed * speedFactor

            // If player is driving fast and collides, hit pedestrian!
            val playerDist = sqrt((player.x - ped.x) * (player.x - ped.x) + (player.y - ped.y) * (player.y - ped.y))
            if (playerDist < 25f) {
                if (player.inCar && abs(player.speed) > 1.5f) {
                    // Splat!
                    ped.health = 0f
                    ped.state = "dead"
                    ped.fleeTimer = 0f // used for floor disappear timer
                    spawnBloodParticles(ped.x, ped.y)
                    GameSound.playExplosion()
                    screenShakeAmount = 5f
                    
                    // Earn money and raise police wanted points for crime!
                    increaseWantedPoints(15f)
                    val cashEarned = (20..150).random()
                    player.cash += cashEarned
                    GameSound.playCash()
                } else if (!player.inCar && player.activeWeapon == WeaponType.FISTS && inputMagPressed() && playerDist < 22f) {
                    // Punched
                    ped.health -= 15f
                    ped.state = "fleeing"
                    spawnBloodParticles(ped.x, ped.y)
                    GameSound.playShoot() // soft punch thud
                    if (ped.health <= 0f) {
                        ped.state = "dead"
                        ped.fleeTimer = 0f
                    }
                }
            }

            // Check walls
            val coll = CityGenerator.checkCollision(ped.x, ped.y, 10f)
            if (coll.hit) {
                ped.x += coll.adjustX
                ped.y += coll.adjustY
                // Repath
                val (nx, ny) = CityGenerator.getRandomRoadPoint()
                ped.targetX = nx
                ped.targetY = ny
            }
        }

        pedestrians.removeAll(deadToRemove)
    }

    private fun updateBulletsAndVfx(dt: Float) {
        val bulletToRemove = mutableListOf<Bullet>()

        // 1. Move bullets
        bullets.forEach { b ->
            b.x += b.vx
            b.y += b.vy
            b.rangeRemaining -= sqrt(b.vx * b.vx + b.vy * b.vy)

            if (b.rangeRemaining <= 0) {
                bulletToRemove.add(b)
                return@forEach
            }

            // Hit wall checks
            val col = CityGenerator.checkCollision(b.x, b.y, 5f)
            if (col.hit) {
                bulletToRemove.add(b)
                // Spark particles
                spawnSparks(b.x, b.y)
                return@forEach
            }

            // Check projectile hit targets
            if (b.firedByPlayer) {
                // Hits pedestrians
                pedestrians.forEach { ped ->
                    if (ped.state != "dead") {
                        val d = sqrt((b.x - ped.x) * (b.x - ped.x) + (b.y - ped.y) * (b.y - ped.y))
                        if (d < 16f) {
                            ped.health -= b.damage
                            ped.state = "fleeing"
                            spawnBloodParticles(ped.x, ped.y)
                            bulletToRemove.add(b)

                            if (ped.health <= 0f) {
                                ped.state = "dead"
                                ped.fleeTimer = 0f
                                increaseWantedPoints(12f)
                                player.cash += (40..180).random()
                                GameSound.playCash()
                            }
                        }
                    }
                }

                // Hits other traffic cars
                cars.forEach { car ->
                    if (car.id != player.currentCarId) {
                        val d = sqrt((b.x - car.x) * (b.x - car.x) + (b.y - car.y) * (b.y - car.y))
                        if (d < 24f) {
                            car.health -= b.damage
                            bulletToRemove.add(b)
                            spawnSparks(b.x, b.y)
                            increaseWantedPoints(8f)

                            if (car.health <= 0) {
                                triggerCarExplosion(car)
                            }
                        }
                    }
                }
            } else {
                // Fired by COPS - hits only player
                if (!player.isDead) {
                    val d = sqrt((b.x - player.x) * (b.x - player.x) + (b.y - player.y) * (b.y - player.y))
                    if (d < 20f) {
                        bulletToRemove.add(b)
                        // Armor blocks first
                        if (player.armor > 0) {
                            player.armor = max(0f, player.armor - b.damage * 0.7f)
                            player.health = max(0f, player.health - b.damage * 0.3f)
                        } else {
                            player.health = max(0f, player.health - b.damage)
                        }
                        spawnBloodParticles(player.x, player.y)
                        checkDeathStatus()
                    }
                }
            }
        }
        bullets.removeAll(bulletToRemove)

        // 2. Animate and prune explosions
        val explosionToRemove = mutableListOf<Explosion>()
        explosions.forEach { exp ->
            exp.life -= dt * 2.2f // lifespan around 0.5s
            if (exp.life <= 0f) {
                explosionToRemove.add(exp)
            }
        }
        explosions.removeAll(explosionToRemove)

        // 3. Particles
        val particleToRemove = mutableListOf<Particle>()
        particles.forEach { p ->
            p.x += p.vx
            p.y += p.vy
            p.life -= p.decay
            if (p.life <= 0) {
                particleToRemove.add(p)
            }
        }
        particles.removeAll(particleToRemove)
    }

    private fun updateWantedEscaping(dt: Float) {
        if (player.wantedLevel > 0) {
            player.wantedPoints = max(0f, player.wantedPoints - dt * 2f)
            if (player.wantedPoints <= 0) {
                player.wantedLevel --
                player.wantedPoints = if (player.wantedLevel > 0) 50f else 0f
            }
        }
    }

    private fun increaseWantedPoints(points: Float) {
        if (player.wantedLevel == 5) return // max 5 stars
        player.wantedPoints += points
        if (player.wantedPoints >= 100f) {
            player.wantedLevel = min(5, player.wantedLevel + 1)
            player.wantedPoints = 30f
            GameSound.playExplosion() // alarm warning chime
        }
    }

    private fun triggerCarExplosion(car: GameCar) {
        explosions.add(Explosion(UUID.randomUUID().toString(), car.x, car.y, 100f))
        GameSound.playExplosion()

        // Splash damage to entities within 100px radius
        val maxDist = 110f
        // Damage player
        val dp = sqrt((player.x - car.x) * (player.x - car.x) + (player.y - car.y) * (player.y - car.y))
        if (dp < maxDist) {
            val damage = 90f * (1f - (dp / maxDist))
            player.health = max(0f, player.health - damage)
            checkDeathStatus()
        }

        // Damage pedestrians
        pedestrians.forEach { ped ->
            val d = sqrt((ped.x - car.x) * (ped.x - car.x) + (ped.y - car.y) * (ped.y - car.y))
            if (d < maxDist) {
                ped.health = 0f
                ped.state = "dead"
                ped.fleeTimer = 0f
                spawnBloodParticles(ped.x, ped.y)
            }
        }

        // Chain react: blow up nearby cars
        cars.forEach { c ->
            if (c.id != car.id) {
                val d = sqrt((c.x - car.x) * (c.x - car.x) + (c.y - car.y) * (c.y - car.y))
                if (d < maxDist) {
                    c.health = 0f // boom!
                }
            }
        }

        // Spawn beautiful fiery particle cloud
        for (i in 0..15) {
            val angle = Math.random().toFloat() * PI.toFloat() * 2f
            val sp = 1f + Math.random().toFloat() * 5f
            particles.add(
                Particle(
                    id = "p_exp_${UUID.randomUUID()}",
                    x = car.x,
                    y = car.y,
                    vx = cos(angle) * sp,
                    vy = sin(angle) * sp,
                    color = Color(249, 115, 22), // deep blazing orange
                    size = 5f + Math.random().toFloat() * 8f,
                    decay = 0.02f + Math.random().toFloat() * 0.03f
                )
            )
        }

        // If player was in this car, eject them!
        if (player.currentCarId == car.id) {
            player.inCar = false
            player.currentCarId = null
            player.speed = 0f
        }
        
        // Remove exploded car from traffic list
        cars.remove(car)
    }

    private fun checkDeathStatus() {
        if (player.health <= 0f) {
            player.isDead = true
            GameSound.playMissionFail()
            viewModelScope.launch {
                delay(3500)
                // Spawn player healed at Nearest buyable Studio or Default center hospital
                player.health = 100f
                player.armor = 100f
                player.cash = max(100, player.cash - 1000) // loose $1000 penalty fee for dying (WASTED!)
                player.wantedLevel = 0
                player.wantedPoints = 0f
                player.inCar = false
                player.currentCarId = null
                player.x = 500f
                player.y = 500f // default hospital
                player.isDead = false
            }
        }
    }

    // --- PROCEDURAL MISSION STATE CONTROL LOOP ---
    private fun updateMissionProgress(dt: Float) {
        val mission = player.activeMission ?: return

        if (mission.status == "active") {
            mission.timeRemaining -= dt
            if (mission.timeRemaining <= 0) {
                // Timeout failed!
                mission.status = "failed"
                player.activeMission = null
                GameSound.playMissionFail()
                return
            }

            when (mission.type) {
                MissionType.DELIVERY -> {
                    if (!mission.hasPickedUpCargo) {
                        // Standing at cargo pick up?
                        val dist = sqrt((player.x - mission.checkpointX!!) * (player.x - mission.checkpointX) + (player.y - mission.checkpointY!!) * (player.y - mission.checkpointY))
                        if (dist < 35f) {
                            mission.hasPickedUpCargo = true
                            GameSound.playCash()
                            // Alter description
                            interactionPrompt = "CARGA ADQUIRIDA! Siga para o destino."
                        }
                    } else {
                        // Standing at drop target?
                        val dist = sqrt((player.x - mission.targetX) * (player.x - mission.targetX) + (player.y - mission.targetY) * (player.y - mission.targetY))
                        if (dist < 40f) {
                            completeActiveMission()
                        }
                    }
                }
                MissionType.CHASE -> {
                    // Spawns/updates a fast target car
                    val targetCar = cars.find { it.id == mission.targetId }
                    if (targetCar == null) {
                        // Target car eliminated!
                        completeActiveMission()
                    } else {
                        // Make target car escape actively from player
                        val dx = player.x - targetCar.x
                        val dy = player.y - targetCar.y
                        val dist = sqrt(dx * dx + dy * dy)

                        // Run away from player
                        val runAngle = atan2(dy, dx) + PI.toFloat()
                        targetCar.angle = runAngle
                        targetCar.speed = 5.2f // high fleeing speed
                        targetCar.x += cos(runAngle) * targetCar.speed
                        targetCar.y += sin(runAngle) * targetCar.speed
                    }
                }
                MissionType.ITEM_PICKUP -> {
                    // Standing at item pickup checkpoint?
                    val dist = sqrt((player.x - mission.checkpointX!!) * (player.x - mission.checkpointX) + (player.y - mission.checkpointY!!) * (player.y - mission.checkpointY))
                    if (dist < 35f) {
                        mission.hasPickedUpCargo = true
                    }
                    if (mission.hasPickedUpCargo) {
                        // Drive to delivery drop spot
                        val distEnd = sqrt((player.x - mission.targetX) * (player.x - mission.targetX) + (player.y - mission.targetY) * (player.y - mission.targetY))
                        if (distEnd < 40f) {
                            completeActiveMission()
                        }
                    }
                }
                MissionType.SURVIVAL -> {
                    // Survived in survival zone until time countdown runs up!
                    val dist = sqrt((player.x - mission.targetX) * (player.x - mission.targetX) + (player.y - mission.targetY) * (player.y - mission.targetY))
                    if (dist < 150f) {
                        // Slowly countdown success, spawn shooters of gang faction!
                        if (Math.random() < 0.02) {
                            pedestrians.add(
                                Pedestrian(
                                    id = "gang_${System.currentTimeMillis()}",
                                    x = mission.targetX + (Math.random().toFloat() * 160f - 80f),
                                    y = mission.targetY + (Math.random().toFloat() * 160f - 80f),
                                    angle = 0f,
                                    speed = 1.3f,
                                    health = 100f,
                                    color = Color.Red,
                                    state = "fleeing",
                                    targetX = player.x,
                                    targetY = player.y
                                )
                            )
                        }
                    }
                    if (mission.timeRemaining <= 1.0f) {
                        completeActiveMission()
                    }
                }
                MissionType.BANK_HEIST -> {
                    if (!mission.hasPickedUpCargo) {
                        // Phase 1: Looting
                        val distBank = getProximityToBuilding(CityGenerator.MAZE_BANK)
                        if (distBank > 70f) {
                            interactionPrompt = "ALERTA: Volte para o Banco Maze para concluir o roubo do cofre!"
                        } else {
                            if (bankLootCountdown > 0f) {
                                bankLootCountdown -= dt
                                interactionPrompt = "ROUBANDO COFRE: Aguarde e se defenda! faltam ${(bankLootCountdown.toInt() + 1)}s"
                                if (bankLootCountdown <= 0f) {
                                    mission.hasPickedUpCargo = true
                                    GameSound.playCash()
                                    triggerSmsToast("Lamar", "Cofre limpo mano! A bolsa tá cheia de dólares! Corre pro ponto de fuga na minha casa antes que os meganhas te cerquem!")
                                }
                            }
                        }
                    } else {
                        // Phase 2: Escape to friend's house
                        val distFriend = sqrt((player.x - mission.targetX) * (player.x - mission.targetX) + (player.y - mission.targetY) * (player.y - mission.targetY))
                        if (distFriend < 45f) {
                            completeActiveMission()
                        }
                    }
                }
            }
        }
    }

    private fun completeActiveMission() {
        val mission = player.activeMission ?: return
        mission.status = "completed"

        player.cash += mission.rewardCash
        player.xp += mission.rewardXp
        
        if (mission.type == MissionType.BANK_HEIST) {
            triggerSmsToast("Lamar", "TRABALHO BRILHANTE MANO! Limpamos o Maze Bank! Guarde sua bolada de $${mission.rewardCash.toLocaleString()} em segurança!")
        }
        
        // Dynamic leveling up
        val xpNeededForNext = player.level * 1000
        if (player.xp >= xpNeededForNext) {
            player.level++
            player.xp -= xpNeededForNext
            player.health = 100f
            player.armor = 100f
            interactionPrompt = "NÍVEL CONCLUÍDO! Agora você está no Nível ${player.level}!"
        }

        GameSound.playMissionSuccess()
        player.activeMission = null
        player.wantedLevel = 0 // clear stars as criminal respect reward!
        player.wantedPoints = 0f
    }

    fun startProceduralMission() {
        if (player.activeMission == null) {
            val m = MissionGenerator.generateProceduralMission(player.x, player.y, player.level, player.playTime)
            m.status = "active"
            
            // For CHASE missions, spawn the target fugitive car
            if (m.type == MissionType.CHASE) {
                val pt = CityGenerator.getRandomRoadPoint()
                val fugitiveId = "fugitive_${System.currentTimeMillis()}"
                val targetCar = GameCar(
                    id = fugitiveId,
                    catalogId = "aventador",
                    name = "Fugitivo Aventador",
                    brand = "Pegassi",
                    realLifeCounterpart = "Lamborghini Aventador",
                    x = pt.first,
                    y = pt.second,
                    angle = 0f,
                    speed = 2f,
                    health = 150f,
                    color = "#d97706" // Gold target design
                )
                cars.add(targetCar)
                
                // Copy reference
                player.activeMission = m.copy(targetId = fugitiveId, targetX = targetCar.x, targetY = targetCar.y)
            } else {
                player.activeMission = m
            }

            GameSound.playCash()
        }
    }

    fun startBankHeistMission() {
        player.activeMission = null
        bankLootCountdown = 15f
        
        val heist = Mission(
            id = "heist_${System.currentTimeMillis()}",
            title = "O Grande Assalto ao Banco 💰",
            description = "Limpe o cofre do Banco Maze! Aguarde de pé por 15 segundos se defendendo da polícia. Depois fuja com o dinheiro para a Casa do Amigo!",
            type = MissionType.BANK_HEIST,
            rewardCash = 180000,
            rewardXp = 2500,
            status = "active",
            difficulty = "hard",
            startX = CityGenerator.MAZE_BANK.x + CityGenerator.MAZE_BANK.width / 2f,
            startY = CityGenerator.MAZE_BANK.y + CityGenerator.MAZE_BANK.height / 2f,
            checkpointX = CityGenerator.MAZE_BANK.x + CityGenerator.MAZE_BANK.width / 2f,
            checkpointY = CityGenerator.MAZE_BANK.y + CityGenerator.MAZE_BANK.height / 2f,
            targetX = CityGenerator.FRIENDS_HOUSE.x + CityGenerator.FRIENDS_HOUSE.width / 2f,
            targetY = CityGenerator.FRIENDS_HOUSE.y + CityGenerator.FRIENDS_HOUSE.height / 2f,
            hasPickedUpCargo = false,
            timeLimit = 150,
            timeRemaining = 150f
        )
        
        player.activeMission = heist
        player.wantedLevel = 5
        player.wantedPoints = 120f
        
        spawnAggressiveCopNearBank()
        
        triggerSmsToast("Lamar", "Mano, você assaltou o Maze Bank?! A polícia tá enviando a SWAT inteira! Foge correndo pra minha casa, eu te cubro!")
    }

    private fun spawnAggressiveCopNearBank() {
        for (i in 1..2) {
            val angleOffset = (Math.random().toFloat() * PI.toFloat() * 2f)
            val spawnDist = 180f
            val cx = player.x + cos(angleOffset) * spawnDist
            val cy = player.y + sin(angleOffset) * spawnDist
            cars.add(
                GameCar(
                    id = "police_heist_${UUID.randomUUID()}",
                    catalogId = "mustang",
                    name = "SWAT LSPD",
                    brand = "Declasse",
                    realLifeCounterpart = "Cop Mustang GT",
                    x = cx,
                    y = cy,
                    angle = angleOffset + PI.toFloat(),
                    speed = 2.5f,
                    health = 130f,
                    color = "#020617",
                    isPolice = true
                )
            )
        }
    }

    // --- INTERACT BUTTON LOGICS (F / ACTION KEY EQUIVALENT) ---
    fun onActionButtonPressed() {
        // 1. Enter/Exit vehicle
        if (player.inCar) {
            // Eject player from car
            val activeCar = cars.find { it.id == player.currentCarId }
            if (activeCar != null) {
                activeCar.speed = 0f
                player.inCar = false
                player.currentCarId = null
                // Spawn player safely to the side
                player.x += 35f
                GameSound.playCarHorn()
            }
        } else {
            // Is near Bank to start heist?
            val distBank = getProximityToBuilding(CityGenerator.MAZE_BANK)
            if (distBank < 65f) {
                if (player.activeMission?.type != MissionType.BANK_HEIST) {
                    startBankHeistMission()
                    return
                }
            }

            // Is near some car?
            val nearCar = cars.find { car ->
                val d = sqrt((player.x - car.x) * (player.x - car.x) + (player.y - car.y) * (player.y - car.y))
                d < 45f
            }
            if (nearCar != null) {
                // Steal car
                player.inCar = true
                player.currentCarId = nearCar.id
                player.x = nearCar.x
                player.y = nearCar.y
                player.angle = nearCar.angle
                
                // Driving triggers police if car stolen in front of them
                if (nearCar.isPolice) {
                    increaseWantedPoints(50f)
                } else if (Math.random() < 0.2) {
                    increaseWantedPoints(20f)
                }

                GameSound.playCarHorn()
            }
        }
    }

    // Evaluate proximity prompts
    private fun evaluateInteractionContext() {
        showCarDealerMenu = false
        showAmmuNationMenu = false
        showLscTuningMenu = false
        showHousePurchaseMenu = false
        showClothesMenu = false
        activeHouseToBuy = null
        interactionPrompt = null

        if (player.isDead) return

        // 1. Enter/leave vehicular status
        if (player.inCar) {
            interactionPrompt = "VEÍCULO: Sair do Carro"
        } else {
            val nearCar = cars.find { car ->
                val d = sqrt((player.x - car.x) * (player.x - car.x) + (player.y - car.y) * (player.y - car.y))
                d < 45f
            }
            if (nearCar != null) {
                interactionPrompt = "ROUBAR VEÍCULO: ${nearCar.brand} ${nearCar.name}"
            }
        }

        // 1.5 Bank Heist Proximity Check
        val distBank = getProximityToBuilding(CityGenerator.MAZE_BANK)
        if (distBank < 65f) {
            val mission = player.activeMission
            if (mission != null && mission.type == MissionType.BANK_HEIST) {
                if (!mission.hasPickedUpCargo) {
                    interactionPrompt = "LOTEANDO O COFRE: Aguarde e se defenda! faltam ${bankLootCountdown.toInt() + 1}s"
                } else {
                    interactionPrompt = "FUGA DO ASSALTO: Dinheiro roubado! Fuja da polícia e siga para a Casa do Amigo!"
                }
            } else {
                interactionPrompt = "BANCO MAZE: Pressione [AÇÃO • F] para INICIAR O ASSALTO AO BANCO!"
            }
            return
        }

        // 2. Ammu-Nation
        val distAmmu = getProximityToBuilding(CityGenerator.AMMU_NATION)
        if (distAmmu < 60f) {
            showAmmuNationMenu = true
            interactionPrompt = "LOJA AMMU-NATION: Comprar Armas & Coletes"
            return
        }

        // 3. Simenon Car shop Dealer
        val distSimeon = getProximityToBuilding(CityGenerator.CAR_DEALER)
        if (distSimeon < 70f) {
            showCarDealerMenu = true
            interactionPrompt = "CONCESSIONÁRIA SIMEON: Comprar Carros Reias"
            return
        }

        // 4. LSC Custom Tuning Shop
        val distLsc = getProximityToBuilding(CityGenerator.LS_CUSTOMS)
        if (distLsc < 80f) {
            if (player.inCar) {
                showLscTuningMenu = true
                interactionPrompt = "LOS SANTOS CUSTOMS: Upgrades de Pintura e Tuning"
            } else {
                interactionPrompt = "LSC CUSTOMS: Entre dirigindo seu veículo para tunagem!"
            }
            return
        }

        // 5. Check buyable houses
        HOUSE_CATALOG.forEach { house ->
            val centerX = house.x + house.width / 2f
            val centerY = house.y + house.height / 2f
            val d = sqrt((player.x - centerX) * (player.x - centerX) + (player.y - centerY) * (player.y - centerY))
            if (d < 65f) {
                if (player.ownedHouses.contains(house.id)) {
                    // Owned shelter benefits: Heal & FREE Weapon ammo refill
                    interactionPrompt = "Sua Propriedade: ${house.name}. Vida curada! Coletes e Munições recarregadas."
                    player.health = 100f
                    player.armor = 100f
                    player.weapons.values.forEach { w ->
                        if (w.unlocked) {
                            w.ammo = w.type.maxAmmo
                        }
                    }
                } else {
                    activeHouseToBuy = house
                    showHousePurchaseMenu = true
                    interactionPrompt = "COMPRAR PROPRIEDADE: ${house.name} por $${house.price.toLocaleString()}"
                }
                return@evaluateInteractionContext
            }
        }

        // 6. Clothes Boutique Check
        val distClothes = getProximityToBuilding(CityGenerator.CLOTHES_SHOP)
        if (distClothes < 65f) {
            showClothesMenu = true
            interactionPrompt = "BOUTIQUE LUANDA OUTLET 🇧🇦: Compre trajes exclusivos para Miraldino Malungo!"
            return
        }

        // 7. Miraldino's own default House Check 🇦🇴
        val distMyHome = getProximityToBuilding(CityGenerator.MIRALDINO_HOUSE)
        if (distMyHome < 65f) {
            interactionPrompt = "MINHA VIVENDA DE LUANDA: Lar de Miraldino Malungo! Vida & Armadura curadas!"
            player.health = 100f
            player.armor = 100f
            player.weapons.values.forEach { w ->
                if (w.unlocked) {
                    w.ammo = w.type.maxAmmo
                }
            }
            return
        }
    }

    // --- CLOTHES BOUTIQUE STORE LOGIC ---
    fun buyClothing(outfit: ClothingOutfit) {
        if (!player.ownedOutfits.contains(outfit.id)) {
            if (player.cash < outfit.price) {
                triggerSmsToast("Boutique Luanda", "Saldo insuficiente para o traje ${outfit.name}! Preço: $${outfit.price.toLocaleString()}")
                return
            }
            player.cash -= outfit.price
            player.ownedOutfits.add(outfit.id)
            GameSound.playCash()
        }
        
        player.activeOutfitId = outfit.id
        
        // Eject if in car
        if (player.inCar) {
            player.inCar = false
            player.currentCarId = null
        }
        
        // Teleport to Miraldino's house in Luanda
        val destX = CityGenerator.MIRALDINO_HOUSE.x + CityGenerator.MIRALDINO_HOUSE.width / 2f
        val destY = CityGenerator.MIRALDINO_HOUSE.y + CityGenerator.MIRALDINO_HOUSE.height / 2f
        player.x = destX
        player.y = destY
        
        triggerSmsToast("Boutique Luanda 👔", "${outfit.name} equipado! Miraldino Malungo retornou à sua Vivenda de Luanda para se vestir!")
        showClothesMenu = false
    }

    private fun getProximityToBuilding(b: CityBuilding): Float {
        // Building center point coords
        val cx = b.x + b.width / 2f
        val cy = b.y + b.height / 2f
        return sqrt((player.x - cx) * (player.x - cx) + (player.y - cy) * (player.y - cy))
    }

    private fun inputMagPressed(): Boolean {
        return leftJoystickX != 0f || leftJoystickY != 0f
    }

    // --- WEAPONS MANAGEMENT ---
    fun cycleWeapon() {
        val unlockedWeapons = WeaponType.values().filter { player.weapons[it]?.unlocked == true }
        if (unlockedWeapons.isEmpty()) return
        val currIdx = unlockedWeapons.indexOf(player.activeWeapon)
        val nextIdx = (currIdx + 1) % unlockedWeapons.size
        player.activeWeapon = unlockedWeapons[nextIdx]
        GameSound.playShoot() // soft toggle click
    }

    fun fireActiveWeapon() {
        if (player.isDead) return
        val ammoEngine = player.weapons[player.activeWeapon] ?: return
        
        // Bullet count checks
        if (player.activeWeapon != WeaponType.FISTS && ammoEngine.ammo <= 0) {
            // Click empty ammo sound
            GameSound.playCarHorn()
            return
        }

        if (player.activeWeapon != WeaponType.FISTS) {
            ammoEngine.ammo--
        }

        GameSound.playShoot()

        val shootAngle = player.angle
        val bx = player.x + cos(shootAngle) * 20f
        val by = player.y + sin(shootAngle) * 20f

        if (player.activeWeapon == WeaponType.RPG) {
            // RPG explodes at maximum range
            viewModelScope.launch {
                bullets.add(
                    Bullet(
                        id = "bullet_${System.currentTimeMillis()}",
                        x = bx,
                        y = by,
                        vx = cos(shootAngle) * 11f,
                        vy = sin(shootAngle) * 11f,
                        damage = 100f,
                        firedByPlayer = true,
                        rangeRemaining = 300f
                    )
                )
                delay(650) // wait rocket flight duration before explosion
                explosions.add(Explosion(UUID.randomUUID().toString(), player.x + cos(shootAngle) * 280f, player.y + sin(shootAngle) * 280f, 130f))
                GameSound.playExplosion()
                screenShakeAmount = 18f
                increaseWantedPoints(35f)
            }
        } else {
            // Regular firearm bullets
            bullets.add(
                Bullet(
                    id = "bullet_${System.currentTimeMillis()}",
                    x = bx,
                    y = by,
                    vx = cos(shootAngle) * 14f,
                    vy = sin(shootAngle) * 14f,
                    damage = player.activeWeapon.baseDamage,
                    firedByPlayer = true,
                    rangeRemaining = if (player.activeWeapon == WeaponType.SHOTGUN) 180f else 380f
                )
            )

            // Spawn muzzle smoke particles
            spawnSmoke(bx, by)
            increaseWantedPoints(5f)
        }
    }

    // --- COMMERCE ACTIONS ---
    fun purchaseWeapon(type: WeaponType) {
        if (player.cash >= type.price) {
            player.cash -= type.price
            val w = player.weapons[type]!!
            w.unlocked = true
            w.ammo = min(type.maxAmmo, w.ammo + (type.maxAmmo / 2).coerceAtLeast(10))
            player.activeWeapon = type
            GameSound.playCash()
        }
    }

    fun buySimeonCar(item: CarCatalogItem) {
        if (player.cash >= item.price) {
            player.cash -= item.price
            player.ownedCars.add(item.id)

            // Spawn buyable car directly near dealership
            val id = "player_car_${UUID.randomUUID()}"
            cars.add(
                GameCar(
                    id = id,
                    catalogId = item.id,
                    name = item.name,
                    brand = item.brand,
                    realLifeCounterpart = item.realLifeCounterpart,
                    x = CityGenerator.CAR_DEALER.x + 130f,
                    y = CityGenerator.CAR_DEALER.y + 35f,
                    angle = 0f,
                    speed = 0f,
                    health = 100f,
                    isPlayerOwned = true,
                    color = item.defaultColor,
                    tuning = CarTuning(primaryColor = item.defaultColor)
                )
            )

            // Instantly auto-board the purchased car
            player.inCar = true
            player.currentCarId = id
            player.x = CityGenerator.CAR_DEALER.x + 130f
            player.y = CityGenerator.CAR_DEALER.y + 35f

            GameSound.playCash()
            showCarDealerMenu = false
        }
    }

    fun purchaseHouseProperty(house: HouseCatalogItem) {
        if (player.cash >= house.price) {
            player.cash -= house.price
            player.ownedHouses.add(house.id)
            GameSound.playCash()
            showHousePurchaseMenu = false
        }
    }

    // --- TUNING LSC UPGRADES ACTIONS ---
    fun applyCarPaint(colorHex: String, finish: String) {
        val activeCar = cars.find { it.id == player.currentCarId } ?: return
        if (player.cash >= 1500) {
            player.cash -= 1500
            activeCar.tuning.primaryColor = colorHex
            activeCar.tuning.paintFinish = finish
            activeCar.color = colorHex // Sync style
            GameSound.playCash()
        }
    }

    fun applyCarWheelType(wheel: String) {
        val activeCar = cars.find { it.id == player.currentCarId } ?: return
        if (player.cash >= 3000) {
            player.cash -= 3000
            activeCar.tuning.wheelType = wheel
            GameSound.playCash()
        }
    }

    fun upgradeEngineLevel() {
        val activeCar = cars.find { it.id == player.currentCarId } ?: return
        if (activeCar.tuning.engineLevel < 4) {
            val cost = activeCar.tuning.engineLevel * 8000
            if (player.cash >= cost) {
                player.cash -= cost
                activeCar.tuning.engineLevel++
                GameSound.playCash()
            }
        }
    }

    fun upgradeBrakesLevel() {
        val activeCar = cars.find { it.id == player.currentCarId } ?: return
        if (activeCar.tuning.brakesLevel < 4) {
            val cost = activeCar.tuning.brakesLevel * 4500
            if (player.cash >= cost) {
                player.cash -= cost
                activeCar.tuning.brakesLevel++
                GameSound.playCash()
            }
        }
    }

    fun upgradeSuspensionLevel() {
        val activeCar = cars.find { it.id == player.currentCarId } ?: return
        if (activeCar.tuning.suspensionLevel < 4) {
            val cost = activeCar.tuning.suspensionLevel * 5000
            if (player.cash >= cost) {
                player.cash -= cost
                activeCar.tuning.suspensionLevel++
                GameSound.playCash()
            }
        }
    }

    // --- PARTICLE EMITTERS ---
    private fun spawnSmoke(x: Float, y: Float) {
        particles.add(
            Particle(
                id = "p_smoke_${UUID.randomUUID()}",
                x = x,
                y = y,
                vx = (Math.random().toFloat() * 1.2f - 0.6f),
                vy = (Math.random().toFloat() * 1.2f - 0.6f),
                color = Color(203, 213, 225, 120), // light grey smoke
                size = 3f + Math.random().toFloat() * 3f,
                decay = 0.04f
            )
        )
    }

    private fun spawnSkidMarkParticles(car: GameCar) {
        val sx = car.x - cos(car.angle) * 15f
        val sy = car.y - sin(car.angle) * 15f
        particles.add(
            Particle(
                id = "p_skid_${UUID.randomUUID()}",
                x = sx,
                y = sy,
                vx = -cos(car.angle) * 0.5f,
                vy = -sin(car.angle) * 0.5f,
                color = Color(30, 41, 59, 180), // dark slate tire smudge
                size = 4f,
                decay = 0.03f
            )
        )
    }

    private fun spawnCrashParticles(x: Float, y: Float) {
        for (i in 0..6) {
            val a = Math.random().toFloat() * PI.toFloat() * 2f
            val sp = Math.random().toFloat() * 3f + 1f
            particles.add(
                Particle(
                    id = "p_crash_${UUID.randomUUID()}",
                    x = x,
                    y = y,
                    vx = cos(a) * sp,
                    vy = sin(a) * sp,
                    color = Color(245, 158, 11), // sparks
                    size = 2f,
                    decay = 0.06f
                )
            )
        }
    }

    private fun spawnBloodParticles(x: Float, y: Float) {
        for (i in 0..8) {
            val a = Math.random().toFloat() * PI.toFloat() * 2f
            val sp = Math.random().toFloat() * 2f + 0.5f
            particles.add(
                Particle(
                    id = "p_blood_${UUID.randomUUID()}",
                    x = x,
                    y = y,
                    vx = cos(a) * sp,
                    vy = sin(a) * sp,
                    color = Color(185, 28, 28), // deep red blood
                    size = 1.5f + Math.random().toFloat() * 3f,
                    decay = 0.03f
                )
            )
        }
    }

    private fun spawnSparks(x: Float, y: Float) {
        for (i in 0..4) {
            val a = Math.random().toFloat() * PI.toFloat() * 2f
            val s = Math.random().toFloat() * 2.5f
            particles.add(
                Particle(
                    id = "spark_${UUID.randomUUID()}",
                    x = x,
                    y = y,
                    vx = cos(a) * s,
                    vy = sin(a) * s,
                    color = Color.Yellow,
                    size = 1.8f,
                    decay = 0.08f
                )
            )
        }
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a < -PI) a += (2 * PI).toFloat()
        while (a > PI) a -= (2 * PI).toFloat()
        return a
    }

    // Currency formatting helper
    fun Int.toLocaleString(): String {
        return "$ %,d".format(this).trim().replace(',', '.')
    }
}
