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
            hasPlayedCard(Suit.WELCOME, Rank.WELCOME) // Actually 4 of those where played
        }
        centrifugo.resetRequests()
    }

    private fun checkCentrifugoTrick1(state: GameState) {
        val north = positionSeat(Position.NORTH, state.seats)
        cth.assertActions(north.uuid, cth.parseActions(centrifugo.allServeEvents)).apply {
            hasTrump(Trump.SPADES)
            hasGschobe(Position.WEST)
            hasCount(UpdatePossibleWeise::class, 1)
            hasPlayedCard(Suit.SPADES, Rank.TEN)
            hasPlayedCard(Suit.SPADES, Rank.EIGHT)
            hasPlayedCard(Suit.HEARTS, Rank.SIX)
            hasPlayedCard(Suit.SPADES, Rank.QUEEN)
            hasWeis(WeisType.VIER_BLATT, 100)
            hasCount(ClearPlayedCards::class, 1)
        }
        centrifugo.resetRequests()
    }

    private fun checkPointsForHand2(state: GameState) {
        val points = pointsByPositionTotal(state.hands, state.tricks)
        assertThat(points.getValue(Position.NORTH).weisPoints, equalTo(40))
        assertThat(points.getValue(Position.WEST).weisPoints, equalTo(40))
        assertThat(points.getValue(Position.SOUTH).weisPoints, equalTo(140))
        assertThat(points.getValue(Position.EAST).weisPoints, equalTo(0))

        assertThat(points.getValue(Position.NORTH).cardPoints, equalTo(179)) // Only change versus points in hand 1
        assertThat(points.getValue(Position.WEST).cardPoints, equalTo(26))
        assertThat(points.getValue(Position.SOUTH).cardPoints, equalTo(64))
        assertThat(points.getValue(Position.EAST).cardPoints, equalTo(102))
    }

    private fun playHand2Trick1(state: GameState): GameState {
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)

        // WEST started the first hand, so it's now SOUTH's turn. They don't want to schiebe and choose UNEUFE trump
        recover({ service.schiebe(SchiebeRequest(state.game.uuid.toString(), "NO"), south) }) { fail() }
        recover({ service.trump(ChooseTrumpRequest(state.game.uuid.toString(), "UNEUFE"), south) }) { fail() }

        // SOUTH plays the first card in the first trick of the second hand
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "EIGHT"))
            service.play(request, south)
        }) { fail() }

        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "NINE"))
            service.play(request, east)
        }) { fail() }

        // NORTH will win the trick, H6 is bock
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "SIX"))
            service.play(request, north)
        }) { fail() }

        val updatedState = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "SEVEN"))
            service.play(request, west)
        }) { fail() }

        assertThat(updatedState.game.status, equalTo(GameStatus.FINISHED))

        return updatedState
    }

    private fun checkPointsForHand1(state: GameState) {
        val points = pointsByPositionTotal(state.hands, state.tricks)
        assertThat(points.getValue(Position.NORTH).weisPoints, equalTo(40))
        assertThat(points.getValue(Position.WEST).weisPoints, equalTo(40))
        assertThat(points.getValue(Position.SOUTH).weisPoints, equalTo(140))
        assertThat(points.getValue(Position.EAST).weisPoints, equalTo(0))

        assertThat(points.getValue(Position.NORTH).cardPoints, equalTo(122))
        assertThat(points.getValue(Position.WEST).cardPoints, equalTo(26))
        assertThat(points.getValue(Position.SOUTH).cardPoints, equalTo(64))
        assertThat(points.getValue(Position.EAST).cardPoints, equalTo(102))
    }

    private fun playHand1Trick3To9(state: GameState): GameState {
        // Let's speed this up a little. After meticulously asserting everything for trick 1 and 2 we can
        // just let the hand be played out and check if our state is correct at the end.
        val cardsPlayed = listOf(
            // Trick3: E*->S->W->N
            PlayCard(Position.EAST, "SJ"),
            PlayCard(Position.NORTH, "H9"),
            PlayCard(Position.WEST, "S6"),
            PlayCard(Position.SOUTH, "D8"),

            // Trick4: E*->S->W->N
            PlayCard(Position.EAST, "S9"),
            PlayCard(Position.NORTH, "HJ"),
            PlayCard(Position.WEST, "S7"),
            PlayCard(Position.SOUTH, "H8"),

            // Trick5: E->S->W->N*
            PlayCard(Position.EAST, "HK"),
            PlayCard(Position.NORTH, "HA"),
            PlayCard(Position.WEST, "HQ"),
            PlayCard(Position.SOUTH, "D10"),

            // Trick6: E->S*->W->N
            PlayCard(Position.NORTH, "CQ"),
            PlayCard(Position.WEST, "CK"),
            PlayCard(Position.SOUTH, "CA"),
            PlayCard(Position.EAST, "C6"),

            // Trick7: E->S*->W->N
            PlayCard(Position.SOUTH, "DA"),
            PlayCard(Position.EAST, "C7"),
            PlayCard(Position.NORTH, "DQ"),
            PlayCard(Position.WEST, "D6"),

            // Trick8: E->S->W->N*
            PlayCard(Position.SOUTH, "DJ"),
            PlayCard(Position.EAST, "C8"),
            PlayCard(Position.NORTH, "DK"),
            PlayCard(Position.WEST, "D7"),

            // Trick9: E->S->W->N* (last stich)
            PlayCard(Position.NORTH, "H10"),
            PlayCard(Position.WEST, "CJ"),
            PlayCard(Position.SOUTH, "D9"),
            PlayCard(Position.EAST, "C10")
        )

        val uuid = state.game.uuid.toString()
        var newState: GameState = state
        cardsPlayed.forEach { c ->
            val player = playerAtPosition(c.position, newState.seats, newState.allPlayers)
            val card = interpretCard(c.card)!!
            newState = recover({
                val request = PlayCardRequest(uuid, PlayedCard(card.suit.name, card.rank.name))
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
        assertThat(points.getValue(Position.WEST).cardPoints, equalTo(26))
        assertThat(points.getValue(Position.WEST).weisPoints, equalTo(40)) // +40 Stoeck
        assertThat(points.getValue(Position.SOUTH).cardPoints, equalTo(0))
        assertThat(points.getValue(Position.SOUTH).weisPoints, equalTo(140))
        assertThat(points.getValue(Position.EAST).cardPoints, equalTo(30)) // +30 (King plus Ace)
        assertThat(points.getValue(Position.EAST).weisPoints, equalTo(0))
    }

    private fun playHand1Trick2(state: GameState): GameState {
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)

        // West won the last trick so they go again first and again start with SPADES which is also Stoeck
        val stateAfterStoeck = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "KING"))
            service.play(request, west)
        }) { fail() }

        assertTrue(stateAfterStoeck.hands[0].weiseOf(Position.WEST).any { it.type == WeisType.STOECK })

        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "SEVEN"))
            service.play(request, south)
        }) { fail() }

        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "ACE"))
            service.play(request, east)
        }) { fail() }

        val stateAfterAllCards = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("CLUBS", "NINE"))
            service.play(request, north)
        }) { fail() }

        assertThat(stateAfterAllCards.tricks.size, equalTo(4))

        return stateAfterAllCards
    }

    private fun checkPointsTrick1(state: GameState) {
        // All points are times 2 because trump is SPADES
        val points = pointsByPositionTotal(state.hands, state.tricks)
        assertThat(points.getValue(Position.NORTH).cardPoints, equalTo(0))
        assertThat(points.getValue(Position.NORTH).weisPoints, equalTo(40)) // DREI_BLATT
        assertThat(points.getValue(Position.WEST).cardPoints, equalTo(26)) // 10 and Queen
        assertThat(points.getValue(Position.WEST).weisPoints, equalTo(0))
        assertThat(points.getValue(Position.SOUTH).cardPoints, equalTo(0))
        assertThat(points.getValue(Position.SOUTH).weisPoints, equalTo(140)) // DREI_BLATT and VIER_BLATT
        assertThat(points.getValue(Position.EAST).cardPoints, equalTo(0))
        assertThat(points.getValue(Position.EAST).weisPoints, equalTo(0))
    }

    private fun prepareHand2() {
        val cardsN = interpretCards("H6,S7,C6,D9,H10,SJ,CQ,DK,SA")
        val cardsW = interpretCards("H7,S8,C9,D10,HJ,SQ,CK,DA,S6")
        val cardsS = interpretCards("H8,S9,C10,DJ,HQ,SK,CA,D6,S10")
        val cardsE = interpretCards("H9,S10,CJ,DQ,HK,HA,C8,D7,C7")

        foresight.pushDeck(cardsN + cardsW + cardsS + cardsE)
    }

    private fun prepareHand1() {
        // Has a DREI_BLATT for 20 Points (H9-HJ)
        val cardsN = interpretCards("H9,H10,HJ,HA,CQ,C9,DK,DQ,S10")
        // Will schiebe and has STOECK if west chooses SPADES as trump (fingers crossed)
        val cardsW = interpretCards("SQ,SK,S6,S7,CJ,CK,D6,D7,HQ")
        // Has a VIER_BLATT for 50 Points (D8-DJ) and a DREI_BLATT for 20 Points (H6-H8)
        val cardsS = interpretCards("D8,D9,D10,DJ,H6,H7,H8,DA,CA")
        // Has a DREI_BLATT for 20 Points (C6-C9)
        val cardsE = interpretCards("C6,C7,C8,C10,S8,S9,SJ,SA,HK")

        // Manipulate our luck a little
        foresight.pushDeck(cardsN + cardsW + cardsS + cardsE)
    }

    private fun playHand1Trick1(state: GameState): GameState {
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)

        // WEST is our start player, let's quickly make sure the state waits for schiebe and we can't just play a card
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "QUEEN"))
            service.play(request, west)
        }) { assertTrue(it is InvalidState && it.nextState == State.SCHIEBE) }

        // The correct player (west) now declares: yes, schiebe please (it's now EAST's turn to declare trump)
        val stateAfterSchiebe = recover({
            val request = SchiebeRequest(state.game.uuid.toString(), "YES")
            service.schiebe(request, west)
        }) { fail() }

        assertThat(stateAfterSchiebe.hands[0].trump, equalTo(Trump.NONE))
        assertThat(stateAfterSchiebe.hands[0].gschobe, equalTo(Gschobe.YES))

        val stateAfterTrump = recover({
            val request = ChooseTrumpRequest(stateAfterSchiebe.game.uuid.toString(), "SPADES")
            service.trump(request, east)
        }) { fail() }

        assertThat(stateAfterTrump.hands[0].trump, equalTo(Trump.SPADES))

        // West the first card of the hand (and the trick)
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "QUEEN"))
            service.play(request, west)
        }) { fail() }

        // Next up south, has no spades so they play a low hearts - but oh no! They should first declare their weis.
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "SIX"))
            service.play(request, south)
        }) { assertTrue(it is InvalidState && it.nextState == State.WEISEN_FIRST) }

        // Okay then, let's weis our VIER_BLATT for 50 Points (D9-DJ) first
        val stateAfterWeis = recover({
            val weis = Weis(WeisType.VIER_BLATT, interpretCards("D8,D9,D10,DJ"))
            val request = WeisenRequest(state.game.uuid.toString(), weis)
            service.weisen(request, south)
        }) { fail() }

        assertThat(stateAfterWeis.hands[0].weiseOf(Position.SOUTH).size, equalTo(1))
        assertThat(stateAfterWeis.hands[0].weiseOf(Position.SOUTH).first().type, equalTo(WeisType.VIER_BLATT))
        assertThat(stateAfterWeis.hands[0].weiseOf(Position.SOUTH).first().cards.size, equalTo(4))

        // Try again to play that darn H6, this time it should work
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "SIX"))
            service.play(request, south)
        }) { fail() }

        // East can also weis a DREI_BLATT...
        recover({
            val weis = Weis(WeisType.DREI_BLATT, interpretCards("C6,C7,C8"))
            val request = WeisenRequest(state.game.uuid.toString(), weis)
            service.weisen(request, east)
        }) { fail() }

        // ...and play a card. Can follow suit with S9
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "EIGHT"))
            service.play(request, east)
        }) { fail() }

        // Does anyone NOT have a weis these days? North shows their DREI_BLATT...
        recover({
            val weis = Weis(WeisType.DREI_BLATT, interpretCards("H9,H10,HJ"))
            val request = WeisenRequest(state.game.uuid.toString(), weis)
            service.weisen(request, north)
        }) { fail() }

        // ...and plays S10 because they must follow suit
        val stateAfterAllCards = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("SPADES", "TEN"))
            service.play(request, north)
        }) { fail() }

        // Now something special happened: The Team (N/S, E/W) with the most weise automatically weises any remaining
        // weise they might have. In our case SOUTH has another DREI_BLATT (H6-H8). Also, a new (empty) trick is initiated
        assertThat(stateAfterAllCards.tricks.size, equalTo(3)) // new trick...
        assertThat(stateAfterAllCards.tricks[0].cards().size, equalTo(0)) // ...that is also still empty

        assertThat(stateAfterAllCards.tricks[1].cards().size, equalTo(4))
        assertThat(stateAfterAllCards.hands[0].weiseOf(Position.NORTH).size, equalTo(1))
        assertThat(stateAfterAllCards.hands[0].weiseOf(Position.EAST).size, equalTo(1))
        assertThat(stateAfterAllCards.hands[0].weiseOf(Position.SOUTH).size, equalTo(2))
        assertThat(stateAfterAllCards.hands[0].weiseOf(Position.WEST).size, equalTo(0))
        assertTrue(stateAfterAllCards.hands[0].weiseOf(Position.SOUTH).any { it.type == WeisType.DREI_BLATT })
        assertTrue(stateAfterAllCards.hands[0].weiseOf(Position.SOUTH).any { it.type == WeisType.VIER_BLATT })

        return stateAfterAllCards
    }

    private fun playWelcomeHand(state: GameState): GameState {
        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val west = playerAtPosition(Position.WEST, state.seats, state.allPlayers)
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        val east = playerAtPosition(Position.EAST, state.seats, state.allPlayers)

        // NORTH plays the first card
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "WELCOME"))
            service.play(request, north)
        }) { fail() }

        // Next up WEST
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "WELCOME"))
            service.play(request, west)
        }) { fail() }

        // SOUTH
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "WELCOME"))
            service.play(request, south)
        }) { fail() }

        // EAST
        val newState = recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "WELCOME"))
            service.play(request, east)
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
        assertThat(newState.hands[0].startingPosition, equalTo(Position.WEST)) // comes next after NORTH
        assertThat(newState.hands[0].gschobe, equalTo(Gschobe.NOT_YET))
        assertThat(Position.entries.flatMap { newState.hands[0].cardsOf(it) }.size, equalTo(36)) // all cards are dealt
        assertThat(Position.entries.flatMap { newState.hands[0].weiseOf(it) }.size, equalTo(0))

        return newState
    }

    private fun checkWrongStartPlayer(state: GameState) {
        // We start off with a bang, it's not souths turn so this should not be possible
        val south = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        recover({
            val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "SIX"))
            service.play(request, south)
        }) { assertTrue(it is PlayerIsLocked) }
    }
}
