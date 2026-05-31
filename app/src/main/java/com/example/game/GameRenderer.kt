package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

inline fun DrawScope.safeRotate(degrees: Float, pivot: Offset, block: DrawScope.() -> Unit) {
    this.drawContext.transform.rotate(degrees, pivot)
    this.block()
    this.drawContext.transform.rotate(-degrees, pivot)
}

object GameRenderer {

    fun drawCityBase(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        cameraX: Float,
        cameraY: Float
    ) {
        val roadWidth = CityGenerator.ROAD_WIDTH
        val citySize = CityGenerator.CITY_SIZE

        // Slate asphalt/ground backing
        drawScope.drawRect(
            color = Color(0xFF1E293B), // Slate dark
            size = Size(screenWidth, screenHeight)
        )

        // Translate drawing to follow camera center
        drawScope.drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(-cameraX, -cameraY)

            // Green grass border boundaries
            val grassColor = Color(0xFF059669)
            drawScope.drawRect(grassColor, Offset(-1000f, -1000f), Size(1000f, citySize + 2200f)) // Left
            drawScope.drawRect(grassColor, Offset(citySize, -1000f), Size(1000f, citySize + 2200f)) // Right
            drawScope.drawRect(grassColor, Offset(-1000f, -1000f), Size(citySize + 2000f, 1000f)) // Top
            drawScope.drawRect(grassColor, Offset(-1000f, citySize), Size(citySize + 2000f, 1000f)) // Bottom

            // Sidewalk master ground block
            drawScope.drawRect(
                color = Color(0xFF334155), // Sidewalk grey
                topLeft = Offset(0f, 0f),
                size = Size(citySize, citySize)
            )

            // Asphalt main channels
            val roadAsphaltColor = Color(0xFF0F172A)
            CityGenerator.ROAD_AVENUES_H.forEach { ry ->
                drawScope.drawRect(
                    color = roadAsphaltColor,
                    topLeft = Offset(0f, ry - roadWidth / 2f),
                    size = Size(citySize, roadWidth)
                )
            }
            CityGenerator.ROAD_AVENUES_V.forEach { rx ->
                drawScope.drawRect(
                    color = roadAsphaltColor,
                    topLeft = Offset(rx - roadWidth / 2f, 0f),
                    size = Size(roadWidth, citySize)
                )
            }

            // Road Divider Yellow Dashed Lines
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
            CityGenerator.ROAD_AVENUES_H.forEach { ry ->
                drawScope.drawLine(
                    color = Color(0xFFEAB308), // Glowing Yellow separator
                    start = Offset(0f, ry),
                    end = Offset(citySize, ry),
                    strokeWidth = 3f,
                    pathEffect = pathEffect
                )
            }
            CityGenerator.ROAD_AVENUES_V.forEach { rx ->
                drawScope.drawLine(
                    color = Color(0xFFEAB308),
                    start = Offset(rx, 0f),
                    end = Offset(rx, citySize),
                    strokeWidth = 3f,
                    pathEffect = pathEffect
                )
            }

            canvas.restore()
        }
    }

    fun drawBuildingsAndHouses3D(
        drawScope: DrawScope,
        cameraX: Float,
        cameraY: Float,
        playerX: Float,
        playerY: Float
    ) {
        val projScale = 0.11f // 3D Extrusion factor

        drawScope.drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(-cameraX, -cameraY)

            // 1. Render buyable house markers
            HOUSE_CATALOG.forEach { house ->
                // Neon bounding rectangle
                val neonColor = Color(android.graphics.Color.parseColor(house.color))
                drawScope.drawRect(
                    color = neonColor,
                    topLeft = Offset(house.x, house.y),
                    size = Size(house.width, house.height),
                    style = Stroke(width = 4f)
                )
                // Fill yard green-yellow transp
                drawScope.drawRect(
                    color = neonColor.copy(alpha = 0.14f),
                    topLeft = Offset(house.x, house.y),
                    size = Size(house.width, house.height)
                )

                // Outer structure outline block
                val bX = house.x + 12f
                val bY = house.y + 12f
                val bW = house.width - 24f
                val bH = house.height - 24f
                drawScope.drawRect(
                    color = Color(0xFF1E1B4B), // indigo blue structure
                    topLeft = Offset(bX, bY),
                    size = Size(bW, bH)
                )

                // White window glowing
                drawScope.drawRect(
                    color = Color(0xFFFEF08A),
                    topLeft = Offset(bX + bW / 4f, bY + bH / 4f),
                    size = Size(5f, 5f)
                )
                drawScope.drawRect(
                    color = Color(0xFFFEF08A),
                    topLeft = Offset(bX + bW * 3f / 4f, bY + bH / 4f),
                    size = Size(5f, 5f)
                )
            }

            // 2. Extrude 3D Buildings based on players view center
            CityGenerator.BUILDINGS.forEach { b ->
                val bColor = Color(android.graphics.Color.parseColor(b.color))
                val bRoofColor = Color(android.graphics.Color.parseColor(b.roofColor))

                // Flat floor layout bounding footprint
                drawScope.drawRect(
                    color = bColor,
                    topLeft = Offset(b.x, b.y),
                    size = Size(b.width, b.height)
                )

                // Projection offset from center to top Roof
                val rX = b.x + (b.x + b.width / 2f - playerX) * projScale
                val rY = b.y + (b.y + b.height / 2f - playerY) * projScale

                // Wall connecting polygons (Left wall, Right wall, Top wall, Bottom wall)
                val wallPaint = Paint().apply {
                    color = bColor.copy(alpha = 0.95f)
                    style = PaintingStyle.Fill
                }

                // Path construction
                // Top wall
                val pTop = Path().apply {
                    moveTo(b.x, b.y)
                    lineTo(rX, rY)
                    lineTo(rX + b.width, rY)
                    lineTo(b.x + b.width, b.y)
                    close()
                }
                canvas.drawPath(pTop, wallPaint)

                // Right wall
                val pRight = Path().apply {
                    moveTo(b.x + b.width, b.y)
                    lineTo(rX + b.width, rY)
                    lineTo(rX + b.width, rY + b.height)
                    lineTo(b.x + b.width, b.y + b.height)
                    close()
                }
                canvas.drawPath(pRight, wallPaint)

                // Bottom Wall
                val pBottom = Path().apply {
                    moveTo(b.x, b.y + b.height)
                    lineTo(rX, rY + b.height)
                    lineTo(rX + b.width, rY + b.height)
                    lineTo(b.x + b.width, b.y + b.height)
                    close()
                }
                canvas.drawPath(pBottom, wallPaint)

                // Left Wall
                val pLeft = Path().apply {
                    moveTo(b.x, b.y)
                    lineTo(rX, rY)
                    lineTo(rX, rY + b.height)
                    lineTo(b.x, b.y + b.height)
                    close()
                }
                canvas.drawPath(pLeft, wallPaint)

                // Draw Roof top lid
                drawScope.drawRect(
                    color = bRoofColor,
                    topLeft = Offset(rX, rY),
                    size = Size(b.width, b.height)
                )

                // Highlight border
                drawScope.drawRect(
                    color = Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(rX, rY),
                    size = Size(b.width, b.height),
                    style = Stroke(width = 1.5f)
                )
            }

            canvas.restore()
        }
    }

    fun drawCarsAndPlayers(
        drawScope: DrawScope,
        cameraX: Float,
        cameraY: Float,
        player: GamePlayer,
        carsList: List<GameCar>,
        pedestriansList: List<Pedestrian>
    ) {
        drawScope.drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(-cameraX, -cameraY)

            // 1. Draw Pedestrians
            pedestriansList.forEach { ped ->
                if (ped.state == "dead") {
                    // Blood trace pool
                    drawScope.drawCircle(
                        color = Color(0xFF991B1B).copy(alpha = 0.65f),
                        radius = 16f,
                        center = Offset(ped.x, ped.y)
                    )
                    return@forEach
                }

                // Render tiny shoulder circles rotated
                drawScope.safeRotate(degrees = Math.toDegrees(ped.angle.toDouble()).toFloat(), pivot = Offset(ped.x, ped.y)) {
                    // Civilian body
                    drawScope.drawCircle(
                        color = ped.color,
                        radius = 9f,
                        center = Offset(ped.x, ped.y)
                    )
                    // Head circle
                    drawScope.drawCircle(
                        color = Color(0xFFFDE047), // flesh yellow
                        radius = 5.5f,
                        center = Offset(ped.x, ped.y)
                    )
                    // Little shoulders/arms forward
                    drawScope.drawRect(
                        color = ped.color,
                        topLeft = Offset(ped.x + 3f, ped.y - 12f),
                        size = Size(4f, 24f)
                    )
                }
            }

            // 2. Draw cars including headlights / brakelights
            carsList.forEach { car ->
                drawScope.safeRotate(degrees = Math.toDegrees(car.angle.toDouble()).toFloat(), pivot = Offset(car.x, car.y)) {
                    val scaleX = 24f
                    val scaleY = 15f
                    val primaryColor = Color(android.graphics.Color.parseColor(car.tuning.primaryColor))
                    val secondaryColor = Color(android.graphics.Color.parseColor(car.tuning.secondaryColor))

                    // Wheels
                    val wheelColor = Color(0xFF1E293B)
                    // Front wheels
                    drawScope.drawRect(wheelColor, Offset(car.x + 10f, car.y - scaleY - 2f), Size(8f, 3f))
                    drawScope.drawRect(wheelColor, Offset(car.x + 10f, car.y + scaleY - 1f), Size(8f, 3f))
                    // Back wheels
                    drawScope.drawRect(wheelColor, Offset(car.x - 14f, car.y - scaleY - 2f), Size(9f, 3f))
                    drawScope.drawRect(wheelColor, Offset(car.x - 14f, car.y + scaleY - 1f), Size(9f, 3f))

                    // Chassis Body
                    drawScope.drawRect(
                        color = primaryColor,
                        topLeft = Offset(car.x - 20f, car.y - scaleY),
                        size = Size(40f, scaleY * 2)
                    )

                    // Secondary decals/spoiler trim
                    drawScope.drawRect(
                        color = secondaryColor,
                        topLeft = Offset(car.x - 18f, car.y - scaleY * 0.7f),
                        size = Size(6f, scaleY * 1.4f)
                    )

                    // Windows glass tinted black
                    drawScope.drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(car.x - 4f, car.y - scaleY * 0.75f),
                        size = Size(14f, scaleY * 1.5f)
                    )
                    // Roof cover
                    drawScope.drawRect(
                        color = primaryColor,
                        topLeft = Offset(car.x - 12f, car.y - scaleY * 0.65f),
                        size = Size(10f, scaleY * 1.3f)
                    )

                    // Taillights (glow red when braking / reversing)
                    val isBraking = car.speed < 0 || (car.speed > 0.5f && Math.random() < 0.2)
                    val tailGlow = if (isBraking) Color(0xFFEF4444) else Color(0xFF991B1B)
                    drawScope.drawRect(tailGlow, Offset(car.x - 21f, car.y - scaleY + 2f), Size(2f, 4f))
                    drawScope.drawRect(tailGlow, Offset(car.x - 21f, car.y + scaleY - 6f), Size(2f, 4f))

                    // Headlights white-yellow
                    val lightGlow = Color(0xFFFEF08A)
                    drawScope.drawRect(lightGlow, Offset(car.x + 19f, car.y - scaleY + 2f), Size(2f, 4f))
                    drawScope.drawRect(lightGlow, Offset(car.x + 19f, car.y + scaleY - 6f), Size(2f, 4f))

                    // Headlight light cone gradient vector extending forward
                    val gradBrush = Brush.linearGradient(
                        colors = listOf(Color(254, 240, 138, 70), Color(254, 240, 138, 0)),
                        start = Offset(car.x + 20f, car.y),
                        end = Offset(car.x + 140f, car.y)
                    )
                    val lightConePath = Path().apply {
                        moveTo(car.x + 20f, car.y - 10f)
                        lineTo(car.x + 150f, car.y - 50f)
                        lineTo(car.x + 150f, car.y + 50f)
                        lineTo(car.x + 20f, car.y + 10f)
                        close()
                    }
                    drawScope.drawPath(lightConePath, gradBrush)

                    // Strobe police siren cop flashes
                    if (car.isPolice) {
                        val flashBlue = ((System.currentTimeMillis() / 150) % 2).toInt() == 0
                        val strobeColor = if (flashBlue) Color(0xFF3B82F6) else Color(0xFFEF4444)
                        drawScope.drawRect(strobeColor, Offset(car.x - 4f, car.y - 6f), Size(8f, 12f))
                        drawScope.drawCircle(strobeColor.copy(alpha = 0.35f), 45f, Offset(car.x, car.y))
                    }
                }
            }

            // 3. Draw Player on foot if NOT inside vehicular controls
            if (!player.inCar) {
                drawScope.safeRotate(degrees = Math.toDegrees(player.angle.toDouble()).toFloat(), pivot = Offset(player.x, player.y)) {
                    val outfit = CLOTHING_OUTFITS.find { it.id == player.activeOutfitId }
                    val jkColor = outfit?.let { Color(android.graphics.Color.parseColor(it.jacketColor)) } ?: Color(0xFF1E293B)
                    val ptColor = outfit?.let { Color(android.graphics.Color.parseColor(it.pantsColor)) } ?: Color(0xFF2563EB)

                    // Leather Jacket shoulders
                    drawScope.drawCircle(
                        color = jkColor,
                        radius = 11f,
                        center = Offset(player.x, player.y)
                    )
                    // Pants legs
                    drawScope.drawCircle(
                        color = ptColor,
                        radius = 8f,
                        center = Offset(player.x - 5f, player.y)
                    )
                    // Head
                    drawScope.drawCircle(
                        color = Color(0xFFFDE047),
                        radius = 6.5f,
                        center = Offset(player.x, player.y)
                    )
                    // Hair spike
                    val hairColor = Color(0xFF78350F)
                    drawScope.drawCircle(hairColor, 3f, Offset(player.x - 2f, player.y))

                    // Gun silhouette drawn in-hand depending on WeaponType
                    if (player.activeWeapon != WeaponType.FISTS) {
                        val gunColor = Color(0xFF4B5563)
                        drawScope.drawRect(
                            color = gunColor,
                            topLeft = Offset(player.x + 8f, player.y + 4f),
                            size = Size(10f, 3f)
                        )
                    }
                }
            }

            canvas.restore()
        }
    }

    fun drawVfxAndCheckpoints(
        drawScope: DrawScope,
        cameraX: Float,
        cameraY: Float,
        playerX: Float,
        playerY: Float,
        bullets: List<Bullet>,
        explosions: List<Explosion>,
        particles: List<Particle>,
        activeMission: Mission?,
        customWaypointX: Float? = null,
        customWaypointY: Float? = null
    ) {
        drawScope.drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(-cameraX, -cameraY)

            // Drawing direct GPS path route tracking lines with glowing yellow
            if (customWaypointX != null && customWaypointY != null) {
                drawScope.drawLine(
                    color = Color(0xFFFACC15).copy(alpha = 0.82f),
                    start = Offset(playerX, playerY),
                    end = Offset(customWaypointX, customWaypointY),
                    strokeWidth = 5.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                )
            }

            // 1. Projectiles / bullets glows
            bullets.forEach { b ->
                val bColor = if (b.firedByPlayer) Color(0xFFFCA5A5) else Color(0xFFFED7AA)
                drawScope.drawLine(
                    color = bColor,
                    start = Offset(b.x - b.vx * 0.5f, b.y - b.vy * 0.5f),
                    end = Offset(b.x, b.y),
                    strokeWidth = 3f
                )
                // Spark glowing point
                drawScope.drawCircle(Color.White, 3f, Offset(b.x, b.y))
            }

            // 2. Dust/Smoke/Sparks particles
            particles.forEach { p ->
                drawScope.drawCircle(
                    color = p.color.copy(alpha = p.life),
                    radius = p.size,
                    center = Offset(p.x, p.y)
                )
            }

            // 3. Dynamic fire dome explosions
            explosions.forEach { exp ->
                val scaleRad = exp.maxRadius * (1f - (exp.life - 1f) * (exp.life - 1f))
                val gradBrush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFF97316), Color(0xFFDC2626), Color.Transparent),
                    center = Offset(exp.x, exp.y),
                    radius = scaleRad
                )
                drawScope.drawCircle(
                    brush = gradBrush,
                    radius = scaleRad,
                    center = Offset(exp.x, exp.y)
                )
            }

            // 4. Draw active procedural checkpoints
            if (activeMission != null && activeMission.status == "active") {
                if (activeMission.type == MissionType.DELIVERY || activeMission.type == MissionType.ITEM_PICKUP) {
                    val showCargoArrival = activeMission.hasPickedUpCargo
                    val checkpointX = if (showCargoArrival) activeMission.targetX else (activeMission.checkpointX ?: activeMission.targetX)
                    val checkpointY = if (showCargoArrival) activeMission.targetY else (activeMission.checkpointY ?: activeMission.targetY)

                    // Flashing glowing checkpoint circle on the street asphalt
                    val pulse = ((System.currentTimeMillis() / 250) % 2).toInt() == 0
                    val archColor = if (showCargoArrival) Color(0xFFEAB308) else Color(0xFF22C55E) // Gold vs green
                    
                    // Outer ring
                    drawScope.drawCircle(
                        color = archColor.copy(alpha = if (pulse) 0.5f else 0.25f),
                        radius = 45f,
                        center = Offset(checkpointX, checkpointY),
                        style = Stroke(width = 8f)
                    )
                    // Inner disk translucent
                    drawScope.drawCircle(
                        color = archColor.copy(alpha = 0.12f),
                        radius = 45f,
                        center = Offset(checkpointX, checkpointY)
                    )
                }
            }

            // 4.5 Draw custom GPS waypoint glowing ring on ground if set
            if (customWaypointX != null && customWaypointY != null) {
                val wx = customWaypointX
                val wy = customWaypointY
                val pulse = ((System.currentTimeMillis() / 250) % 2).toInt() == 0
                val waypointColor = Color(0xFFFACC15) // Bright Yellow
                
                // Outer glowing ring
                drawScope.drawCircle(
                    color = waypointColor.copy(alpha = if (pulse) 0.6f else 0.3f),
                    radius = 35f,
                    center = Offset(wx, wy),
                    style = Stroke(width = 6f)
                )
                // Inner disk translucency
                drawScope.drawCircle(
                    color = waypointColor.copy(alpha = 0.15f),
                    radius = 35f,
                    center = Offset(wx, wy)
                )
            }

            canvas.restore()
        }
    }
}
