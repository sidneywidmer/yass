package ch.yass.integration

import arrow.core.raise.recover
import ch.yass.admin.dsl.game
import ch.yass.admin.dsl.interpretCard
import ch.yass.admin.dsl.interpretCards
import ch.yass.core.error.InvalidState
import ch.yass.core.error.PlayerIsLocked
import ch.yass.game.Foresight
import ch.yass.game.GameService
import ch.yass.game.api.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.engine.playerAtPosition
import ch.yass.game.engine.pointsByPositionTotal
import ch.yass.game.engine.positionSeat
import ch.yass.game.pubsub.CardPlayed
import ch.yass.game.pubsub.ClearPlayedCards
import ch.yass.game.pubsub.GameFinished
import ch.yass.game.pubsub.UpdatePossibleWeise
import ch.yass.integration.helper.CentrifugoTestHelper
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance

private data class PlayCard(val position: Position, val card: String)

class PlayerTest : Integration() {
    private val service: GameService = container.direct.instance()
    private val foresight: Foresight = container.direct.instance()
    private val cth = CentrifugoTestHelper(container.direct.instance())

    /**
     * 4 Players in the game and the welcome hand is dealt but not played. s turn.
     */
    private fun getState(): GameState {
        return game {
            settings {
                wcValue(400) // We'll be achieved in the first trick of the second hand
                wcType(WinningConditionType.POINTS)
            }
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
     * Init empty game and manually play cards for players and always check if points, overall
     * game state and any external services are correct or correctly called.
     */
    @Test
    fun testManuallyPlayGame() {
        var state = getState()

        checkWrongStartPlayer(state)

        prepareHand1()
        prepareHand2()
        state = playWelcomeHand(state)
        checkCentrifugoWelcomeHand(state)

        state = playHand1Trick1(state)
        checkPointsTrick1(state)
        checkCentrifugoTrick1(state)

        state = playHand1Trick2(state)
        checkPointsTrick2(state)

        state = playHand1Trick3To9(state)
        checkPointsForHand1(state)

        state = playHand2Trick1(state)
        checkPointsForHand2(state)
        checkCentrifugoHand2(state)
    }

    private fun checkCentrifugoHand2(state: GameState) {
        val northSeat = positionSeat(Position.NORTH, state.seats)
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)

        cth.assertActions(northSeat.uuid, cth.parseActions(centrifugo.allServeEvents)).apply {
            hasCount(GameFinished::class, 1)
            hasWinner(north)
            hasWinner(south)
        }
        centrifugo.resetRequests()
    }

    private fun checkCentrifugoWelcomeHand(state: GameState) {
        // There is so many of those events sent and we're not testing all of them yet but focus on the
        // most important ones.
        val north = positionSeat(Position.NORTH, state.seats)
        cth.assertActions(north.uuid, cth.parseActions(centrifugo.allServeEvents)).apply {
            hasCount(CardPlayed::class, 4) // Every player played a welcome hand
            hasCount(ClearPlayedCards::class, 1)
            hasState(State.NEW_HAND) // When welcome hand is finished new hand is dealt
            hasState(State.SCHIEBE) // And instantly goes to state SCHIEBE (first state in new trick)
            hasPlayedCard(Suit.WELCOME, Rank.SIX) // Actually 4 of those where played
        }
        centrifugo.resetRequests()
    }

    private fun checkCentrifugoTrick1(state: GameState) {
        val north = positionSeat(Position.NORTH, state.seats)
        cth.assertActions(north.uuid, cth.parseActions(centrifugo.allServeEvents)).apply {
            hasTrump(Trump.HEARTS)
            hasCount(UpdatePossibleWeise::class, 1)
            hasPlayedCard(Suit.HEARTS, Rank.TEN)
            hasPlayedCard(Suit.HEARTS, Rank.EIGHT)
            hasPlayedCard(Suit.SPADES, Rank.SIX)
            hasPlayedCard(Suit.HEARTS, Rank.QUEEN)
            hasWeis(WeisType.VIER_BLATT, 100)
            hasCount(ClearPlayedCards::class, 1)
        }
        centrifugo.resetRequests()
    }

    private fun checkPointsForHand2(state: GameState) {
        val points = pointsByPositionTotal(state.hands, state.tricks)
        assertThat(points.getValue(Position.NORTH).weisPoints, equalTo(40))
        assertThat(points.getValue(Position.EAST).weisPoints, equalTo(40))
        assertThat(points.getValue(Position.SOUTH).weisPoints, equalTo(140))
        assertThat(points.getValue(Position.WEST).weisPoints, equalTo(0))

        assertThat(points.getValue(Position.NORTH).cardPoints, equalTo(179)) // Only change versus points in hand 1
        assertThat(points.getValue(Position.EAST).cardPoints, equalTo(26))
        assertThat(points.getValue(Position.SOUTH).cardPoints, equalTo(64))
        assertThat(points.getValue(Position.WEST).cardPoints, equalTo(102))
    }

    private fun playHand2Trick1(state: GameState): GameState {
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)

        // EAST started the first hand, so it's now SOUTH's turn. They don't want to schiebe and choose UNEUFE trump
        recover({ service.schiebe(SchiebeRequest(state.game.uuid.toString(), "NO"), south) }) { fail() }
        recover({ service.trump(ChooseTrumpRequest(state.game.uuid.toString(), "UNEUFE"), south) }) { fail() }

        // SOUTH plays the first card in the first trick of the second hand
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "EIGHT", "french"))
            service.play(request, south)
        }) { fail() }

        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "NINE", "french"))
            service.play(request, west)
        }) { fail() }

        // NORTH will win the trick, S6 is bock
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "SIX", "french"))
            service.play(request, north)
        }) { fail() }

        val updatedState = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "SEVEN", "french"))
            service.play(request, east)
        }) { fail() }

        assertThat(updatedState.game.status, equalTo(GameStatus.FINISHED))

        return updatedState
    }

    private fun checkPointsForHand1(state: GameState) {
        val points = pointsByPositionTotal(state.hands, state.tricks)
        assertThat(points.getValue(Position.NORTH).weisPoints, equalTo(40))
        assertThat(points.getValue(Position.EAST).weisPoints, equalTo(40))
        assertThat(points.getValue(Position.SOUTH).weisPoints, equalTo(140))
        assertThat(points.getValue(Position.WEST).weisPoints, equalTo(0))

        assertThat(points.getValue(Position.NORTH).cardPoints, equalTo(122))
        assertThat(points.getValue(Position.EAST).cardPoints, equalTo(26))
        assertThat(points.getValue(Position.SOUTH).cardPoints, equalTo(64))
        assertThat(points.getValue(Position.WEST).cardPoints, equalTo(102))
    }

    private fun playHand1Trick3To9(state: GameState): GameState {
        // Let's speed this up a little. After meticulously asserting everything for trick 1 and 2 we can
        // just let the hand be played out and check if our state is correct at the end.
        val cardsPlayed = listOf(
            // Trick3: E->S->W*->N
            PlayCard(Position.WEST, "HJ"),
            PlayCard(Position.NORTH, "S9"),
            PlayCard(Position.EAST, "H6"),
            PlayCard(Position.SOUTH, "C8"),

            // Trick4: E->S->W*->N
            PlayCard(Position.WEST, "H9"),
            PlayCard(Position.NORTH, "SJ"),
            PlayCard(Position.EAST, "H7"),
            PlayCard(Position.SOUTH, "S8"),

            // Trick5: E->S->W->N*
            PlayCard(Position.WEST, "SK"),
            PlayCard(Position.NORTH, "SA"),
            PlayCard(Position.EAST, "SQ"),
            PlayCard(Position.SOUTH, "C10"),

            // Trick6: E->S*->W->N
            PlayCard(Position.NORTH, "DQ"),
            PlayCard(Position.EAST, "DK"),
            PlayCard(Position.SOUTH, "DA"),
            PlayCard(Position.WEST, "D6"),

            // Trick7: E->S*->W->N
            PlayCard(Position.SOUTH, "CA"),
            PlayCard(Position.WEST, "D7"),
            PlayCard(Position.NORTH, "CQ"),
            PlayCard(Position.EAST, "C6"),

            // Trick8: E->S->W->N*
            PlayCard(Position.SOUTH, "CJ"),
            PlayCard(Position.WEST, "D8"),
            PlayCard(Position.NORTH, "CK"),
            PlayCard(Position.EAST, "C7"),

            // Trick9: E->S->W->N* (last stich)
            PlayCard(Position.NORTH, "S10"),
            PlayCard(Position.EAST, "DJ"),
            PlayCard(Position.SOUTH, "C9"),
            PlayCard(Position.WEST, "D10")
        )

        val uuid = state.game.uuid.toString()
        var newState: GameState = state
        cardsPlayed.forEach { c ->
            val player = playerAtPosition(c.position, newState.seats, newState.allPlayers)
            val card = interpretCard(c.card)!!
            newState = recover({
                val request = PlayCardRequest(uuid, PlayedCard(card.suit.name, card.rank.name, "french"))
                service.play(request, player)
            }) { fail() }
        }

        assertThat(newState.hands.size, equalTo(3)) // New empty hand automatically created
        assertThat(newState.tricks.size, equalTo(11)) // 1 (welcome) + 9 (normal hand) + 1 (empty trick in new hand)
        assertThat(newState.hands[0].startingPosition, equalTo(Position.SOUTH))

        return newState
    }

    private fun checkPointsTrick2(state: GameState) {
        val points = pointsByPositionTotal(state.hands, state.tricks)
        assertThat(points.getValue(Position.NORTH).cardPoints, equalTo(0))
        assertThat(points.getValue(Position.NORTH).weisPoints, equalTo(40))
        assertThat(points.getValue(Position.EAST).cardPoints, equalTo(26))
        assertThat(points.getValue(Position.EAST).weisPoints, equalTo(40)) // +40 Stoeck
        assertThat(points.getValue(Position.SOUTH).cardPoints, equalTo(0))
        assertThat(points.getValue(Position.SOUTH).weisPoints, equalTo(140))
        assertThat(points.getValue(Position.WEST).cardPoints, equalTo(30)) // +30 (King plus Ace)
        assertThat(points.getValue(Position.WEST).weisPoints, equalTo(0))
    }

    private fun playHand1Trick2(state: GameState): GameState {
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)

        // East one the last trick so they go again first and again start with HEARTS which is also Stoeck
        val stateAfterStoeck = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "KING", "french"))
            service.play(request, east)
        }) { fail() }

        assertTrue(stateAfterStoeck.hands[0].weiseOf(Position.EAST).any { it.type == WeisType.STOECK })

        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "SEVEN", "french"))
            service.play(request, south)
        }) { fail() }

        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "ACE", "french"))
            service.play(request, west)
        }) { fail() }

        val stateAfterAllCards = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("DIAMONDS", "NINE", "french"))
            service.play(request, north)
        }) { fail() }

        assertThat(stateAfterAllCards.tricks.size, equalTo(4))

        return stateAfterAllCards
    }

    private fun checkPointsTrick1(state: GameState) {
        // All points are times 2 because trump is HEARTS
        val points = pointsByPositionTotal(state.hands, state.tricks)
        assertThat(points.getValue(Position.NORTH).cardPoints, equalTo(0))
        assertThat(points.getValue(Position.NORTH).weisPoints, equalTo(40)) // DREI_BLATT
        assertThat(points.getValue(Position.EAST).cardPoints, equalTo(26)) // 10 and Queen
        assertThat(points.getValue(Position.EAST).weisPoints, equalTo(0))
        assertThat(points.getValue(Position.SOUTH).cardPoints, equalTo(0))
        assertThat(points.getValue(Position.SOUTH).weisPoints, equalTo(140)) // DREI_BLATT and VIER_BLATT
        assertThat(points.getValue(Position.WEST).cardPoints, equalTo(0))
        assertThat(points.getValue(Position.WEST).weisPoints, equalTo(0))
    }

    private fun prepareHand2() {
        val cardsN = interpretCards("S6,H7,D6,C9,S10,HJ,DQ,CK,HA")
        val cardsE = interpretCards("S7,H8,D9,C10,SJ,HQ,DK,CA,H6")
        val cardsS = interpretCards("S8,H9,D10,CJ,SQ,HK,DA,C6,H10")
        val cardsW = interpretCards("S9,H10,DJ,CQ,SK,SA,D8,C7,D7")

        foresight.pushDeck(cardsN + cardsE + cardsS + cardsW)
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
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)

        // EAST is our start player, let's quickly make sure the state waits for schiebe and we can't just play a card
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "QUEEN", "french"))
            service.play(request, east)
        }) { assertTrue(it is InvalidState && it.nextState == State.SCHIEBE) }

        // The correct player (east) now declares: yes, schiebe please (it's now WEST's turn to declare trump)
        val stateAfterSchiebe = recover({
            val request = SchiebeRequest(state.game.uuid.toString(), "YES")
            service.schiebe(request, east)
        }) { fail() }

        assertThat(stateAfterSchiebe.hands[0].trump, equalTo(Trump.NONE))
        assertThat(stateAfterSchiebe.hands[0].gschobe, equalTo(Gschobe.YES))

        val stateAfterTrump = recover({
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
        val stateAfterWeis = recover({
            val weis = Weis(WeisType.VIER_BLATT, interpretCards("C8,C9,C10,CJ"))
            val request = WeisenRequest(state.game.uuid.toString(), weis)
            service.weisen(request, south)
        }) { fail() }

        assertThat(stateAfterWeis.hands[0].weiseOf(Position.SOUTH).size, equalTo(1))
        assertThat(stateAfterWeis.hands[0].weiseOf(Position.SOUTH).first().type, equalTo(WeisType.VIER_BLATT))
        assertThat(stateAfterWeis.hands[0].weiseOf(Position.SOUTH).first().cards.size, equalTo(4))

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
        val stateAfterAllCards = recover({
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
        assertTrue(stateAfterAllCards.hands[0].weiseOf(Position.SOUTH).any { it.type == WeisType.DREI_BLATT })
        assertTrue(stateAfterAllCards.hands[0].weiseOf(Position.SOUTH).any { it.type == WeisType.VIER_BLATT })

        return stateAfterAllCards
    }

    private fun playWelcomeHand(state: GameState): GameState {
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)

        // NORTH plays the first card
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "SIX", "french"))
            service.play(request, north)
        }) { fail() }

        // Next up EAST
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "SIX", "french"))
            service.play(request, east)
        }) { fail() }

        // SOUTH
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "SIX", "french"))
            service.play(request, south)
        }) { fail() }

        // WEST
        val newState = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "SIX", "french"))
            service.play(request, west)
        }) { fail() }

        // By now we should have a new hand dealt (should be deterministic), a new trick started and check for Centrifugo calls
        assertThat(newState.hands.size, equalTo(2))
        assertThat(newState.tricks.size, equalTo(2))

        assertThat(newState.tricks[1].cards().size, equalTo(4)) // all welcome cards
        assertThat(newState.hands[1].trump, equalTo(Trump.FREESTYLE))
        assertThat(newState.hands[1].startingPosition, equalTo(Position.NORTH)) // see game state above start = ...
        assertThat(newState.hands[1].gschobe, equalTo(Gschobe.NO))
        assertThat(Position.entries.flatMap { newState.hands[1].weiseOf(it) }.size, equalTo(0))

        assertThat(newState.tricks[0].cards().size, equalTo(0)) // no cards played in new trick yet
        assertThat(newState.hands[0].trump, equalTo(Trump.NONE))
        assertThat(newState.hands[0].startingPosition, equalTo(Position.EAST)) // comes next after NORTH
        assertThat(newState.hands[0].gschobe, equalTo(Gschobe.NOT_YET))
        assertThat(Position.entries.flatMap { newState.hands[0].cardsOf(it) }.size, equalTo(36)) // all cards are dealt
        assertThat(Position.entries.flatMap { newState.hands[0].weiseOf(it) }.size, equalTo(0))

        return newState
    }

    private fun checkWrongStartPlayer(state: GameState) {
        // We start off with a bang, it's not souths turn so this should not be possible
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "SIX", "french"))
            service.play(request, south)
        }) { assertTrue(it is PlayerIsLocked) }
    }
}
