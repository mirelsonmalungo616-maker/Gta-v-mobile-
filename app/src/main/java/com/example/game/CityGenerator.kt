package com.example.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object CityGenerator {
    const val CITY_SIZE = 2000f
    const val ROAD_WIDTH = 80f

    // Coordinate positions of main horizontal/vertical roadways
    val ROAD_AVENUES_H = listOf(100f, 400f, 700f, 1100f, 1500f, 1900f)
    val ROAD_AVENUES_V = listOf(100f, 500f, 900f, 1300f, 1700f, 1900f)

    // Critical locations
    val AMMU_NATION = CityBuilding("ammu_nation", 850f, 450f, 80f, 60f, "store", "#064e3b", "#065f46", "Ammu-Nation")
    val CAR_DEALER = CityBuilding("car_dealer", 1250f, 750f, 90f, 70f, "store", "#1e3a8a", "#1d4ed8", "Concessionária Simeon")
    val LS_CUSTOMS = CityBuilding("ls_customs", 550f, 1250f, 100f, 80f, "customs", "#4c1d95", "#5b21b6", "Los Santos Customs")
    val MAZE_BANK = CityBuilding("maze_bank", 950f, 950f, 100f, 100f, "bank", "#311005", "#7e1919", "Banco Maze")
    val FRIENDS_HOUSE = CityBuilding("friends_house", 250f, 1650f, 60f, 60f, "friend_house", "#4c0519", "#db2777", "Casa do Amigo")
    val MIRALDINO_HOUSE = CityBuilding("miraldino_house", 650f, 1650f, 60f, 60f, "home", "#1e1b4b", "#c084fc", "Minha Casa (Miraldino)  🇧")
    val CLOTHES_SHOP = CityBuilding("clothes_shop", 1050f, 1650f, 70f, 60f, "clothes", "#0f5132", "#198754", "Loja de Roupas")

    val BUILDINGS: List<CityBuilding> = generateBuildings()

    private fun generateBuildings(): List<CityBuilding> {
        val list = mutableListOf<CityBuilding>()
        
        // Add landmarks
        list.add(AMMU_NATION)
        list.add(CAR_DEALER)
        list.add(LS_CUSTOMS)
        list.add(MAZE_BANK)
        list.add(FRIENDS_HOUSE)
        list.add(MIRALDINO_HOUSE)
        list.add(CLOTHES_SHOP)

        // Block-by-block procedural filling inside sidewalk squares
        for (i in 0 until ROAD_AVENUES_V.size - 1) {
            val v1 = ROAD_AVENUES_V[i] + ROAD_WIDTH / 2f
            val v2 = ROAD_AVENUES_V[i + 1] - ROAD_WIDTH / 2f

            for (j in 0 until ROAD_AVENUES_H.size - 1) {
                val h1 = ROAD_AVENUES_H[j] + ROAD_WIDTH / 2f
                val h2 = ROAD_AVENUES_H[j + 1] - ROAD_WIDTH / 2f

                // Avoid overlapping special designated stores
                if (overlapsSpecial(v1, v2, h1, h2)) continue

                // Procedural small houses/skyscrapers inside the grid block
                var currentX = v1 + 15f
                while (currentX < v2 - 40f) {
                    var currentY = h1 + 15f
                    while (currentY < h2 - 40f) {
                        val bW = min(50f + (currentX % 30f), v2 - 10f - currentX)
                        val bH = min(50f + (currentY % 30f), h2 - 10f - currentY)

                        if (bW > 25f && bH > 25f) {
                            val seed = (currentX * 17f + currentY * 31f).toInt()
                            val type = when (seed % 4) {
                                0 -> "sky"
                                1 -> "office"
                                2 -> "residential"
                                else -> "park" // open empty park, no collision
                            }

                            if (type != "park") {
                                val color = when (type) {
                                    "sky" -> "#1e293b" // slate glass sky scraper
                                    "office" -> "#4b5563" // lighter gray
                                    else -> "#78350f" // brown brick
                                }
                                val roof = when (type) {
                                    "sky" -> "#0f172a"
                                    "office" -> "#374151"
                                    else -> "#92400e"
                                }
                                list.add(
                                    CityBuilding(
                                        id = "b_${currentX.toInt()}_${currentY.toInt()}",
                                        x = currentX,
                                        y = currentY,
                                        width = bW,
                                        height = bH,
                                        type = type,
                                        color = color,
                                        roofColor = roof
                                    )
                                )
                            }
                        }
                        currentY += bH + 25f
                    }
                    currentX += 80f
                }
            }
        }
        return list
    }

    private fun overlapsSpecial(v1: Float, v2: Float, h1: Float, h2: Float): Boolean {
        listOf(AMMU_NATION, CAR_DEALER, LS_CUSTOMS, MAZE_BANK, FRIENDS_HOUSE, MIRALDINO_HOUSE, CLOTHES_SHOP).forEach { store ->
            val sV1 = store.x - 20f
            val sV2 = store.x + store.width + 20f
            val sH1 = store.y - 20f
            val sH2 = store.y + store.height + 20f

            if (max(v1, sV1) < min(v2, sV2) && max(h1, sH1) < min(h2, sH2)) {
                return true
            }
        }
        // Protect house properties too
        HOUSE_CATALOG.forEach { house ->
            val hV1 = house.x - 20f
            val hV2 = house.x + house.width + 20f
            val hH1 = house.y - 20f
            val hH2 = house.y + house.height + 20f

            if (max(v1, hV1) < min(v2, hV2) && max(h1, hH1) < min(h2, hH2)) {
                return true
            }
        }
        return false
    }

    data class CollisionResult(
        val hit: Boolean,
        val adjustX: Float = 0f,
        val adjustY: Float = 0f,
        val blockId: String? = null
    )

    fun checkCollision(x: Float, y: Float, radius: Float): CollisionResult {
        // World edge bounds
        if (x - radius < 0f) return CollisionResult(true, radius - x, 0f)
        if (x + radius > CITY_SIZE) return CollisionResult(true, CITY_SIZE - radius - x, 0f)
        if (y - radius < 0f) return CollisionResult(true, 0f, radius - y)
        if (y + radius > CITY_SIZE) return CollisionResult(true, 0f, CITY_SIZE - radius - y)

        // Building collision
        for (b in BUILDINGS) {
            val closestX = max(b.x, min(x, b.x + b.width))
            val closestY = max(b.y, min(y, b.y + b.height))

            val dx = x - closestX
            val dy = y - closestY
            val distSq = dx * dx + dy * dy

            if (distSq < radius * radius) {
                val dist = sqrt(distSq)
                if (dist == 0f) {
                    val centerX = b.x + b.width / 2f
                    val push = if (x < centerX) -radius else radius
                    return CollisionResult(true, push, 0f, b.id)
                } else {
                    val overlap = radius - dist
                    return CollisionResult(true, (dx / dist) * overlap, (dy / dist) * overlap, b.id)
                }
            }
        }

        return CollisionResult(false)
    }

    fun getRandomRoadPoint(): Pair<Float, Float> {
        val useHorizontal = (Math.random() > 0.5)
        return if (useHorizontal) {
            val ry = ROAD_AVENUES_H.random()
            val rx = 50f + (Math.random().toFloat() * (CITY_SIZE - 100f))
            Pair(rx, ry + (Math.random().toFloat() * 10f - 5f))
        } else {
            val rx = ROAD_AVENUES_V.random()
            val ry = 50f + (Math.random().toFloat() * (CITY_SIZE - 100f))
            Pair(rx + (Math.random().toFloat() * 10f - 5f), ry)
        }
    }
}
