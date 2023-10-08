import React from 'react';
import {Typography} from '@mui/material';
import Card from "./Card.jsx";
import Trump from "./Trump.jsx";

const Trick = ({trick}) => {
    return (
        <div>
            <Typography variant="body2" display="inline">{trick.leadPlayer.name}
                <Trump trump={trick.leadSuit}/>
            </Typography>
            {trick.cards.map((trickCard, trickCardIndex) => (
                <Card key={trickCardIndex} card={trickCard.card} player={trickCard.player}
                      isWinner={trick.winnerPlayer.id === trickCard.player.id}/>
            ))}
        </div>
    );
}

export default Trick;