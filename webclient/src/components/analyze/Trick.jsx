import React from 'react';
import {Grid, Typography} from '@mui/material';
import Trump from "./Trump.jsx";
import Card from "../common/Card.jsx";

const Trick = ({trick}) => {
    return (
        <div>
            <Grid container spacing={2} alignItems="center">
                <Grid item xs={2}>
                    <Typography variant="body2" display="inline">
                        {trick.leadPlayer.name} <Trump trump={trick.leadSuit}/>
                    </Typography>
                </Grid>
                <Grid item xs={10}>
                    {trick.cards.map((trickCard, trickCardIndex) => (
                        <Card key={trickCardIndex} card={trickCard.card} player={trickCard.player}
                              isWinner={trick.winnerPlayer.id === trickCard.player.id}/>
                    ))}
                </Grid>
            </Grid>
        </div>
    );
}

export default Trick;