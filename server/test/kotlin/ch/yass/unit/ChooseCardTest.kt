package ch.yass.unit

import ch.yass.admin.dsl.game
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Trump
import org.junit.jupiter.api.Test

class ChooseCardTest {
    @Test
    fun testCanWinTrick() {
        hier gehts weiter -> ich will ein inmemory state!
        // no players = create default nonbot players
        // no FREESTYLE trump -> create one automatically
        val state = game {
            hands {
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CQ,CA,D9,H8,H9,S7,SA")
                    west(cards = "C6,CK,D6,D8,DJ,DK,HJ,S6,S10")
                    tricks { }
                }
            }
        }
    }
}
