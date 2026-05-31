package com.example.game

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object MissionGenerator {

    private val TITLES_DELIVERY = listOf(
        Pair("Contrabando Expresso 📦", "Recupere o pacote misterioso e entregue no ponto sem bater o veículo!"),
        Pair("Carga de Diamantes Raros 💎", "Transporte o estojo de diamantes confiscados de volta ao porto!"),
        Pair("Substâncias Fortes 💊", "Leve o malote de fórmulas químicas ultra secretas até o abrigo subterrâneo.")
    )

    private val TITLES_CHASE = listOf(
        Pair("Infiltrado Fujão 🏎️", "Um delator está escapando em direção às docas! Alcance o veículo dele e neutralize-o."),
        Pair("Mustang Descontrolado 💀", "Pegue seu armamento! O chefe da gangue rival está cruzando as avenidas."),
        Pair("Malote Roubado 💼", "Intercepte o Audi suspeito que roubou ourivesaria do centro comercial agora!")
    )

    private val TITLES_PICKUP = listOf(
        Pair("Coleta nos Beco-Gatos ⚡", "Arquivos sobre suborno do Maze Bank caíram nas ruas. Recolha todas as pastas!"),
        Pair("Cargas Abandonadas 📦", "Uma furgoneta perdeu 3 carregamentos de munição furtiva. Consiga recolhê-los.")
    )

    private val TITLES_SURVIVAL = listOf(
        Pair("Armadilha no Centro 🩸", "O encontro de mafiosos era uma cilada! Sobreviva no local ao cerco inimigo militarizado."),
        Pair("Invasão de Quadra ⛺", "A gangue rival do morro está invadindo nosso território! Resista no perímetro marcado.")
    )

    fun generateProceduralMission(
        playerX: Float,
        playerY: Float,
        playerLevel: Int,
        playTime: Float
    ): Mission {
        val id = "proc_${System.currentTimeMillis()}"

        // Decide level difficulty
        val difficulty = when {
            playerLevel >= 5 || playTime > 300f -> "hard"
            playerLevel >= 3 || playTime > 120f -> "medium"
            else -> "easy"
        }

        // Cycle through type categories (exclude manually triggered Bank Heist)
        val types = MissionType.values().filter { it != MissionType.BANK_HEIST }
        val pickedType = types.random()

        // Scaler
        val rewardMultiplier = when (difficulty) {
            "hard" -> 2.2f
            "medium" -> 1.5f
            else -> 1.0f
        }

        val baseCash = 3000 + (playerLevel * 1000)
        val finalCash = (baseCash * rewardMultiplier).roundToInt()
        val finalXp = (400 + playerLevel * 150 * rewardMultiplier).roundToInt()

        // Spatial placing
        val (targetX, targetY) = CityGenerator.getRandomRoadPoint()
        val (checkpointX, checkpointY) = CityGenerator.getRandomRoadPoint()

        // Create clean text specs
        val (headline, detailText) = when (pickedType) {
            MissionType.DELIVERY -> TITLES_DELIVERY.random()
            MissionType.CHASE -> TITLES_CHASE.random()
            MissionType.ITEM_PICKUP -> TITLES_PICKUP.random()
            MissionType.SURVIVAL -> TITLES_SURVIVAL.random()
            MissionType.BANK_HEIST -> Pair("O Grande Assalto ao Banco 💰", "Limpe os cofres e fuja com a grana.")
        }

        // Calc distances for appropriate time limits
        val dx = targetX - playerX
        val dy = targetY - playerY
        val distance = sqrt(dx * dx + dy * dy)
        val calculatedSeconds = max(35, (distance / 10f).roundToInt() + 25)

        val descWithDetails = "$detailText\n\nDificuldade: ${difficulty.uppercase()}\nPrêmio: $$finalCash • XP: +$finalXp XP"

        return Mission(
            id = id,
            title = headline,
            description = descWithDetails,
            type = pickedType,
            rewardCash = finalCash,
            rewardXp = finalXp,
            status = "available",
            difficulty = difficulty,
            startX = playerX,
            startY = playerY,
            checkpointX = checkpointX,
            checkpointY = checkpointY,
            targetX = targetX,
            targetY = targetY,
            timeLimit = calculatedSeconds,
            timeRemaining = calculatedSeconds.toFloat()
        )
    }
}
