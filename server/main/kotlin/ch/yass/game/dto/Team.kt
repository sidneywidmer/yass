package ch.yass.game.dto

enum class Team(val positions: List<Position>) {
    NS(listOf(Position.NORTH, Position.SOUTH)),
    EW(listOf(Position.EAST, Position.WEST));

    fun getPartner(position: Position): Position? =
        if (position in positions) {
            positions.first { it != position }
        } else null

    companion object {
        fun getPartner(position: Position): Position =
            Team.entries.first { position in it.positions }.getPartner(position)!!

        fun getOpponents(position: Position): List<Position> =
            Team.entries.first { position !in it.positions }.positions
    }
}