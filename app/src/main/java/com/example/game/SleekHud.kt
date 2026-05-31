package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SleekHud(
    vm: GameViewModel,
    modifier: Modifier = Modifier
) {
    val player = vm.player
    val activeCar = vm.cars.find { it.id == player.currentCarId }

    Box(modifier = modifier.fillMaxSize()) {

        // ==========================================
        //  TOP LEFT: MINIMAP & HEALTH/ARMOR
        // ==========================================
        Box(
            modifier = Modifier
                .padding(14.dp)
                .align(Alignment.TopStart)
                .width(150.dp)
                .background(Color(0xE60F172A), RoundedCornerShape(12.dp))
                .border(2.dp, Color(0x996366F1), RoundedCornerShape(12.dp))
                .padding(6.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Mini Radar Canvas (Clique para abrir o Mapa Ampliado)
                Canvas(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .clickable { vm.isLargeMapOpen = true }
                ) {
                    val mapRadius = 65f
                    val radarX = size.width / 2f
                    val radarY = size.height / 2f
                    val radarScale = 0.16f // zoomed in street radar view

                    // Center is player position
                    // Draw nearby roads
                    CityGenerator.ROAD_AVENUES_H.forEach { ry ->
                        val dy = (ry - player.y) * radarScale
                        val relativeY = radarY + dy
                        if (relativeY in 0f..size.height) {
                            drawRect(
                                color = Color(0x66475569),
                                topLeft = Offset(0f, relativeY - 6f),
                                size = Size(size.width, 12f)
                            )
                        }
                    }
                    CityGenerator.ROAD_AVENUES_V.forEach { rx ->
                        val dx = (rx - player.x) * radarScale
                        val relativeX = radarX + dx
                        if (relativeX in 0f..size.width) {
                            drawRect(
                                color = Color(0x66475569),
                                topLeft = Offset(relativeX - 6f, 0f),
                                size = Size(12f, size.height)
                            )
                        }
                    }

                    // Special destinations on Minimap
                    // Los Santos Customs icon
                    val lscDx = (CityGenerator.LS_CUSTOMS.x + 50f - player.x) * radarScale
                    val lscDy = (CityGenerator.LS_CUSTOMS.y + 40f - player.y) * radarScale
                    drawCircle(Color(0xFFA855F7), 6f, Offset(radarX + lscDx, radarY + lscDy))

                    // Simeon dealer icon
                    val dealerDx = (CityGenerator.CAR_DEALER.x + 45f - player.x) * radarScale
                    val dealerDy = (CityGenerator.CAR_DEALER.y + 35f - player.y) * radarScale
                    drawCircle(Color(0xFF3B82F6), 6f, Offset(radarX + dealerDx, radarY + dealerDy))

                    // Ammu-Nation icon
                    val ammuDx = (CityGenerator.AMMU_NATION.x + 40f - player.x) * radarScale
                    val ammuDy = (CityGenerator.AMMU_NATION.y + 30f - player.y) * radarScale
                    drawCircle(Color(0xFF10B981), 6f, Offset(radarX + ammuDx, radarY + ammuDy))

                    // Maze Bank icon (Bank heist)
                    val bankDx = (CityGenerator.MAZE_BANK.x + 50f - player.x) * radarScale
                    val bankDy = (CityGenerator.MAZE_BANK.y + 50f - player.y) * radarScale
                    drawCircle(Color(0xFFF97316), 6f, Offset(radarX + bankDx, radarY + bankDy))

                    // Casa do Amigo (Friend's house)
                    val friendDx = (CityGenerator.FRIENDS_HOUSE.x + 30f - player.x) * radarScale
                    val friendDy = (CityGenerator.FRIENDS_HOUSE.y + 30f - player.y) * radarScale
                    drawCircle(Color(0xFFEC4899), 6f, Offset(radarX + friendDx, radarY + friendDy))

                    // Buyable Houses on Radar
                    HOUSE_CATALOG.forEach { house ->
                        val x = house.x + house.width / 2f
                        val y = house.y + house.height / 2f
                        val hDx = (x - player.x) * radarScale
                        val hDy = (y - player.y) * radarScale
                        val owned = player.ownedHouses.contains(house.id)
                        drawCircle(if (owned) Color(0xFF22C55E) else Color(0xFFEAB308), 5f, Offset(radarX + hDx, radarY + hDy))
                    }

                    // Active Mission check markers on radar
                    player.activeMission?.let { m ->
                        if (m.status == "active") {
                            val cargoX = if (m.hasPickedUpCargo) m.targetX else (m.checkpointX ?: m.targetX)
                            val cargoY = if (m.hasPickedUpCargo) m.targetY else (m.checkpointY ?: m.targetY)
                            val cDx = (cargoX - player.x) * radarScale
                            val cDy = (cargoY - player.y) * radarScale
                            drawCircle(Color(0xFFEF4444), 6f, Offset(radarX + cDx, radarY + cDy))
                        }
                    }

                    // Custom GPS Waypoint line and marker on Radar
                    if (vm.customWaypointX != null && vm.customWaypointY != null) {
                        val wDx = (vm.customWaypointX!! - player.x) * radarScale
                        val wDy = (vm.customWaypointY!! - player.y) * radarScale
                        
                        // Yellow direct line from player center towards custom waypoint marker
                        drawLine(
                            color = Color(0xFFFACC15), // bright yellow route
                            start = Offset(radarX, radarY),
                            end = Offset(radarX + wDx, radarY + wDy),
                            strokeWidth = 3f
                        )
                        
                        // Small yellow circle destination marker
                        drawCircle(Color(0xFFFACC15), 5.5f, Offset(radarX + wDx, radarY + wDy))
                    }

                    // Draw static player indicator (facing up) in center
                    drawCircle(Color.White, 5.5f, Offset(radarX, radarY))
                    val directionX = radarX + cos(player.angle) * 11f
                    val directionY = radarY + sin(player.angle) * 11f
                    drawLine(Color.White, Offset(radarX, radarY), Offset(directionX, directionY), strokeWidth = 3f)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // HP Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VIDA ", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF450A0A), RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(player.health / 100f).background(Color(0xFFEF4444), RoundedCornerShape(4.dp)))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // COLETE Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("COLE ", color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF172554), RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(player.armor / 100f).background(Color(0xFF3B82F6), RoundedCornerShape(4.dp)))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x33FFFFFF)))
                Spacer(modifier = Modifier.height(6.dp))
                Text("LEGENDA", color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                LegendItem(color = Color(0xFFF97316), text = "Banco Maze")
                LegendItem(color = Color(0xFF3B82F6), text = "Comp. Carros")
                LegendItem(color = Color(0xFF10B981), text = "Armas (Ammu)")
                LegendItem(color = Color(0xFF22C55E), text = "Minha Casa")
                LegendItem(color = Color(0xFFEC4899), text = "Casa Amigo")
            }
        }

        // ==========================================
        //  TOP RIGHT: STATS HUD
        // ==========================================
        Box(
            modifier = Modifier
                .padding(14.dp)
                .align(Alignment.TopEnd)
                .width(180.dp)
                .background(Color(0xCC0F172A), RoundedCornerShape(12.dp))
                .border(2.dp, Color(0x9922C55E), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // Cash Counter
                Text(
                    text = "$ ${player.cash.toLocaleString()}",
                    color = Color(0xFF22C55E),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleLarge.copy(
                        shadow = Shadow(color = Color(0xFF15803D), blurRadius = 8f)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // XP / Level displays
                Text(
                    text = "LEVEL ${player.level}",
                    color = Color(0xFFEAB308),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                val xpForNext = player.level * 1000
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF713F12), CircleShape)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((player.xp.toFloat() / xpForNext).coerceIn(0f, 1f)).background(Color(0xFFEAB308), CircleShape))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Weapon Selection Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = player.activeWeapon.displayName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (player.activeWeapon != WeaponType.FISTS) {
                            val wData = player.weapons[player.activeWeapon]
                            Text(
                                text = "Munição: ${wData?.ammo}/${player.activeWeapon.maxAmmo}",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "LIVRE",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable { vm.cycleWeapon() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (player.activeWeapon) {
                                WeaponType.FISTS -> "👊"
                                WeaponType.PISTOL -> "🔫"
                                WeaponType.SMG -> "🔫"
                                WeaponType.SHOTGUN -> "🦖"
                                WeaponType.RPG -> "🚀"
                            },
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // ==========================================
        //  TOP CENTER: POLICE WANTED CHASE INDICATOR
        // ==========================================
        if (player.wantedLevel > 0) {
            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .align(Alignment.TopCenter)
                    .background(Color(0xDD991B1B), RoundedCornerShape(10.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PROCURADO 🚨 ",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    // Draw star glyph icons base
                    for (i in 1..5) {
                        Text(
                            text = "★",
                            color = if (i <= player.wantedLevel) Color(0xFFFDE047) else Color(0x40FFFFFF),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==========================================
        //  MID LEFT: MISSION OVERLAY WINDOW
        // ==========================================
        player.activeMission?.let { mission ->
            Box(
                modifier = Modifier
                    .padding(top = 180.dp, start = 14.dp)
                    .align(Alignment.TopStart)
                    .width(220.dp)
                    .background(Color(0xE6090D16), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFF1F5F9).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        "OBJETIVO ATIVO",
                        color = Color(0xFFEF4444),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        mission.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (mission.type == MissionType.DELIVERY && !mission.hasPickedUpCargo) {
                            "→ Vá buscar a carga no ponto verde do mapa"
                        } else if (mission.type == MissionType.DELIVERY) {
                            "→ Entregue a carga no ponto amarelo no mapa"
                        } else if (mission.type == MissionType.CHASE) {
                            "→ Destrua o veículo fugitivo dourado!"
                        } else if (mission.type == MissionType.BANK_HEIST && !mission.hasPickedUpCargo) {
                            "→ fique no Banco! Roubando cofre..."
                        } else if (mission.type == MissionType.BANK_HEIST) {
                            "→ CORRA! Leve a grana para a Casa do Amigo!"
                        } else {
                            "→ Veja as marcações no radar!"
                        },
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Timer bar
                    val percentage = (mission.timeRemaining / mission.timeLimit).coerceIn(0f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Tempo: ${mission.timeRemaining.toInt()}s ",
                            color = if (percentage < 0.25f) Color.Red else Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0x33FFFFFF), RoundedCornerShape(2.dp))) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(percentage).background(if (percentage < 0.25f) Color.Red else Color.Green, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
        }

        // ==========================================
        //  BOTTOM CENTER: INTERACTION PROMPT TEXT
        // ==========================================
        vm.interactionPrompt?.let { prompt ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp)
                    .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = prompt,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ==========================================
        //  BOTTOM RIGHT: VEHICLE HUD SPEEDOMETER
        // ==========================================
        if (player.inCar && activeCar != null) {
            Box(
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.BottomEnd)
                    .width(190.dp)
                    .background(Color(0xE6030712), RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFFA855F7), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        "${activeCar.brand.uppercase()} ${activeCar.name.uppercase()}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Origem real: ${activeCar.realLifeCounterpart}",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val speedKmh = (sqrt(activeCar.speed * activeCar.speed) * 22f).toInt()
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$speedKmh",
                            color = Color(0xFFA855F7),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " KM/H",
                            color = Color(0xFFC084FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    // Performance tuning stars display
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Motor: Lvl ${activeCar.tuning.engineLevel}", color = Color(0xFFE2E8F0), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Freio: Lvl ${activeCar.tuning.brakesLevel}", color = Color(0xFFE2E8F0), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Susp: Lvl ${activeCar.tuning.suspensionLevel}", color = Color(0xFFE2E8F0), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // ==========================================
        //  TOUCH CONTROL INTERFACES: VIRTUAL BUTTON PANEL
        // ==========================================
        // LEFT STICK DRAG DETECTOR
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
                .size(110.dp)
                .background(Color(0x33475569), CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            vm.leftJoystickX = 0f
                            vm.leftJoystickY = 0f
                        },
                        onDragCancel = {
                            vm.leftJoystickX = 0f
                            vm.leftJoystickY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Calculate joystick coordinates normalized internally
                            val maxRadius = 130f
                            val rawX = vm.leftJoystickX * maxRadius + dragAmount.x
                            val rawY = vm.leftJoystickY * maxRadius + dragAmount.y
                            val dist = sqrt(rawX * rawX + rawY * rawY)

                            if (dist > maxRadius) {
                                vm.leftJoystickX = rawX / dist
                                vm.leftJoystickY = rawY / dist
                            } else {
                                vm.leftJoystickX = rawX / maxRadius
                                vm.leftJoystickY = rawY / maxRadius
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Little drag thumb
            val xOffset = (vm.leftJoystickX * 30).dp
            val yOffset = (vm.leftJoystickY * 30).dp
            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .size(45.dp)
                    .background(Color.White.copy(alpha = 0.75f), CircleShape)
                    .border(2.dp, Color(0xFF475569), CircleShape)
            )
        }

        // RIGHT PEDALS & FIRING BUTTON ACTIONS
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Pedals (Only if driving car)
            if (player.inCar) {
                // Reverse/Brake Pedal
                Box(
                    modifier = Modifier
                        .size(50.dp, 80.dp)
                        .background(Color(0x99DC2626), RoundedCornerShape(8.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { vm.throttleInput = -1f },
                                onDragEnd = { vm.throttleInput = 0f },
                                onDrag = { _, _ -> }
                            )
                        }
                        .clickable { }, // dummy click
                    contentAlignment = Alignment.Center
                ) {
                    Text("FREIO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Forward throttle pedal
                Box(
                    modifier = Modifier
                        .size(50.dp, 100.dp)
                        .background(Color(0x9922C55E), RoundedCornerShape(8.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { vm.throttleInput = 1f },
                                onDragEnd = { vm.throttleInput = 0f },
                                onDrag = { _, _ -> }
                            )
                        }
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text("ACEL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.width(18.dp))
            }

            // ACTION PAD Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    // MISSION TRIGGER BUTTON (Proc Gen)
                    if (player.activeMission == null) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color(0xCC059669), CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                                .clickable { vm.startProceduralMission() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Missão", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // MAIN F-ACTION BUTTON (Enter/Exit Car, purchase validation etc.)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xE64F46E5), CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { vm.onActionButtonPressed() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (player.inCar) "SAIR 🚗" else "AÇÃO • F", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // WEAPON FIRE TRIGGER ACTION BUTTON
                val fireColor = if (player.activeWeapon == WeaponType.FISTS) Color(0xE14B5563) else Color(0xE1B91C1C)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(fireColor, CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { vm.fireActiveWeapon() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (player.activeWeapon == WeaponType.FISTS) "👊 BATER" else "🔥 ATIRAR",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // ==========================================
        //  OVERLAYS COVALENT MODAL Menus
        // ==========================================
        // 1. LUANDA CUSTOMS TUNING PANEL
        if (vm.showLscTuningMenu && player.inCar && activeCar != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB3000000))
                    .padding(horizontal = 40.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(420.dp)
                        .background(Color(0xFF0B0F19), RoundedCornerShape(16.dp))
                        .border(3.dp, Color(0xFFA855F7), RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    val coroutineScope = rememberCoroutineScope()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LUANDA CUSTOMS 🔧", color = Color(0xFFA855F7), fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Text("Oficina Especializada: Personalize e tune as suas viaturas reais!", color = Color.White, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Paints Selection row
                        Text("1. COR DA PINTURA REALISTA (Custo: $1.500)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
                            val paintColors = listOf(
                                Triple("#2563EB", "Metálico Azul", "Metálico"),
                                Triple("#DC2626", "Vermelho Brilhante", "Brilhante"),
                                Triple("#059669", "Verde Esmeralda", "Cromado"),
                                Triple("#EAB308", "Camuflagem Ouro", "Metálico"),
                                Triple("#1E293B", "Preto Fosco Stealth", "Fosco")
                            )
                            paintColors.forEach { (colorHex, name, fin) ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            Color(android.graphics.Color.parseColor(colorHex)),
                                            CircleShape
                                        )
                                        .border(2.dp, Color.White, CircleShape)
                                        .clickable { vm.applyCarPaint(colorHex, fin) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Wheels/rim selection row
                        Text("2. NOVAS JANTES E PNEUS DE ALTA PERFORMANCE (Custo: $3.000)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
                            val wheels = listOf("Esportivo", "Off-Road", "Luxo", "Corrida")
                            wheels.forEach { wName ->
                                Button(
                                    onClick = { vm.applyCarWheelType(wName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(wName, color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Performance tuners options
                        Text("3. PREPARAÇÃO DE MOTOR E SUSPENSÃO ESTRADA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(4.dp))

                        // Engine Upgrade
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Estágio do Motor (Lvl ${activeCar.tuning.engineLevel}/4)", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            val cost = activeCar.tuning.engineLevel * 8000
                            Button(
                                onClick = { vm.upgradeEngineLevel() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                enabled = activeCar.tuning.engineLevel < 4
                            ) {
                                Text(if (activeCar.tuning.engineLevel >= 4) "MAXED" else "Melhorar ($${cost.toLocaleString()})", fontSize = 10.sp)
                            }
                        }

                        // Brakes Upgrade
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Pastilhas de Freio Brembo (Lvl ${activeCar.tuning.brakesLevel}/4)", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            val cost = activeCar.tuning.brakesLevel * 4500
                            Button(
                                onClick = { vm.upgradeBrakesLevel() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                enabled = activeCar.tuning.brakesLevel < 4
                            ) {
                                Text(if (activeCar.tuning.brakesLevel >= 4) "MAXED" else "Melhorar ($${cost.toLocaleString()})", fontSize = 10.sp)
                            }
                        }

                        // Suspension Upgrade
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Suspensão Rebaixada Esportiva (Lvl ${activeCar.tuning.suspensionLevel}/4)", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            val cost = activeCar.tuning.suspensionLevel * 5000
                            Button(
                                onClick = { vm.upgradeSuspensionLevel() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                enabled = activeCar.tuning.suspensionLevel < 4
                            ) {
                                Text(if (activeCar.tuning.suspensionLevel >= 4) "MAXED" else "Melhorar ($${cost.toLocaleString()})", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { vm.showLscTuningMenu = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
                        ) {
                            Text("SAIR DA OFICINA DE TUNAGEM", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 2. SIMEON'S CONCESSIONARIA CAR SELLER MENU
        if (vm.showCarDealerMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(440.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CONCESSIONÁRIA SIMEON YETARIAN 🚗", color = Color(0xFF3B82F6), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Escolha e compre supercarros da vida real para acelerar na cidade!", color = Color.LightGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).height(240.dp)) {
                            LazyColumn {
                                items(CAR_CATALOG) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Inspirado: ${item.realLifeCounterpart}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            Text("Velocidade: ${(item.baseMaxSpeed * 22f).toInt()} km/h", color = Color(0xFFFDE047), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Button(
                                            onClick = { vm.buySimeonCar(item) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                            enabled = player.cash >= item.price
                                        ) {
                                            Text("Comprar ($${item.price.toLocaleString()})", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { vm.showCarDealerMenu = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
                        ) {
                            Text("Fechar Catálogo", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 3. AMMU-NATION BUY ARMS CATÁLOGO
        if (vm.showAmmuNationMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(420.dp)
                        .background(Color(0xFF052E16), RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AMMU-NATION GUNSHOP 🔫", color = Color(0xFF10B981), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Mantenha-se armado e protegido com os melhores coletes!", color = Color.LightGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        val wepsForSale = WeaponType.values().filter { it != WeaponType.FISTS }
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                            LazyColumn {
                                items(wepsForSale) { bulletType ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(Color(0xFF14532D), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(bulletType.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Dano Base: ${bulletType.baseDamage.toInt()} | Munição: +${bulletType.maxAmmo / 2}", color = Color.LightGray, fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { vm.purchaseWeapon(bulletType) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                            enabled = player.cash >= bulletType.price
                                        ) {
                                            Text("Adquirir ($${bulletType.price.toLocaleString()})", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Max Health / Armor healing refill pack trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF15803D), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kit Colete Tático & Saúde Completo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Button(
                                onClick = {
                                    if (player.cash >= 1200) {
                                        player.cash -= 1200
                                        player.health = 100f
                                        player.armor = 100f
                                        GameSound.playCash()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                enabled = player.cash >= 1200
                            ) {
                                Text("Comprar ($1.200)", color = Color.Black, fontSize = 9.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { vm.showAmmuNationMenu = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
                        ) {
                            Text("Fechar Menu Ammu", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 4. HOUSE PURCHASE SUITE POPUP
        vm.activeHouseToBuy?.let { house ->
            if (vm.showHousePurchaseMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xBB000000))
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(380.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                            .border(3.dp, Color(android.graphics.Color.parseColor(house.color)), RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ADQUIRIR MANSÃO / ASTRÓLOGA 🏡", color = Color(android.graphics.Color.parseColor(house.color)), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                            Text(house.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(house.description, color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                "VALOR: $${house.price.toLocaleString()}",
                                color = Color(0xFFEAB308),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row {
                                Button(
                                    onClick = { vm.showHousePurchaseMenu = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Voltar", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { vm.purchaseHouseProperty(house) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    modifier = Modifier.weight(1.5f),
                                    enabled = player.cash >= house.price
                                ) {
                                    Text("COMPRAR CHAVE 🔑", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4.7 CLOTHES STORE SUITE POPUP (Boutique Luanda Outlet)
        ClothesBoutiqueMenu(vm = vm, player = player)

        // ==========================================
        //  CRITICAL GAME SCREEN OVERLAYS: DEATH / WASTED
        // ==========================================
        if (player.isDead) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD9991B1B)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ELIMINADO",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                        style = MaterialTheme.typography.displayMedium.copy(
                            shadow = Shadow(color = Color.Black, blurRadius = 15f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Você foi nocauteado pela polícia. Perda médica confiscada: $1.000",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // -------------------------------------------------------------
        //  iFruit SMS TOAST NOTIFICATION
        // -------------------------------------------------------------
        vm.activeSmsSender?.let { sender ->
            vm.activeSmsText?.let { text ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .width(320.dp)
                        .background(Color(0xE60F172A), RoundedCornerShape(12.dp))
                        .border(2.dp, Color(0xFF6366F1), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF312E81), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(sender.uppercase(), color = Color(0xFFFDE047), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(text, color = Color.White, fontSize = 10.sp, maxLines = 2)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        //  CELL PHONE (TELEFÓVEL) BUTTON ACTION TRIGGER
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(54.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
                    ),
                    CircleShape
                )
                .border(1.5.dp, Color.White, CircleShape)
                .clickable { vm.isPhoneOpen = !vm.isPhoneOpen },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📱", fontSize = 18.sp)
                Text("TELEFONE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        // -------------------------------------------------------------
        //  SMARTPHONE OVERLAY DEVICE FRAME
        // -------------------------------------------------------------
        if (vm.isPhoneOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 74.dp)
                    .width(300.dp)
                    .fillMaxHeight(0.85f)
                    .background(Color(0xFF0B0F19), RoundedCornerShape(24.dp))
                    .border(4.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Status Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("iFruit 5G", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ 100%", color = Color(0xFF22C55E), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📶", fontSize = 10.sp)
                        }
                    }

                    // 2. Main Phone Screen content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF1E1B4B))
                    ) {
                        when (vm.currentPhoneApp) {
                            PhoneApp.HOME -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "12:00 PM",
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            shadow = Shadow(color = Color.Black, blurRadius = 8f)
                                        ),
                                        fontWeight = FontWeight.Light,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text("Los Santos, SA", color = Color.LightGray, fontSize = 10.sp)
                                    
                                    Spacer(modifier = Modifier.height(26.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            PhoneIconItem(icon = "💬", name = "Mensagens", color = Color(0xFF22C55E)) {
                                                vm.currentPhoneApp = PhoneApp.CONTACTS
                                            }
                                            PhoneIconItem(icon = "🚗", name = "Souther SA", color = Color(0xFFEF4444)) {
                                                vm.currentPhoneApp = PhoneApp.VEHICLE_SHOP
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            PhoneIconItem(icon = "🏡", name = "Dynasty 8", color = Color(0xFFF59E0B)) {
                                                vm.currentPhoneApp = PhoneApp.DYNASTY8
                                            }
                                            PhoneIconItem(icon = "ℹ️", name = "Guia Rápido", color = Color(0xFF3B82F6)) {
                                                vm.currentPhoneApp = PhoneApp.INFO
                                            }
                                        }
                                    }
                                }
                            }
                            PhoneApp.CONTACTS -> {
                                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    PhoneHeader(title = "Contatos iFruit", onBack = { vm.currentPhoneApp = PhoneApp.HOME })
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    val contacts = listOf(
                                        Triple("lamar", "Lamar Davis 🟢", Color(0xFF22C55E)),
                                        Triple("lester", "Lester Crest 💻", Color(0xFFA855F7)),
                                        Triple("simeon", "Simeon Concess 🚗", Color(0xFF3B82F6)),
                                        Triple("mecanico", "Mecânico Geral 🔧", Color(0xFFF59E0B))
                                    )

                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(contacts) { (id, name, color) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        vm.activeContactId = id
                                                        vm.currentPhoneApp = PhoneApp.CHAT
                                                    }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(32.dp).background(color, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                            PhoneApp.CHAT -> {
                                val contactId = vm.activeContactId
                                val history = vm.chatHistory[contactId] ?: remember { mutableStateListOf() }
                                Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    PhoneHeader(title = vm.getContactName(contactId), onBack = { vm.currentPhoneApp = PhoneApp.CONTACTS })
                                    
                                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp)) {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(history) { msg ->
                                                val align = if (msg.isFromPlayer) Alignment.CenterEnd else Alignment.CenterStart
                                                val bg = if (msg.isFromPlayer) Color(0xFF4F46E5) else Color(0xFF334155)
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = align
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .widthIn(max = 180.dp)
                                                            .background(bg, RoundedCornerShape(10.dp))
                                                            .padding(8.dp)
                                                    ) {
                                                        Text(msg.text, color = Color.White, fontSize = 10.5.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Ações de Comunicação:", color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
                                        
                                        when (contactId) {
                                            "lamar" -> {
                                                PhoneActionBtn(text = "Pedir Conselho (Dicas)") {
                                                    val randomTips = listOf(
                                                        "Se a polícia embaçar, entre num motorizado e acelere ou chame o Lester!",
                                                        "Guarde sua grana! Compre uma Super Mansão de Vinewood para se curar de graça!",
                                                        "As missões geradas dinamicamente dão muita grana e XP!",
                                                        "A caneca de tuning LSC melhora drasticamente o motor do esportivo!"
                                                    )
                                                    vm.sendPlayerSms("lamar", "Lamar, manda a visão geral da quebrada!", randomTips.random())
                                                }
                                                PhoneActionBtn(text = "Recrutar Cobertura ($1.500)") {
                                                    vm.sendPlayerSms("lamar", "Lamar, manda cobertura de distrito!", "Mandei os parças tocarem o terror! Olha as explosões corporativas!", 1500) {
                                                        vm.explosions.add(
                                                            Explosion(
                                                                id = "lamar_back_${UUID.randomUUID()}",
                                                                x = player.x + (Math.random().toFloat() * 100f - 50f),
                                                                y = player.y + (Math.random().toFloat() * 100f - 50f),
                                                                maxRadius = 120f
                                                            )
                                                        )
                                                        vm.screenShakeAmount = 20f
                                                    }
                                                }
                                            }
                                            "lester" -> {
                                                val hasWanted = player.wantedLevel > 0
                                                val cost = player.wantedLevel * 500
                                                PhoneActionBtn(
                                                    text = if (hasWanted) "Subornar Tiras (-$cost)" else "Sem Procura Ativa",
                                                    enabled = hasWanted
                                                ) {
                                                    vm.sendPlayerSms(
                                                        "lester",
                                                        "Lester, os federais tão na minha bota! Limpa meu Wanted Level!",
                                                        "Resolvido mano! Hackeei o banco de dados da LSPD. Suas estrelas sumiram!",
                                                        cost
                                                    ) {
                                                        player.wantedLevel = 0
                                                        player.wantedPoints = 0f
                                                    }
                                                }
                                            }
                                            "simeon" -> {
                                                PhoneActionBtn(text = "Exportação de Luxo") {
                                                    vm.sendPlayerSms(
                                                        "simeon",
                                                        "Simeon, tem algum carro especial pra exportar?",
                                                        "Amigo! Entregue encomendas dinamitadas pelo botão verde (Missão) no menu da rua principal para fazer riquezas!"
                                                    )
                                                }
                                            }
                                            "mecanico" -> {
                                                if (player.ownedCars.isEmpty()) {
                                                    Text("Nenhum transporte comprado ainda para requisitar!", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(4.dp))
                                                } else {
                                                    Text("Entregar veículo perto de você:", color = Color.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                            items(player.ownedCars.distinct()) { catalogId ->
                                                                val catItem = CAR_CATALOG.find { it.id == catalogId }
                                                                catItem?.let { item ->
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                                                            .clickable {
                                                                                vm.sendPlayerSms(
                                                                                    "mecanico",
                                                                                    "Traz meu ${item.brand} ${item.name} para a minha posição!",
                                                                                    "A caminho chefe! Deixei o veículo estacionado bem do seu lado, use o botão AÇÃO para pilotar!"
                                                                                ) {
                                                                                    val spawnId = "mechanic_deliv_${UUID.randomUUID()}"
                                                                                    vm.cars.add(
                                                                                        GameCar(
                                                                                            id = spawnId,
                                                                                            catalogId = item.id,
                                                                                            name = item.name,
                                                                                            brand = item.brand,
                                                                                            realLifeCounterpart = item.realLifeCounterpart,
                                                                                            x = player.x + 40f,
                                                                                            y = player.y + 40f,
                                                                                            angle = player.angle,
                                                                                            speed = 0f,
                                                                                            health = 100f,
                                                                                            isPlayerOwned = true,
                                                                                            color = item.defaultColor,
                                                                                            tuning = CarTuning(primaryColor = item.defaultColor)
                                                                                        )
                                                                                    )
                                                                                }
                                                                            }
                                                                            .padding(6.dp),
                                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                                    ) {
                                                                        Text(item.name, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                                        Text(item.type.displayName, color = Color.LightGray, fontSize = 8.sp)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            PhoneApp.VEHICLE_SHOP -> {
                                var selectedTab by remember { mutableStateOf(VehicleType.CAR) }
                                Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    PhoneHeader(title = "Concessionária Online", onBack = { vm.currentPhoneApp = PhoneApp.HOME })
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val types = VehicleType.values()
                                        types.forEach { t ->
                                            val isSel = selectedTab == t
                                            Box(
                                                modifier = Modifier
                                                    .background(if (isSel) Color(0xFF4F46E5) else Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                                    .clickable { selectedTab = t }
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(t.displayName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    val filteredItems = CAR_CATALOG.filter { it.type == selectedTab }
                                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            items(filteredItems) { item ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                        .padding(6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text("Real: ${item.realLifeCounterpart}", color = Color.LightGray, fontSize = 8.sp)
                                                        Text("Max: ${(item.baseMaxSpeed * 22f).toInt()} KM/H", color = Color(0xFFFDE047), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Button(
                                                        onClick = { vm.selectVehicleToBuyFromPhone(item) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Text("$${item.price.toLocaleString()}", fontSize = 8.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            PhoneApp.DYNASTY8 -> {
                                Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    PhoneHeader(title = "Dynasty 8 Imóveis", onBack = { vm.currentPhoneApp = PhoneApp.HOME })
                                    
                                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            items(HOUSE_CATALOG) { house ->
                                                val isOwned = player.ownedHouses.contains(house.id)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                        .padding(6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(house.name, color = Color(android.graphics.Color.parseColor(house.color)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text(house.description, color = Color.LightGray, fontSize = 8.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    if (isOwned) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color(0xFF15803D), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Text("ADQUIRIDA 🔑", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = { vm.buyMansionFromPhone(house) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(24.dp)
                                                        ) {
                                                            Text("$${house.price.toLocaleString()}", fontSize = 8.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            PhoneApp.INFO -> {
                                Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    PhoneHeader(title = "Manual de Jogo", onBack = { vm.currentPhoneApp = PhoneApp.HOME })
                                    
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        item {
                                            InfoCard(title = "Controles de Jogo 🎮", text = "Use o Joystick esquerdo para andar a pé ou dirigir veículos. Para atirar, equipe uma arma e pressione o botão vermelho ATIRAR!")
                                        }
                                        item {
                                            InfoCard(title = "Entrar em Carros 🚗", text = "Ande até qualquer veículo trafegando na rua ou chamado pelo mecânico e pressione o botão azul AÇÃO • F para entrar/sair.")
                                        }
                                        item {
                                            InfoCard(title = "Loja de Armas 🔫", text = "Visite o Ammu-Nation (círculo verde no mapa) para comprar pistolas, MP5s, escopetas e RPGs devastadores!")
                                        }
                                        item {
                                            InfoCard(title = "Dica de Casa própria 🏡", text = "Ao comprar propriedades pelo app Dynasty 8 no telemóvel, visite-as para ter CURA COMPLETA & MUNIÇÕES RECARREGADAS DE GRAÇA na hora!")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Bottom Button Gestures bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .background(Color(0xFF0F172A))
                            .clickable { vm.currentPhoneApp = PhoneApp.HOME },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(4.dp)
                                .background(Color.LightGray, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        //  DELIVERY HOUSE CHOOSER DIALOG
        // -------------------------------------------------------------
        vm.clickedVehicleToBuy?.let { vBuy ->
            if (vm.showDeliveryDestinationMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xDD000000))
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(380.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                            .border(3.dp, Color(0xFF3B82F6), RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ENTREGA DO TRANSPORTE 🚚", color = Color(0xFF3B82F6), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Adquirir ${vBuy.brand} ${vBuy.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Selecione para qual das suas mansões a concessionária deve entregar este veículo novo em folha:", color = Color.LightGray, fontSize = 10.5.sp, textAlign = TextAlign.Center)
                            
                            Spacer(modifier = Modifier.height(14.dp))

                            // List of owned houses
                            val ownedHouses = player.ownedHouses
                            if (ownedHouses.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        .clickable { vm.deliverPurchasedVehicle("current") }
                                        .padding(10.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Text("📍 Entregar na minha Posição Atual", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("(Você ainda não possui nenhuma casa!", color = Color.Yellow, fontSize = 8.5.sp)
                                        Text("Compre casas para ter garagens particulares!)", color = Color.Yellow, fontSize = 8.5.sp)
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(ownedHouses) { hId ->
                                            val hObj = HOUSE_CATALOG.find { it.id == hId }
                                            hObj?.let { house ->
                                                val col = Color(android.graphics.Color.parseColor(house.color))
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                                        .border(1.dp, col, RoundedCornerShape(10.dp))
                                                        .clickable { vm.deliverPurchasedVehicle(house.id) }
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(house.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("Entregar 🔑", color = col, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                                    .clickable { vm.deliverPurchasedVehicle("current") }
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("📍 Posição Atual do Jogador", color = Color.LightGray, fontSize = 11.sp)
                                                Text("Entregar", color = Color.White, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { vm.showDeliveryDestinationMenu = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar Compra", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. LARGE MAP OVERLAY GPS SYSTEM
        if (vm.isLargeMapOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD000000))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .width(420.dp)
                        .wrapContentHeight(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MENU DE PAUSA - GPS DE LOS SANTOS 🗺️",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(onClick = { vm.isLargeMapOpen = false }, modifier = Modifier.size(24.dp)) {
                                Text("✖", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Toque em qualquer local do mapa para marcar seu rumo (GPS Linha Amarela)",
                            color = Color.LightGray,
                            fontSize = 8.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Outer Box for Canvas and Labels
                        val mapSizeDp = 340.dp
                        Box(
                            modifier = Modifier
                                .size(mapSizeDp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(2.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val scaleX = size.width.toFloat() / 2000f
                                        val scaleY = size.height.toFloat() / 2000f
                                        vm.customWaypointX = (offset.x / scaleX).coerceIn(0f, 2000f)
                                        vm.customWaypointY = (offset.y / scaleY).coerceIn(0f, 2000f)
                                        GameSound.playCash()
                                    }
                                }
                        ) {
                            // 1. Drawing Canvas for background, grid, roads, and lines
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val scaleX = size.width / 2000f
                                val scaleY = size.height / 2000f

                                // Grid lines
                                for (i in 1..4) {
                                    val gx = (2000f / 5) * i * scaleX
                                    val gy = (2000f / 5) * i * scaleY
                                    drawLine(Color(0x1F475569), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                                    drawLine(Color(0x1F475569), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                                }

                                // Draw horizontal roads
                                CityGenerator.ROAD_AVENUES_H.forEach { ry ->
                                    val rcy = ry * scaleY
                                    drawRect(
                                        color = Color(0xFF334155),
                                        topLeft = Offset(0f, rcy - (CityGenerator.ROAD_WIDTH / 2f) * scaleY),
                                        size = Size(size.width, CityGenerator.ROAD_WIDTH * scaleY)
                                    )
                                    drawLine(
                                        color = Color(0x33FFFFFF),
                                        start = Offset(0f, rcy),
                                        end = Offset(size.width, rcy),
                                        strokeWidth = 1.5f * scaleY
                                    )
                                }

                                // Draw vertical roads
                                CityGenerator.ROAD_AVENUES_V.forEach { rx ->
                                    val rcx = rx * scaleX
                                    drawRect(
                                        color = Color(0xFF334155),
                                        topLeft = Offset(rcx - (CityGenerator.ROAD_WIDTH / 2f) * scaleX, 0f),
                                        size = Size(CityGenerator.ROAD_WIDTH * scaleX, size.height)
                                    )
                                    drawLine(
                                        color = Color(0x33FFFFFF),
                                        start = Offset(rcx, 0f),
                                        end = Offset(rcx, size.height),
                                        strokeWidth = 1.5f * scaleX
                                    )
                                }

                                // Draw custom route yellow line
                                if (vm.customWaypointX != null && vm.customWaypointY != null) {
                                    val pX = player.x * scaleX
                                    val pY = player.y * scaleY
                                    val wX = vm.customWaypointX!! * scaleX
                                    val wY = vm.customWaypointY!! * scaleY

                                    drawLine(
                                        color = Color(0xFFFACC15),
                                        start = Offset(pX, pY),
                                        end = Offset(wX, wY),
                                        strokeWidth = 4f
                                    )
                                }

                                // Draw mission red line
                                player.activeMission?.let { m ->
                                    if (m.status == "active") {
                                        val pX = player.x * scaleX
                                        val pY = player.y * scaleY
                                        val cargoX = if (m.hasPickedUpCargo) m.targetX else (m.checkpointX ?: m.targetX)
                                        val cargoY = if (m.hasPickedUpCargo) m.targetY else (m.checkpointY ?: m.targetY)

                                        drawLine(
                                            color = Color(0xFFEF4444).copy(alpha = 0.7f),
                                            start = Offset(pX, pY),
                                            end = Offset(cargoX * scaleX, cargoY * scaleY),
                                            strokeWidth = 3f
                                        )
                                    }
                                }
                            }

                            // 2. Composable Local helpers inside the overlay
                            val LandmarkOverlay: @Composable (Float, Float, String, String, Color) -> Unit = { x, y, emoji, label, color ->
                                val floatX = x / 2000f
                                val floatY = y / 2000f
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = (floatX * 340f).dp - 9.dp,
                                            y = (floatY * 340f).dp - 9.dp
                                        )
                                        .size(18.dp)
                                        .background(color, CircleShape)
                                        .border(1f.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 7.5.sp)
                                }
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .offset(
                                            x = (floatX * 340f).dp + 11.dp,
                                            y = (floatY * 340f).dp - 7.dp
                                        )
                                        .background(Color(0xD9000000), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 2.dp, vertical = 0.5.dp)
                                )
                            }

                            // Draw Landmarks
                            LandmarkOverlay(CityGenerator.AMMU_NATION.x + 40f, CityGenerator.AMMU_NATION.y + 30f, "🔫", "Ammu-Nation", Color(0xFF10B981))
                            LandmarkOverlay(CityGenerator.CAR_DEALER.x + 45f, CityGenerator.CAR_DEALER.y + 35f, "🚗", "Siméon", Color(0xFF3B82F6))
                            LandmarkOverlay(CityGenerator.LS_CUSTOMS.x + 50f, CityGenerator.LS_CUSTOMS.y + 40f, "🔧", "LS Customs", Color(0xFFA855F7))
                            LandmarkOverlay(CityGenerator.MAZE_BANK.x + 50f, CityGenerator.MAZE_BANK.y + 50f, "🏦", "Banco Maze", Color(0xFFDC2626))
                            LandmarkOverlay(CityGenerator.FRIENDS_HOUSE.x + 30f, CityGenerator.FRIENDS_HOUSE.y + 30f, "👤", "Casa do Amigo", Color(0xFFEC4899))

                            // Draw Buyable Houses
                            HOUSE_CATALOG.forEach { house ->
                                val hx = house.x + house.width / 2f
                                val hy = house.y + house.height / 2f
                                val owned = player.ownedHouses.contains(house.id)
                                LandmarkOverlay(hx, hy, "🏠", if (owned) "Minha Casa" else "Mansão", if (owned) Color(0xFF22C55E) else Color(0xFFEAB308))
                            }

                            // Draw Custom Waypoint
                            if (vm.customWaypointX != null && vm.customWaypointY != null) {
                                val wx = vm.customWaypointX!!
                                val wy = vm.customWaypointY!!
                                val floatX = wx / 2000f
                                val floatY = wy / 2000f
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = (floatX * 340f).dp - 10.dp,
                                            y = (floatY * 340f).dp - 10.dp
                                        )
                                        .size(20.dp)
                                        .background(Color(0xFFFACC15), CircleShape)
                                        .border(1.5.dp, Color.Black, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📍", fontSize = 9.sp)
                                }
                            }

                            // Draw Player cowboy position
                            val pFloatX = player.x / 2000f
                            val pFloatY = player.y / 2000f
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (pFloatX * 340f).dp - 10.dp,
                                        y = (pFloatY * 340f).dp - 10.dp
                                    )
                                    .size(20.dp)
                                    .background(Color.White, CircleShape)
                                    .border(1.5.dp, Color.Black, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🤠", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (vm.customWaypointX != null) {
                                Button(
                                    onClick = {
                                        vm.customWaypointX = null
                                        vm.customWaypointY = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Limpar GPS ❌", fontSize = 10.sp, color = Color.White)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Button(
                                onClick = { vm.isLargeMapOpen = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Voltar ao Jogo 🎮", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
//  CELL PHONE COMPOSE HELPER COMPONENTS
// ---------------------------------------------------------------------
@Composable
fun PhoneIconItem(icon: String, name: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PhoneHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .clickable { onBack() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("< Voltar", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun PhoneActionBtn(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(26.dp),
        contentPadding = PaddingValues(vertical = 2.dp, horizontal = 4.dp)
    ) {
        Text(text, color = if (enabled) Color.White else Color.Gray, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoCard(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(title, color = Color.Yellow, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text, color = Color.LightGray, fontSize = 9.sp)
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

// Format locale
fun Int.toLocaleString(): String {
    return "$ %,d".format(this).trim().replace(',', '.')
}

@Composable
fun LargeGpsMapOverlay(vm: GameViewModel, player: GamePlayer) {
    if (vm.isLargeMapOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6090F1D))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.92f)
                    .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                    .border(3.dp, Color(0xFFEAB308), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🗺️ MAPA AMPLIADO DE LUANDA 🇦🇴",
                                color = Color(0xFFEAB308),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Toque em qualquer ponto do mapa para marcar destino e ver o trajeto amarelo do GPS!",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                        // Clear Waypoint Button
                        if (vm.customWaypointX != null && vm.customWaypointY != null) {
                            Button(
                                onClick = {
                                    vm.customWaypointX = null
                                    vm.customWaypointY = null
                                    vm.triggerSmsToast("Operadora GPS 📍", "Todas as rotas marcadas foram apagadas do GPS.")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Limpar Rota ❌", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Map Area Box
                    var mapWidth by remember { mutableStateOf(400f) }
                    var mapHeight by remember { mutableStateOf(400f) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF0F1725), RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                            .onSizeChanged {
                                mapWidth = if (it.width > 0) it.width.toFloat() else 400f
                                mapHeight = if (it.height > 0) it.height.toFloat() else 400f
                            }
                    ) {
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val roadScaleX = mapWidth / 2000f
                        val roadScaleY = mapHeight / 2000f

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val scaleX = offset.x / mapWidth
                                        val scaleY = offset.y / mapHeight
                                        vm.customWaypointX = scaleX * 2000f
                                        vm.customWaypointY = scaleY * 2000f
                                        vm.triggerSmsToast("Navegador GPS 📍", "Destino marcado! Siga a linha amarela no mapa ou radar.")
                                        GameSound.playCash()
                                    }
                                }
                        ) {
                            // Draw Streets / Avenues
                            CityGenerator.ROAD_AVENUES_H.forEach { rh ->
                                val y = rh * roadScaleY
                                drawRect(Color(0xFF334155), Offset(0f, y - 6f), Size(size.width, 12f))
                            }
                            CityGenerator.ROAD_AVENUES_V.forEach { rv ->
                                val x = rv * roadScaleX
                                drawRect(Color(0xFF334155), Offset(x - 6f, 0f), Size(12f, size.height))
                            }

                            // Draw special designated building markers
                            listOf(
                                CityGenerator.AMMU_NATION,
                                CityGenerator.CAR_DEALER,
                                CityGenerator.LS_CUSTOMS,
                                CityGenerator.MAZE_BANK,
                                CityGenerator.FRIENDS_HOUSE,
                                CityGenerator.MIRALDINO_HOUSE,
                                CityGenerator.CLOTHES_SHOP
                            ).forEach { store ->
                                val sX = store.x * roadScaleX
                                val sY = store.y * roadScaleY
                                val sW = store.width * roadScaleX
                                val sH = store.height * roadScaleY
                                
                                val sColor = when (store.type) {
                                    "store" -> Color(0xFF065F46)
                                    "customs" -> Color(0xFF5B21B6)
                                    "bank" -> Color(0xFF991B1B)
                                    "home" -> Color(0xFF3730A3)
                                    "clothes" -> Color(0xFFD97706)
                                    else -> Color(0xFF854D0E)
                                }

                                drawRect(sColor, Offset(sX, sY), Size(sW, sH))
                                drawRect(Color.White.copy(alpha = 0.5f), Offset(sX, sY), Size(sW, sH), style = Stroke(width = 1.5f))
                            }

                            // Player coordinate representation (Green blinking circle)
                            val pX = player.x * roadScaleX
                            val pY = player.y * roadScaleY
                            val pulseInterval = ((System.currentTimeMillis() / 250) % 2).toInt() == 0
                            drawCircle(
                                color = if (pulseInterval) Color(0xFF22C55E) else Color(0xFF4ADE80),
                                radius = 11f,
                                center = Offset(pX, pY)
                            )
                            drawCircle(Color.White, 4f, Offset(pX, pY))

                            // Waypoint representation (pulsing yellow line and circle)
                            if (vm.customWaypointX != null && vm.customWaypointY != null) {
                                val wX = vm.customWaypointX!! * roadScaleX
                                val wY = vm.customWaypointY!! * roadScaleY

                                // Yellow Direct Route path line
                                drawLine(
                                    color = Color(0xFFFACC15).copy(alpha = 0.9f),
                                    start = Offset(pX, pY),
                                    end = Offset(wX, wY),
                                    strokeWidth = 4f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )

                                drawCircle(
                                    color = Color(0xFFFACC15).copy(alpha = if (pulseInterval) 0.8f else 0.4f),
                                    radius = 14f,
                                    center = Offset(wX, wY)
                                )
                                drawCircle(Color(0xFFFACC15), 6f, Offset(wX, wY))
                                drawCircle(Color.White, 2.5f, Offset(wX, wY))
                            }
                        }
                        
                        // Floating Label names on top of building areas
                        listOf(
                            Pair(CityGenerator.AMMU_NATION, "Ammu-Nation 🔫"),
                            Pair(CityGenerator.CAR_DEALER, "Concessionária 🚗"),
                            Pair(CityGenerator.LS_CUSTOMS, "Tuning LSC 🔧"),
                            Pair(CityGenerator.MAZE_BANK, "Banco Maze 💰"),
                            Pair(CityGenerator.FRIENDS_HOUSE, "Casa do Amigo 🏡"),
                            Pair(CityGenerator.MIRALDINO_HOUSE, "Minha Vivenda 🇦🇴🏡"),
                            Pair(CityGenerator.CLOTHES_SHOP, "Loja de Roupas 👔")
                        ).forEach { (store, name) ->
                            val labelX = with(density) { (store.x * roadScaleX).toDp() }
                            val labelY = with(density) { (store.y * roadScaleY - 14f).toDp() }
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = labelX, y = labelY)
                                    .background(Color(0xD90F172A), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                textLabel(name)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Back Close Button
                    Button(
                        onClick = { vm.isLargeMapOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar Mapa Ampliado 🗺️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun textLabel(txt: String) {
    Text(txt, color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
}

@Composable
fun ClothesBoutiqueMenu(vm: GameViewModel, player: GamePlayer) {
    if (vm.showClothesMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
                    .border(3.dp, Color(0xFF10B981), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "👔 BOUTIQUE LUANDA OUTLET 🇦🇴",
                        color = Color(0xFF10B981),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Seja bem-vindo, Miraldino Malungo! Vista-se bem e viva com estilo em Luanda.",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Render outfit options
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    ) {
                        items(CLOTHING_OUTFITS) { outfit ->
                            val isOwned = player.ownedOutfits.contains(outfit.id)
                            val isActive = player.activeOutfitId == outfit.id
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                    .border(
                                        2.dp,
                                        if (isActive) Color(0xFFEAB308) else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { vm.buyClothing(outfit) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = outfit.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = outfit.description,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Draw sample color swatches
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Visual: ", color = Color.Gray, fontSize = 9.sp)
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(android.graphics.Color.parseColor(outfit.jacketColor)), CircleShape)
                                                .border(1.dp, Color.White, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(android.graphics.Color.parseColor(outfit.pantsColor)), CircleShape)
                                                .border(1.dp, Color.White, CircleShape)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    if (isActive) {
                                        Text(
                                            text = "EQUIPADO",
                                            color = Color(0xFFEAB308),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    } else if (isOwned) {
                                        Text(
                                            text = "POSSUÍDO",
                                            color = Color(0xFF22C55E),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "[ Equipar ]",
                                            color = Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    } else {
                                        Text(
                                            text = "$${outfit.price.toLocaleString()}",
                                            color = if (player.cash >= outfit.price) Color(0xFF10B981) else Color(0xFFEF4444),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Comprar",
                                            color = Color.LightGray,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { vm.showClothesMenu = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sair da Boutique", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
