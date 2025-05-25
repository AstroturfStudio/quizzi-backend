package model

import domain.RoomEvent
import dto.GameRoomDTO
import exception.RoomIsEmpty
import exception.TooMuchPlayersInRoom
import exception.WrongCommandWrongTime
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import response.ServerSocketMessage
import service.CategoryService
import service.RoomBroadcastService
import service.RoomManagerService
import state.GameState
import state.PlayerState
import state.RoomState
import util.Logger
import java.util.*

@Serializable
data class GameRoom(
    val id: String,
    val name: String,
    val game: Game,
    private val players: MutableSet<PlayerInRoom> = Collections.synchronizedSet(mutableSetOf())
) {
    companion object {
        const val COUNTDOWN_TIME = 3L
    }

    private val gameScope = CoroutineScope(Dispatchers.Default + Job())

    private var state: RoomState = RoomState.Waiting

    fun getState(): RoomState = state

    suspend fun transitionTo(newState: RoomState) {
        if (state == newState) {
            return
        }

        when (state) {
            RoomState.Waiting -> {}
            RoomState.Countdown -> {
                if (newState is RoomState.Waiting) {
                    throw IllegalStateException("Invalid transition from Countdown to $newState")
                }
            }

            RoomState.Pausing -> {
                if (newState !is RoomState.Countdown && newState !is RoomState.Closing) {
                    throw IllegalStateException("Invalid transition from Playing to $newState")
                }
            }

            RoomState.Playing -> {
                if (newState is RoomState.Countdown) {
                    throw IllegalStateException("Invalid transition from Playing to $newState")
                }
            }

            RoomState.Closing -> {
                if (newState is RoomState.Countdown || newState is RoomState.Playing) {
                    throw IllegalStateException("Invalid transition from Closed to $newState")
                }
            }

        }
        state = newState
        onStateChanged(newState)
    }

    suspend fun handleEvent(event: RoomEvent) {
        when (state) {
            RoomState.Waiting -> {}
            RoomState.Countdown -> {
                if (event !is RoomEvent.Disconnected) {
                    throw WrongCommandWrongTime()
                }
            }

            RoomState.Pausing -> {}
            RoomState.Playing -> {
                if (event !is RoomEvent.Disconnected) {
                    throw WrongCommandWrongTime()
                }
            }

            RoomState.Closing -> {
                if (event !is RoomEvent.Disconnected) {
                    throw WrongCommandWrongTime()
                }
            }

        }
        onProcessEvent(event)
    }

    private suspend fun onStateChanged(newState: RoomState) {
        broadcastRoomState()
        when (newState) {
            RoomState.Waiting -> {}
            RoomState.Countdown -> {
                countdownBeforeStart()
                transitionTo(RoomState.Playing)
            }

            RoomState.Pausing -> {
                game.transitionTo(GameState.Pause)
            }

            RoomState.Playing -> {
                gameScope.launch {
                    game.transitionTo(GameState.Playing)
                }
            }

            RoomState.Closing -> {
                if (game.getState() !is GameState.Over) {
                    game.transitionTo(GameState.Over)
                }
                RoomManagerService.INSTANCE.closeRoom(this)
            }
        }
    }

    private suspend fun onProcessEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.Created -> {
                addPlayer(event.player)
                broadcastRoomState()
            }

            is RoomEvent.Joined -> {
                addPlayer(event.player)
                broadcastRoomState()
            }

            is RoomEvent.Ready -> {
                playerReady(event.playerId)
                broadcastRoomState()
                if (isAllPlayerReady()) {
                    transitionTo(RoomState.Countdown)
                }
            }

            is RoomEvent.Disconnected -> {
                removePlayer(event.playerId)

                if (players.isEmpty()) {
                    transitionTo(RoomState.Closing)
                    throw RoomIsEmpty(id)
                }

                if (game.getState() is GameState.Over) {
                    return
                }
                transitionTo(RoomState.Pausing)

                val disconnectMessage = ServerSocketMessage.PlayerDisconnected(
                    playerId = event.playerId
                )
                broadcast(disconnectMessage)
            }

            RoomEvent.Status -> {
                broadcastRoomState()
            }
        }
    }

    /////////////////////////////////

    fun getPlayerCount(): Int = players.size

    fun getPlayerNames(): List<String> = players.map { it.name }

    fun getPlayers(): Set<PlayerInRoom> = players

    /////////////////////////////////

    private suspend fun broadcast(message: ServerSocketMessage) {
        Logger.i("Broadcasting message to room ${id}: $message")
        RoomBroadcastService.INSTANCE.broadcast(id, message)
    }

    private suspend fun broadcastRoomState() {
        Logger.i("Broadcasting game state for room $id")


        val gameUpdate = ServerSocketMessage.RoomUpdate(
            players = players.map { it.toDTO() },
            state = state,
            gameRoom = GameRoomDTO(
                id,
                name,
                players.size,
                game.type,
                CategoryService.getCategoryById(game.categoryId).toDto(),
                players.map { it.name },
                state
            )
        )
        broadcast(gameUpdate)
    }

    /////////////////////////////////

    private fun addPlayer(player: Player) {
        if (players.size >= game.maxPlayerCount()) throw TooMuchPlayersInRoom()
        val index = players.size
        players.add(player.toPlayerInRoom(index))
        game.players.add(player.toPlayerInGame(index))
    }

    private fun removePlayer(playerId: String) {
        players.removeIf { p -> p.id == playerId }
        game.players.removeIf { p -> p.id == playerId }
    }

    private fun isAllPlayerReady(): Boolean {
        val notReadyPlayers = players.filter { player -> player.state == PlayerState.WAIT }.size
        return (notReadyPlayers == 0) && (game.maxPlayerCount() == players.size)
    }

    private fun playerReady(playerId: String) {
        players
            .filter { player -> player.id == playerId }
            .forEach { player -> player.state = PlayerState.READY }
    }

    private suspend fun countdownBeforeStart() {
        Logger.i("Starting countdown for room $id")
        for (timeLeft in COUNTDOWN_TIME downTo 1) {
            delay(1000)
            val countdownTimeUpdate = ServerSocketMessage.CountdownTimeUpdate(remaining = timeLeft)
            broadcast(countdownTimeUpdate)
        }
        delay(1000)
    }
}
