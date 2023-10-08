import React from 'react';
import {Typography} from '@mui/material';
import Position from "./Position.jsx";
import Card from "./Card.jsx";

const Player = ({player}) => {
    return (
        <div>
            <Typography variant="body1" display="inline"><Position position={player.position}/> ({player.name}):</Typography>
            {player.cards.map((card, cardIndex) => (
                <Card key={cardIndex} card={card}/>
            ))}
        </div>
    );
}

export default Player;