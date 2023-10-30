import React from "react"
import {Tooltip} from "@mui/material";


const Card = ({card, player, isWinner}) => {
    if (!card) {
        return <span className="card-container">--</span>
    }

    const {suit, rank} = card
    let suitEmoji = ""
    let rankEmoji = ""

    switch (suit) {
        case "SPADES":
            suitEmoji = "♠️"
            break
        case "HEARTS":
            suitEmoji = "❤️"
            break
        case "DIAMONDS":
            suitEmoji = "♦️"
            break
        case "CLUBS":
            suitEmoji = "♣️"
            break
        case "WELCOME":
            suitEmoji = "W"
            break
        default:
            suitEmoji = suit
    }

    switch (rank) {
        case "ONE":
            rankEmoji = "1"
            break
        case "TWO":
            rankEmoji = "2"
            break
        case "THREE":
            rankEmoji = "3"
            break
        case "FOUR":
            rankEmoji = "4"
            break
        case "FIVE":
            rankEmoji = "5"
            break
        case "SIX":
            rankEmoji = "6"
            break
        case "SEVEN":
            rankEmoji = "7"
            break
        case "EIGHT":
            rankEmoji = "8"
            break
        case "NINE":
            rankEmoji = "9"
            break
        case "TEN":
            rankEmoji = "10"
            break
        case "JACK":
            rankEmoji = "J"
            break
        case "QUEEN":
            rankEmoji = "Q"
            break
        case "KING":
            rankEmoji = "K"
            break
        case "ACE":
            rankEmoji = "A"
            break
        case "HELLO":
            rankEmoji = "H"
            break
        default:
            rankEmoji = rank
    }

    if (!player) {
        return <span className="card-container">{suitEmoji}{rankEmoji}</span>
    }

    return (
        <Tooltip title={player.name} placement="top">
            <span className="card-container" style={isWinner ? {"backgroundColor": "#fff4bd"} : {}}>
                {suitEmoji}{rankEmoji}
            </span>
        </Tooltip>
    )
}

export default Card