package service

import model.Game
import model.ResistToTimeGame
import model.ResistanceGame

/**
 * @author guvencenanguvenal
 */
class GameFactory private constructor() {
    companion object {
        val INSTANCE: GameFactory by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { GameFactory() }
        val resistanceGame: String = "Resistance Game"
        val resistToTimeGame: String = "Resist To Time Game"

        fun getAllGameTypes(): Set<String> {
            return mutableSetOf(resistanceGame, resistToTimeGame)
        }
    }

    fun createGame(id: String, categoryId: Int, type: String, roomId: String): Game {
        return when (type) {
            resistanceGame -> ResistanceGame(id, categoryId, roomId)
            resistToTimeGame -> ResistToTimeGame(id, categoryId, roomId)
            else -> ResistanceGame(id, categoryId, roomId)
        }
    }
}