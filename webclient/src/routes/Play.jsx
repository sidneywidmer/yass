import React, {useEffect} from 'react';
import {useParams} from 'react-router-dom';
import {fetchGame, useFetchData} from "../helpers/api.jsx";
import {Centrifuge} from "centrifuge";

const Play = (params) => {
    let {gameCode} = useParams();
    const data = useFetchData(() => fetchGame(gameCode), [gameCode]);

    useEffect(() => {
        if (!data || !data.gameUuid) {
            return;
        }

        const centrifuge = new Centrifuge('ws://127.0.0.1:8000/connection/websocket');
        centrifuge.connect();
        centrifuge.on('connected', function (ctx) {
            console.log('Connected to centrifuge', ctx)
        });
        centrifuge.on('disconnected', function (ctx) {
            console.log('Disconnected from centrifuge', ctx)
        });

        // Code to run on the first render
        const sub = centrifuge.newSubscription('game:#' + data.gameUuid);
        sub.on('publication', function (ctx) {
            console.log("New publication in game channel: ", ctx);
        });
        sub.on('subscribed', function (ctx) {
            console.log("Now subscribed to game channel: ", ctx);
        });
        sub.on('error', function (ctx) {
            console.log("Game channel error: ", ctx);
        });
        sub.subscribe();
    }, [data]);

    if (!data) {
        return null;
    }

    return (
        <div>
            hello
        </div>
    );
}

export default Play;