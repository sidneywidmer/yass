class Points {
    constructor(north, east, south, west) {
        this.NORTH = north;
        this.EAST = east;
        this.SOUTH = south;
        this.WEST = west;
    }
}

class Player {
    constructor(id, uuid, name, bot, createdAt, updatedAt) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.bot = bot;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

class PlayerWithCards {
    constructor(uuid, name, cards, position) {
        this.uuid = uuid;
        this.name = name;
        this.cards = cards; // TODO: map
        this.position = position
    }
}

class Card {
    constructor(suit, rank, skin) {
        this.suit = suit;
        this.rank = rank;
        this.skin = skin;
    }
}

class TrickCard {
    constructor(player, card) {
        this.player = new Player(player.id, player.uuid, player.name, player.bot, player.createdAt, player.updatedAt);
        this.card = card ? new Card(card.suit, card.rank, card.skin) : null;
    }
}

class Trick {
    constructor(cards, leadPlayer, leadSuit, winnerPlayer) {
        this.cards = cards.map((trickCard) => new TrickCard(trickCard.player, trickCard.card));
        this.leadPlayer = new Player(leadPlayer.id, leadPlayer.uuid, leadPlayer.name, leadPlayer.bot, leadPlayer.createdAt, leadPlayer.updatedAt);
        this.leadSuit = leadSuit;
        if (winnerPlayer === null) {
            this.winnerPlayer = null;
        } else {
            this.winnerPlayer = new Player(winnerPlayer.id, winnerPlayer.uuid, winnerPlayer.name, winnerPlayer.bot, winnerPlayer.createdAt, winnerPlayer.updatedAt);
        }
    }
}

class Hand {
    constructor(trump, gschobe, startingPlayer, players, tricks, points) {
        this.trump = trump;
        this.gschobe = gschobe;
        this.startingPlayer = new Player(startingPlayer.id, startingPlayer.uuid, startingPlayer.name, startingPlayer.bot, startingPlayer.createdAt, startingPlayer.updatedAt);
        this.players = players.map((player) => new PlayerWithCards(player.uuid, player.name, player.cards, player.position));
        this.tricks = tricks.map((trick) => new Trick(trick.cards, trick.leadPlayer, trick.leadSuit, trick.winnerPlayer));
        this.points = new Points(points.NORTH, points.EAST, points.SOUTH, points.WEST)
    }
}

class AnalyzeGame {
    constructor(hands, points, gameUuid) {
        this.gameUuid = gameUuid
        this.points = points
        this.hands = hands.map((hand) => new Hand(hand.trump, hand.gschobe, hand.startingPlayer, hand.players, hand.tricks, hand.points));
    }
}

export default AnalyzeGame;