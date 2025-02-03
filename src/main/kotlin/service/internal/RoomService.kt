package service.internal

import domain.RoomEvent
import exception.*
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.GameRoom
import model.Player
import response.DisconnectedPlayer
import service.GameFactory
import service.RoomBroadcastService
import service.SessionManagerService
import state.RoomState
import util.Logger
import java.util.*

/**
 * @author guvencenanguvenal
 */
class RoomService {

    private val rooms = Collections.synchronizedMap(mutableMapOf<String, GameRoom>())

    private val playerToRoom = Collections.synchronizedMap(mutableMapOf<String, String>())

    private val disconnectedPlayers =
            Collections.synchronizedMap(mutableMapOf<String, DisconnectedPlayer>())

    fun getAllRooms(): MutableMap<String, GameRoom> = rooms

    fun getRoomById(id: String) = rooms[id] ?: throw RoomNotFound(id)

    fun getRoomByPlayerId(playerId: String) = rooms[playerToRoom[playerId]] ?: throw RoomNotFound("from PlayerId")

    suspend fun createRoom(
            roomName: String,
            creator: Player,
            gameCategoryId: Int,
            gameType: String
    ): GameRoom {
        val roomId = UUID.randomUUID().toString()
        val game = GameFactory.INSTANCE.createGame(roomId, gameCategoryId, gameType, roomId)
        val room = GameRoom(roomId, roomName, game)
        synchronized(this) {
            rooms[roomId] = room
            playerToRoom[creator.id] = roomId
        }
        room.handleEvent(RoomEvent.Created(creator))
        Logger.i("Room $roomId created by player ${creator.id}")
        return room
    }

    fun joinRoom(player: Player, roomId: String) {
        synchronized(this) {
            val room = rooms[roomId] ?: throw RoomNotFound(roomId)
            val disconnectedPlayer = disconnectedPlayers[player.id]
            val playerInRoom = playerToRoom[player.id]

            if (playerInRoom != null || disconnectedPlayer != null) {
                throw AlreadyInAnotherRoom()
            }
            if (room.getPlayerCount() >= room.game.maxPlayerCount()) {
                throw TooMuchPlayersInRoom()
            }
            playerToRoom[player.id] = roomId
        }
    }

    fun rejoinRoom(player: Player, roomId: String): Boolean {
        val disconnectedPlayer = disconnectedPlayers[player.id] ?: throw NotYourRoom()

        val currentTime = System.currentTimeMillis()
        val thirtyMinutesInMillis = 30 * 60 * 1000

        if (disconnectedPlayer.roomId == roomId && (currentTime - disconnectedPlayer.disconnectTime) < thirtyMinutesInMillis) {
            playerToRoom[player.id] = roomId
            disconnectedPlayers.remove(player.id)
            return true
        }
        return false
    }

    suspend fun closeRoom(room: GameRoom) {
        val playerIds: List<String>
        synchronized(this) {
            playerIds = room.getPlayers().map { player ->
                playerToRoom.remove(player.id)
                player.id
            }
            rooms.remove(room.id)
        }
        playerIds.forEach { playerId ->
            SessionManagerService.INSTANCE.removePlayerSession(playerId)
        }
        RoomBroadcastService.INSTANCE.deleteRoom(room.id)
        Logger.i("${room.id} room is cleaned!")
    }

    suspend fun playerDisconnected(disconnectedPlayerId: String) {
        try {
            val room = getRoomByPlayerId(disconnectedPlayerId)

            try {
                room.handleEvent(RoomEvent.Disconnected(disconnectedPlayerId))
            } catch (_: RoomIsEmpty) {
                room.transitionTo(RoomState.Closing)
                return
            }

            disconnectedPlayers[disconnectedPlayerId] = DisconnectedPlayer(
                playerId = disconnectedPlayerId,
                roomId = room.id
            )
            playerToRoom.remove(disconnectedPlayerId)

            CoroutineScope(Dispatchers.Default).launch {
                delay(20000)
                if (room.getState() is RoomState.Pausing) {
                    room.transitionTo(RoomState.Closing)
                    Logger.i("Player $disconnectedPlayerId did not reconnect within 30 seconds, cleaning up room ${room.id}")
                }
                disconnectedPlayers.remove(disconnectedPlayerId)
            }
        } catch (e: RoomNotFound) {
            return
        }
    }
}