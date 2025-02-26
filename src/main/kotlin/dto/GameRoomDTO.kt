package dto

import kotlinx.serialization.Serializable
import state.RoomState

@Serializable
data class GameRoomDTO(
    val id: String,
    val name: String,
    val playerCount: Int,
    val gameType: String,
    val category: String,
    val players: List<String>,
    val roomState: RoomState,
)