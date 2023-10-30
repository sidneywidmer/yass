import React, {useEffect} from 'react';
import {Centrifuge} from "centrifuge";
import Card from "../common/Card.jsx";
import {Box} from "@mui/material";

const Seat = ({seat}) => {
    useEffect(() => {
        const data = {"data": {"impersonate": seat.player.uuid}}
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
            Seat: {seat.uuid} <br/>
            Position: {seat.position} <br/>
            {seat.cards.map((trickCard, trickCardIndex) => (
                <Card key={trickCardIndex} card={trickCard}/>
            ))}
        </>
    );
}

export default Seat;
