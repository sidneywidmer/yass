package ch.yass.unit

import ch.yass.admin.dsl.inMemoryGame
import ch.yass.admin.dsl.interpretCard
import ch.yass.game.bot.CanWinTrickWithLastCard
import ch.yass.game.bot.CanWinTrickWithLastCardLowest
import ch.yass.game.bot.CanWinTrickWithLastCardNoTrump
import ch.yass.game.bot.getPlayCandidate
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.playerAtPosition
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChooseCardTest {

    @Test
    fun testCanWinTrick() {
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
                        trick(north = "C9", east = "C7", south = "C10", west = null)
                    }
                }
            }
        }

        // CQ is correct. Not C6 (not winning trick) and not S6 or S10 (both _could_ win because they're trump
        // but we should not waste trumps if not necessary). Also not CK, just take the lowest card.
        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)
        val candidate = getPlayCandidate(player, state)
        assertTrue(candidate.card == interpretCard("CQ"))
        assertTrue {
            candidate.reasons.containsAll(
                setOf(
                    CanWinTrickWithLastCard, CanWinTrickWithLastCardLowest, CanWinTrickWithLastCardNoTrump
                )
            )
        }
    }

    @Test
    fun testLeadWithHighestIfSuitTrump() {
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
                        trick(north = null, east = null, south = null, west = null)
                    }
                }
            }
        }

        val player = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val candidate = getPlayCandidate(player, state)
        assertTrue(candidate.card == interpretCard("SJ"))
    }

    @Test
    fun testLeadWithBestIfUneufe() {
        val state = inMemoryGame {
            hands {
                hand {
                    trump(Trump.UNEUFE)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CA,D8,D9,H8,H9,S7,SA")
                    west(cards = "C6,CQ,CK,D6,DJ,DK,HJ,S6,S10")
                    tricks {
                        trick(north = null, east = null, south = null, west = null)
                    }
                }
            }
        }

        val player = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val candidate = getPlayCandidate(player, state)
        assertTrue(candidate.card == interpretCard("H6"))
    }

    @Test
    fun testLeadWithBestIfObeabe() {
        val state = inMemoryGame {
            hands {
                hand {
                    trump(Trump.OBEABE)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CA,D8,D9,H8,H9,S7,SA")
                    west(cards = "C6,CQ,CK,D6,DJ,DK,HJ,S6,S10")
                    tricks {
                        trick(north = null, east = null, south = null, west = null)
                    }
                }
            }
        }

        val player = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val candidate = getPlayCandidate(player, state)
        assertTrue(candidate.card == interpretCard("HK"))
    }

}
