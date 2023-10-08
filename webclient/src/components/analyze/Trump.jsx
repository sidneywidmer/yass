import React from "react"

const Trump = ({trump}) => {
    let emoji = ""

    switch (trump) {
        case "SPADES":
            emoji = "♠️"
            break
        case "HEARTS":
            emoji = "❤️"
            break
        case "DIAMONDS":
            emoji = "♦️"
            break
        case "CLUBS":
            emoji = "♣️"
            break
        case "FREESTYLE":
            emoji = "🆓"
            break
        default:
            emoji = trump
    }

    return <span>{emoji}</span>
}

export default Trump