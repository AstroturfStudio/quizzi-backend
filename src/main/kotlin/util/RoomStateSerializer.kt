package util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import state.RoomState

@Serializer(forClass = RoomState::class)
object RoomStateSerializer : KSerializer<RoomState> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RoomState", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RoomState) {
        val type = when (value) {
            RoomState.Waiting -> "Waiting"
            RoomState.Countdown -> "Countdown"
            RoomState.Pausing -> "Pausing"
            RoomState.Playing -> "Playing"
            RoomState.Closing -> "Closing"
        }
        encoder.encodeString(type)
    }

    override fun deserialize(decoder: Decoder): RoomState {
        return when (val type = decoder.decodeString()) {
            "Waiting" -> RoomState.Waiting
            "Countdown" -> RoomState.Countdown
            "Pausing" -> RoomState.Pausing
            "Playing" -> RoomState.Playing
            "Closing" -> RoomState.Closing
            else -> throw IllegalArgumentException("Unknown RoomState: $type")
        }
    }
}