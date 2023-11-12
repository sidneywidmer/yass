import React from "react"

const Trump = ({trump}) => {
    const trumpEmojis = {
        SPADES: "♠️",
        HEARTS: "❤️",
        DIAMONDS: "♦️",
        CLUBS: "♣️",
        UNEUFE: "⬆️️",
        OBEABE: "⬇️️",
        FREESTYLE: "🆓",
    };

    let emoji = trumpEmojis[trump] || trump;

    return <span>{emoji}</span>
}

export default Trump