package model

interface CursorBasedGame {
    var cursorPosition: Float
    fun updateCursorPosition(newPosition: Float)
    fun isCursorAtLimit(): Boolean
}