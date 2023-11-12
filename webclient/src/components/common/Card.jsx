import React, {forwardRef} from 'react';
import {Tooltip} from "@mui/material";


const Card = forwardRef((props, ref) => {
    const {card, styles, clickHandler, ...other} = props;

    const handler = clickHandler || (() => {
    });

    if (!card) {
        return <span ref={ref} className="card-container">--</span>
    }

    const baseClass = 'card-container';
    const cardClasses = styles ? [baseClass, ...styles].join(' ') : baseClass;

    const cardEmojis = {
        suit: {
            SPADES: "♠️",
            HEARTS: "❤️",
            DIAMONDS: "♦️",
            CLUBS: "♣️",
            WELCOME: "W",
        },
        rank: {
            ONE: "1",
            TWO: "2",
            THREE: "3",
            FOUR: "4",
            FIVE: "5",
            SIX: "6",
            SEVEN: "7",
            EIGHT: "8",
            NINE: "9",
            TEN: "10",
            JACK: "J",
            QUEEN: "Q",
            KING: "K",
            ACE: "A",
            HELLO: "H",
        },
    };

    const {suit, rank} = card;
    const suitEmoji = cardEmojis.suit[suit] || suit;
    const rankEmoji = cardEmojis.rank[rank] || rank;

    return <span {...other} ref={ref} className={cardClasses}
                 onClick={() => handler(card)}>{suitEmoji}{rankEmoji}</span>
})

export default Card