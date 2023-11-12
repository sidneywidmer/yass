import React from 'react';
import {Grid, Tooltip, Typography} from '@mui/material';
import Trump from "../common/Trump.jsx";
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
                        <Tooltip title={trickCard.player.name} placement="top">
                            <Card key={trickCardIndex} card={trickCard.card}
                                  styles={trick.winnerPlayer.id === trickCard.player.id ? ['winner'] : []}/>
                        </Tooltip>
                    ))}
                </Grid>
            </Grid>
        </div>
    );
}

export default Trick;