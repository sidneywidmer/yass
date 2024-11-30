package ch.yass.integration
import arrow.core.raise.recover
import ch.yass.admin.dsl.game
import ch.yass.admin.dsl.interpretCards
import ch.yass.core.error.InvalidState
import ch.yass.core.error.PlayerIsLocked
import ch.yass.game.Foresight
import ch.yass.game.GameRepository
import ch.yass.game.GameService
import ch.yass.game.api.ChooseTrumpRequest
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.SchiebeRequest
import ch.yass.game.api.WeisenRequest
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.State
import ch.yass.game.dto.Trump
import ch.yass.game.dto.Weis
import ch.yass.game.dto.WeisType
import ch.yass.game.engine.playerAtPosition
import ch.yass.game.engine.pointsByPositionTotal
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance

class PlayerTest : Integration() {
    private val service: GameService = container.direct.instance()
    private val repo: GameRepository = container.direct.instance()
    private val foresight: Foresight = container.direct.instance()

    /**
     * 4 Players in the game and the welcome hand is dealt but not played. s turn.
     */
    private fun getState(): GameState {
        return game {
            players {
                north(name = "ueli", bot = false)
                east(name = "doris", bot = false)
                south(name = "christoph", bot = false)
                west(name = "daniela", bot = false)
            }
            hands {
                hand {
                    trump(Trump.FREESTYLE)
                    gschobe(Gschobe.NO)
                    north(cards = "welcome", start = true)
                    east(cards = "welcome")
                    south(cards = "welcome")
                    west(cards = "welcome")
                    tricks { }
                }
            }
        }
    }

    /**
     * Init empty game and manually play cards for players and always check if points and
     * game state are correct.
     */
    @Test
    fun testManuallyPlayGame() {
        val state = getState()

        checkWrongStartPlayer(state)

        prepareHand1() // We do this here because the last card in the welcome hand will init the new hand automatically
        playWelcomeHand(state)
        playHand1Trick1(state).also { checkPointsTrick1(it) }
    }

    private fun checkPointsTrick1(state: GameState) {
        // All points are times 2 because trump is HEARTS
        val points = pointsByPositionTotal(state.hands, state.tricks, state.seats)
        assertThat(points[Position.NORTH]!!.cardPoints, equalTo(0))
        assertThat(points[Position.NORTH]!!.weisPoints, equalTo(40)) // DREI_BLATT
        assertThat(points[Position.EAST]!!.cardPoints, equalTo(26)) // 10 and Queen
        assertThat(points[Position.EAST]!!.weisPoints, equalTo(0))
        assertThat(points[Position.SOUTH]!!.cardPoints, equalTo(0))
        assertThat(points[Position.SOUTH]!!.weisPoints, equalTo(140)) // DREI_BLATT and VIER_BLATT
        assertThat(points[Position.WEST]!!.cardPoints, equalTo(0))
        assertThat(points[Position.WEST]!!.weisPoints, equalTo(0))
    }

    private fun prepareHand1() {
        // Has a DREI_BLATT for 20 Points (S9-SJ)
        val cardsN = interpretCards("S9,S10,SJ,SA,DQ,D9,CK,CQ,H10")
        // Will schiebe and has STOECK if west chooses HEARTS as trump (fingers crossed)
        val cardsE = interpretCards("HQ,HK,H6,H7,DJ,DK,C6,C7,SQ")
        // Has a VIER_BLATT for 50 Points (C8-CJ) and a DREI_BLATT for 20 Points (S6-S8)
        val cardsS = interpretCards("C8,C9,C10,CJ,S6,S7,S8,CA,DA")
        // Has a DREI_BLATT for 20 Points (D6-D9)
        val cardsW = interpretCards("D6,D7,D8,D10,H8,H9,HJ,HA,SK")

        // Manipulate our luck a little
        foresight.pushDeck(cardsN + cardsE + cardsS + cardsW)
    }

    private fun playHand1Trick1(state: GameState): GameState {
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)!!
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)!!
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)!!
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)!!

        // EAST is our start player, let's quickly make sure the state waits for schiebe and we can't just play a card
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "QUEEN", "french"))
            service.play(request, east)
        }) { assertTrue(it is InvalidState && it.nextState == State.SCHIEBE) }

        // The correct player (east) now declares: yes, schiebe please (it's now WEST's turn to declare trump)
        var stateAfterSchiebe = recover({
            val request = SchiebeRequest(state.game.uuid.toString(), "YES")
            service.schiebe(request, east)
        }) { fail() }

        assertThat(stateAfterSchiebe.hands[0].trump, nullValue())
        assertThat(stateAfterSchiebe.hands[0].gschobe, equalTo(Gschobe.YES))

        var stateAfterTrump = recover({
            val request = ChooseTrumpRequest(stateAfterSchiebe.game.uuid.toString(), "HEARTS")
            service.trump(request, west)
        }) { fail() }

        assertThat(stateAfterTrump.hands[0].trump, equalTo(Trump.HEARTS))

        // East plays the first card of the hand (and the trick)
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "QUEEN", "french"))
            service.play(request, east)
        }) { fail() }

        // Next up south, has no hearts so they play a low spades - but oh no! They should first declare their weis.
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "SIX", "french"))
            service.play(request, south)
        }) { assertTrue(it is InvalidState && it.nextState == State.WEISEN_FIRST) }

        // Okay then, let's weis our VIER_BLATT for 50 Points (C9-CJ) first
        var stateAfterWeisS = recover({
            val weis = Weis(WeisType.VIER_BLATT, interpretCards("C8,C9,C10,CJ"))
            val request = WeisenRequest(state.game.uuid.toString(), weis)
            service.weisen(request, south)
        }) { fail() }

        assertThat(stateAfterWeisS.hands[0].weiseOf(Position.SOUTH).size, equalTo(1))
        assertThat(stateAfterWeisS.hands[0].weiseOf(Position.SOUTH).first().type, equalTo(WeisType.VIER_BLATT))
        assertThat(stateAfterWeisS.hands[0].weiseOf(Position.SOUTH).first().cards.size, equalTo(4))

        // Try again to play that darn S6, this time it should work
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "SIX", "french"))
            service.play(request, south)
        }) { fail() }

        // West can also weis a DREI_BLATT...
        recover({
            val weis = Weis(WeisType.DREI_BLATT, interpretCards("D6,D7,D8"))
            val request = WeisenRequest(state.game.uuid.toString(), weis)
            service.weisen(request, west)
        }) { fail() }

        // ...and play a card. Can follow suit with H9
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "EIGHT", "french"))
            service.play(request, west)
        }) { fail() }

        // Does anyone NOT have a weis these days? North shows their DREI_BLATT...
        recover({
            val weis = Weis(WeisType.DREI_BLATT, interpretCards("S9,S10,SJ"))
            val request = WeisenRequest(state.game.uuid.toString(), weis)
            service.weisen(request, north)
        }) { fail() }

        // ...and plays H10 because they must follow suit
        var stateAfterAllCards = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "TEN", "french"))
            service.play(request, north)
        }) { fail() }

        // Now something special happened: The Team (N/S, E/W) with the most weise automatically weises any remaining
        // weise they might have. In our case SOUTH has another DREI_BLATT (S6-S8). Also, a new (empty) trick is initiated
        assertThat(stateAfterAllCards.tricks.size, equalTo(3)) // new trick...
        assertThat(stateAfterAllCards.tricks[0].cards().size, equalTo(0)) // ...that is also still empty

        assertThat(stateAfterAllCards.tricks[1].cards().size, equalTo(4))
        assertThat(stateAfterAllCards.hands[0].weiseOf(Position.NORTH).size, equalTo(1))
        assertThat(stateAfterAllCards.hands[0].weiseOf(Position.EAST).size, equalTo(0))
        assertThat(stateAfterAllCards.hands[0].weiseOf(Position.WEST).size, equalTo(1))
        assertThat(stateAfterAllCards.hands[0].weiseOf(Position.SOUTH).size, equalTo(2))
        assertTrue(stateAfterAllCards.hands[0].weiseOf(Position.SOUTH).any { it.type ==  WeisType.DREI_BLATT})
        assertTrue(stateAfterAllCards.hands[0].weiseOf(Position.SOUTH).any { it.type ==  WeisType.VIER_BLATT})

        return stateAfterAllCards
    }

    private fun playWelcomeHand(state: GameState) {
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)!!
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)!!
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)!!
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)!!

        // NORTH plays the first card
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))
            service.play(request, north)
        }) { fail() }

        // Next up EAST
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))
            service.play(request, east)
        }) { fail() }

        // SOUTH
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))
            service.play(request, south)
        }) { fail() }

        // WEST
        var state = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))
            service.play(request, west)
        }) { fail() }

        // By now we should have a new hand dealt (should be deterministic), a new trick started and check for Centrifugo calls
        assertThat(state.hands.size, equalTo(2))
        assertThat(state.tricks.size, equalTo(2))

        assertThat(state.tricks[1].cards().size, equalTo(4)) // all welcome cards
        assertThat(state.hands[1].trump, equalTo(Trump.FREESTYLE))
        assertThat(state.hands[1].startingPosition, equalTo(Position.NORTH)) // see game state above start = ...
        assertThat(state.hands[1].gschobe, equalTo(Gschobe.NO))
        assertThat(Position.entries.flatMap { state.hands[1].weiseOf(it) }.size, equalTo(0))

        assertThat(state.tricks[0].cards().size, equalTo(0)) // no cards played in new trick yet
        assertThat(state.hands[0].trump, nullValue())
        assertThat(state.hands[0].startingPosition, equalTo(Position.EAST)) // comes next after NORTH
        assertThat(state.hands[0].gschobe, equalTo(Gschobe.NOT_YET))
        assertThat(Position.entries.flatMap { state.hands[0].cardsOf(it) }.size, equalTo(36)) // all cards are dealt
        assertThat(Position.entries.flatMap { state.hands[0].weiseOf(it) }.size, equalTo(0))
    }

    private fun checkWrongStartPlayer(state: GameState) {
        // We start off with a bang, it's not souths turn so this should not be possible
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)!!
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))
            service.play(request, south)
        }) { assertTrue(it is PlayerIsLocked) }
    }
}
