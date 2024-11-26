package model

import dto.PlayerDTO
import kotlinx.serialization.Serializable

/**
 * @author guvencenanguvenal
 */
@Serializable
abstract class Game(
    val id: String,
    val categoryId: Int,
    var currentQuestion: Question? = null
) {
    abstract fun nextQuestion(): Question
    abstract fun processAnswer(players: MutableList<PlayerDTO>, answeredPlayerId: String?, answer: Int?): Boolean

    abstract fun getRoundTime(): Long

    abstract fun maxPlayerCount(): Int
}