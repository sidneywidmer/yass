import React, {useEffect, useState} from 'react';
import {Centrifuge} from "centrifuge";
import Card from "../common/Card.jsx";
import {api, chooseTrump, playCard, schiebe} from "../../helpers/api.jsx";
import Trump from "../common/Trump.jsx";
import useGameStore from "../../store/global.jsx";

const Seat = ({seat, gameUuid, cardsPlayed, setCardsPlayed}) => {
    const [isConnected, setIsConnected] = useState(false);
    const [actions, setActions] = useState([]);
    const [cardsInHand, setCardsInHand] = useState(seat.cards);
    const [points, setPoints] = useState(seat.points);
    const [state, setState] = useState(seat.state);
    const [activePosition, setActivePosition] = useState(seat.activePosition);
    const [trump, setTrump] = useState(seat.trump);

    const onCardClick = (card) => {
        const data = {
            "game": gameUuid,
            "card": card,
            "data": {"impersonate": seat.player.uuid}
        }
        api(() => playCard(data));
    }

    const onTrumpClick = (trump) => {
        const data = {
            "game": gameUuid,
            "trump": trump,
            "data": {"impersonate": seat.player.uuid}
        }
        api(() => chooseTrump(data));
    }

    const onSchiebeClick = (gschobe) => {
        const data = {
            "game": gameUuid,
            "gschobe": gschobe,
            "data": {"impersonate": seat.player.uuid}
        }
        api(() => schiebe(data));
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

    const cardPlayed = (action) => {
        // All positions get this event but only NORTH has access to this state otherwise
        // each played card would be displayed 4 times
        if (setCardsPlayed !== undefined) {
            setCardsPlayed([...cardsPlayed, action.card]);
        }
    };

    const updateHand = (action) => {
        setCardsInHand(action.cards);
    };

    const updateState = (action) => {
        setState(action.state);
    };

    const updateActive = (action) => {
        setActivePosition(action.position);
    };

    const updatePoints = (action) => {
        setPoints(action.points);
        if (setCardsPlayed !== undefined) {
            useGameStore.getState().addMessage({severity: "info", message: "Points updated!"});
        }
    };

    const updatePlayedCards = (action) => {
        if (setCardsPlayed !== undefined) {
            setCardsPlayed(action.cards);
        }
    };

    const updateTrump = (action) => {
        setTrump(action.trump);
    };

    const message = (action) => {
        if (setCardsPlayed !== undefined) {
            useGameStore.getState().addMessage({severity: "info", message: action.message});
        }
    };

    useEffect(() => {
        const data = {data: {impersonate: seat.player.uuid}};

        const centrifuge = new Centrifuge('ws://127.0.0.1:8000/connection/websocket', data);
        centrifuge.connect();

        centrifuge.on('connected', function (ctx) {
            console.log("connected ", ctx);
            setIsConnected(true);
        });
        centrifuge.on('disconnected', function (ctx) {
            setIsConnected(false);
        });


        const sub = centrifuge.newSubscription('seat:#' + seat.uuid, data);
        sub.on('subscribed', function (ctx) {
            console.log('subscribed ', ctx);
        });
        sub.subscribe();


        sub.on('publication', function (ctx) {
            setActions(previous => [ctx, ...previous]);

        });

        return () => {
            centrifuge.disconnect();
            sub.unsubscribe();
        };
    }, [seat.uuid]);

    useEffect(() => {
        let timeoutId;

        const debouncedEffect = () => {
            if (actions.length === 0) {
                return;
            }

            const actionMap = {
                CardPlayed: cardPlayed,
                UpdateHand: updateHand,
                UpdatePoints: updatePoints,
                UpdateState: updateState,
                UpdateActive: updateActive,
                UpdatePlayedCards: updatePlayedCards,
                UpdateTrump: updateTrump,
                Message: message,
            };

            actions.shift().data.forEach((action) => {
                const executeFunction = actionMap[action.type];
                if (executeFunction) {
                    if (setCardsPlayed !== undefined) {
                        console.log(seat.uuid, action.type)
                    }
                    executeFunction(action);
                }
            });
        };

        // Set up the debounced effect
        timeoutId = setTimeout(debouncedEffect, 100); // Adjust the delay as needed (e.g., 500ms)

        // Cleanup: Clear the timeout when the component unmounts or when the dependency changes
        return () => {
            clearTimeout(timeoutId);
        };
    }, [actions]);


    const getCardStyles = (cardInHand, activePosition, seat, state) => {
        const baseStyles = ["clickable"];

        if (cardInHand.state === "UNPLAYABLE" || activePosition !== seat.position || state !== "PLAY_CARD") {
            baseStyles.push("locked");
        }

        if (cardInHand.state === "ALREADY_PLAYED") {
            baseStyles.push("already-played");
        }

        return baseStyles;
    };

    return (
        <div>
            Points: {points[seat.position]}, <Trump trump={trump}/><br/>
            Active: {activePosition}, State: {state}<br/>
            {cardsInHand.map((cardInHand, cardInHandIndex) => (
                <Card key={cardInHandIndex}
                      styles={getCardStyles(cardInHand, activePosition, seat, state)}
                      card={cardInHand}
                      clickHandler={onCardClick}/>
            ))}
            <br/><br/>
            <span
                className={`card-container clickable ${activePosition === seat.position && state === "SCHIEBE" ? "" : "locked"}`}
                onClick={() => onSchiebeClick("YES")}>Y</span>
            <span
                className={`card-container clickable ${activePosition === seat.position && state === "SCHIEBE" ? "" : "locked"}`}
                onClick={() => onSchiebeClick("NO")}>N</span>
            <br/>
            {trumps.map((trump, index) => (
                <div key={index}
                     className={`card-container clickable ${activePosition === seat.position && state === "TRUMP" ? "" : "locked"}`}
                     onClick={() => onTrumpClick(trump)}>
                    <Trump trump={trump}/>
                </div>
            ))}
        </div>
    );
}

export default Seat;
