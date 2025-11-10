package ch.yass.unit

import arrow.core.raise.either
import ch.yass.admin.dsl.inMemoryGame
import ch.yass.admin.dsl.interpretCard
import ch.yass.core.error.CardUndertrumps
import ch.yass.game.bot.CanWinTrickWithLastCard
import ch.yass.game.bot.CanWinTrickWithLastCardLowest
import ch.yass.game.bot.CanWinTrickWithLastCardNoTrump
import ch.yass.game.bot.getPlayCandidate
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.cardIsPlayable
import ch.yass.game.engine.playerAtPosition
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class UndertrumpUnplayableTest {

    @Test
    fun testCanNotUndertrump() {
        val state = inMemoryGame {
            hands {
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CA,D8,D9,H8,H9,S7,SA")
                    west(cards = "C6,CQ,CK,D6,DJ,DK,HJ,S6,S10")
                    tricks {
                        trick(north = "C9", east = "C7", south = "SA", west = null)
                    }
                }
            }
        }

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)
        either { cardIsPlayable(interpretCard("S10")!!, player, state) }
            .onLeft { assertTrue { it is CardUndertrumps } }
    }

    @Test
    fun testCanUndertrumpIfOnlyTrumpsLeft() {
        val state = inMemoryGame {
            hands {
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CA,D8,D9,H8,H9,S7,SA")
                    west(cards = "S10")
                    tricks {
                        trick(north = "C9", east = "C7", south = "SA", west = null)
                    }
                }
            }
        }

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)
        either { cardIsPlayable(interpretCard("S10")!!, player, state) }
            .onLeft { fail() }
    }


    @Test
    fun testTrumpIfPlayedTrumpIsHigher() {
        val state = inMemoryGame {
            hands {
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CA,D8,D9,H8,H9,S7,SA")
                    west(cards = "C6,CQ,CK,D6,DJ,DK,HJ,S6,S10")
                    tricks {
                        trick(north = "C9", east = "C7", south = "S7", west = null)
                    }
                }
            }
        }

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)
        either { cardIsPlayable(interpretCard("S10")!!, player, state) }
            .onLeft { fail() }
    }

}
