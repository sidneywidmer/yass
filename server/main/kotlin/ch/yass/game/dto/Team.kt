package ch.yass.game.dto

enum class Team(val positions: List<Position>) {
    NS(listOf(Position.NORTH, Position.SOUTH)),
    EW(listOf(Position.EAST, Position.WEST));
}