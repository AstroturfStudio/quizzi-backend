package service

import dto.GameRoomDTO
import model.GameRoom
import model.Player
import service.internal.RoomService

/**
 * @author guvencenanguvenal
 */
class RoomManagerService private constructor() {
    companion object {
        val INSTANCE: RoomManagerService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { RoomManagerService() }
    }

    private val roomService: RoomService = RoomService()

    fun getAllRooms(): MutableMap<String, GameRoom> = roomService.getAllRooms()

    fun getRoomById(id: String) = roomService.getRoomById(id)

    fun getRoomByPlayerId(playerId: String) = roomService.getRoomByPlayerId(playerId)

    suspend fun createRoom(name: String, playerId: String, categoryId: Int, gameType: String): GameRoom {
        val creatorPlayer = PlayerManagerService.INSTANCE.getPlayer(playerId)
        val room = roomService.createRoom(name, creatorPlayer, categoryId, gameType)
        return room
    }

    fun joinRoom(player: Player, roomId: String): Boolean = roomService.joinRoom(player, roomId)

    fun rejoinRoom(player: Player, roomId: String): Boolean = roomService.rejoinRoom(player, roomId)

    suspend fun closeRoom(room: GameRoom) = roomService.closeRoom(room)

    suspend fun playerDisconnected(playerId: String) {
        roomService.playerDisconnected(playerId)
    }

    fun getActiveRooms(): List<GameRoomDTO> {
        return roomService.getAllRooms().map { (id, room) ->
            GameRoomDTO(
                id = id,
                name = room.name,
                playerCount = room.getPlayerCount(),
                roomState = room.getState(),
                players = room.getPlayerNames()
            )
        }
    }
}