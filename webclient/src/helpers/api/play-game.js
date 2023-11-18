class CardInHand {
    constructor(suit, rank, skin, state) {
        this.suit = suit;
        this.rank = rank;
        this.skin = skin;
        this.state = state;
    }
}

class CardOnTable {
    constructor(suit, rank, skin, position) {
        this.suit = suit;
        this.rank = rank;
        this.skin = skin;
        this.position = position;
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

class Seat {
    constructor(uuid, cards, position, player, points, state, activePosition, trump) {
        this.uuid = uuid;
        this.cards = cards.map(card => new CardInHand(card.suit, card.rank, card.skin, card.state));
        this.position = position;
        this.player = new Player(player.id, player.uuid, player.name, player.bot, player.createdAt, player.updatedAt);
        this.points = points;
        this.state = state;
        this.activePosition = activePosition;
        this.trump = trump;
    }
}

class PlayGame {
    constructor(gameUuid, seats, cardsPlayed) {
        this.gameUuid = gameUuid;
        this.cardsPlayed = cardsPlayed.map(card => new CardOnTable(card.suit, card.rank, card.skin, card.position));
        this.seats = seats.reduce((map, seat) => {
            map[seat.position] = new Seat(seat.uuid, seat.cards, seat.position, seat.player, seat.points, seat.state, seat.activePosition, seat.trump);
            return map;
        }, {});
    }
}

export default PlayGame;