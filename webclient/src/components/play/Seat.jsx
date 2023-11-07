import React, {useEffect} from 'react';
import {Centrifuge} from "centrifuge";
import Card from "../common/Card.jsx";
import {chooseTrump, playCard, schiebe} from "../../helpers/api.jsx";
import Trump from "../common/Trump.jsx";

const Seat = ({seat, gameUuid, cardsPlayed, setCardsPlayed}) => {
    const onCardClick = (card) => {
        const data = {
            "game": gameUuid,
            "card": card,
            "data": {"impersonate": seat.player.uuid}
        }
        playCard(data)
            .then(responseData => {
                console.log('Response Data:', responseData);
            })
            .catch(error => {
                console.error('Error:', error);
            });
    }

    const onTrumpClick = (trump) => {
        const data = {
            "game": gameUuid,
            "trump": trump,
            "data": {"impersonate": seat.player.uuid}
        }
        chooseTrump(data)
            .then(responseData => {
                console.log('Response Data:', responseData);
            })
            .catch(error => {
                console.error('Error:', error);
            });
    }

    const onSchiebeClick = (gschobe) => {
        const data = {
            "game": gameUuid,
            "gschobe": gschobe,
            "data": {"impersonate": seat.player.uuid}
        }
        schiebe(data)
            .then(responseData => {
                console.log('Response Data:', responseData);
            })
            .catch(error => {
                console.error('Error:', error);
            });
    }

    const trumps = [
        "SPADES",
        "HEARTS",
        "DIAMONDS",
        "CLUBS",
        "UNEUFE",
        "OBEABE",
        "FREESTYLE"
    ];

    const channelCardPlayed = (action) => {
        const newCard = {
            ...action.card,
            "position": action.position
        }
        if (!cardsPlayed.some(card => card.suit === newCard.suit && card.rank === newCard.rank)) {
            console.log(cardsPlayed, newCard);
            setCardsPlayed([...cardsPlayed, newCard]);
        }
    }

    useEffect(() => {
        const data = {"data": {"impersonate": seat.player.uuid}}
        const actionMap = {
            "CardPlayed": channelCardPlayed,
        };

        const centrifuge = new Centrifuge('ws://127.0.0.1:8000/connection/websocket', data);
        centrifuge.connect();
        centrifuge.on('connected', function (ctx) {
            console.log('Connected to centrifuge', ctx)
        });
        centrifuge.on('disconnected', function (ctx) {
            console.log('Disconnected from centrifuge', ctx)
        });

        // Code to run on the first render
        const sub = centrifuge.newSubscription('seat:#' + seat.uuid, data);
        sub.on('publication', function (ctx) {
            console.log("New publication in game channel: ", ctx);
            ctx.data.forEach(action => {
                const executeFunction = actionMap[action.type];
                if (executeFunction) {
                    executeFunction(action);
                }
            });
        });
        sub.on('subscribed', function (ctx) {
            console.log("Now subscribed to game channel: ", ctx);
        });
        sub.on('unsubscribed', function (ctx) {
            console.log("Now unsubscribed to game channel: ", ctx);
        });
        sub.on('error', function (ctx) {
            console.log("Game channel error: ", ctx);
        });
        sub.subscribe();
    }, []);

    return (
        <>
            Position: {seat.position} <br/>
            {seat.cards.map((trickCard, trickCardIndex) => (
                <Card key={trickCardIndex} styles={trickCard.locked ? ["clickable", "locked"] : ["clickable"]}
                      card={trickCard} clickHandler={onCardClick}/>
            ))}
            <br/><br/>
            <span className={"card-container clickable"} onClick={() => onSchiebeClick("YES")}>Y</span>
            <span className={"card-container clickable"} onClick={() => onSchiebeClick("NO")}>N</span>
            <br/>
            {trumps.map((trump, index) => (
                <div key={index} className={"card-container clickable"} onClick={() => onTrumpClick(trump)}>
                    <Trump trump={trump}/>
                </div>
            ))}

        </>
    );
}

export default Seat;
