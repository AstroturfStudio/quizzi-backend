package state

import kotlinx.serialization.Serializable

@Serializable
sealed class RoundState {

    @Serializable
    data object Start : RoundState()

    @Serializable
    data object Interrupt : RoundState()

    @Serializable
    data object End : RoundState()
}