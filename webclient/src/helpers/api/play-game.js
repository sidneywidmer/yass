class Card {
    constructor(suit, rank, skin) {
        this.suit = suit;
        this.rank = rank;
        this.skin = skin;
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
        this.cards = cards.map(card => new Card(card.suit, card.rank, card.skin));
        this.position = position;
        this.player = new Player(player.id, player.uuid, player.name, player.bot, player.createdAt, player.updatedAt);
    }
}

class PlayGame {
    constructor(gameUuid, seats) {
        this.gameUuid = gameUuid;
        this.seats = seats.reduce((map, seat) => {
            map[seat.position] = new Seat(seat.uuid, seat.cards, seat.position, seat.player);
            return map;
        }, {});
    }
}

export default PlayGame;