import exception.SocketCloseError
import handler.MessageHandler
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import router.gameRoutes
import router.playerRoutes
import router.roomRoutes
import service.PlayerManagerService
import service.SessionManagerService
import java.time.Duration
import util.Logger

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        get("/") {
            call.respondText("Flag Quiz Game Server Running!")
        }

        roomRoutes()
        playerRoutes()
        gameRoutes()

        webSocket("/game") {
            val playerId = call.parameters["playerId"] ?: run {
                Logger.w("WebSocket connection attempt without playerId")
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing playerId"))
                return@webSocket
            }

            Logger.i("New WebSocket connection: $playerId")
            
            try {
                val player = PlayerManagerService.INSTANCE.getPlayer(playerId)
                
                SessionManagerService.INSTANCE.addPlayerToSession(playerId, this)
                Logger.i("Player $playerId added to session")

                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                Logger.d("Received message from player $playerId: $text")
                                MessageHandler.INSTANCE.handleMessage(playerId, text)
                            }
                            is Frame.Close -> {
                                Logger.i("Received close frame from player $playerId")
                            }
                            else -> {
                                Logger.w("Received unsupported frame type from player $playerId: ${frame.frameType}")
                            }
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    Logger.i("WebSocket connection closed normally for player $playerId")
                }
            } catch (e: SocketCloseError) {
                Logger.e("Socket close error for player $playerId: ${e.message}")
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, e.message ?: "Socket close error"))
            } catch (e: Exception) {
                Logger.e("Unexpected error for player $playerId", e)
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unexpected Error!"))
            } finally {
                Logger.i("WebSocket connection terminated for player $playerId")
                MessageHandler.INSTANCE.handleDisconnect(playerId)
            }
        }
    }
}