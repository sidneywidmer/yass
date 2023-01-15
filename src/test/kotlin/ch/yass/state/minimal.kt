package ch.yass.state

import ch.yass.dsl.game

fun minimal() =
    game {
        players {
            north(name = "ueli", bot = false)
            east(name = "doris", bot = false)
            south(name = "christoph", bot = false)
            west(name = "daniela", bot = false)
        }
        hands {
            hand {
                trump(null)
                north(cards = "welcome", start = true)
                east(cards = "welcome")
                south(cards = "welcome")
                west(cards = "welcome")
                tricks {
                    trick(north = "WH", east = "WH", south = "WH", west = null)
                }
            }
        }
    }

