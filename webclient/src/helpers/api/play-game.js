class CardInHand {
    constructor(suit, rank, skin, locked) {
        this.suit = suit;
        this.rank = rank;
        this.skin = skin;
        this.locked = locked;
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
    constructor(uuid, cards, position, player) {
        this.uuid = uuid;
        this.cards = cards.map(card => new CardInHand(card.suit, card.rank, card.skin, card.locked));
        this.position = position;
        this.player = new Player(player.id, player.uuid, player.name, player.bot, player.createdAt, player.updatedAt);
    }
}

class PlayGame {
    constructor(gameUuid, seats, cardsPlayed) {
        this.gameUuid = gameUuid;
        this.cardsPlayed = cardsPlayed.map(card => new CardOnTable(card.suit, card.rank, card.skin, card.position));
        this.seats = seats.reduce((map, seat) => {
            map[seat.position] = new Seat(seat.uuid, seat.cards, seat.position, seat.player);
            return map;
        }, {});
    }
}

export default PlayGame;