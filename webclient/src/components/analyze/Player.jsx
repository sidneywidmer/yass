import React from 'react';
import {Grid, Typography} from '@mui/material';
import Position from "./Position.jsx";
import Card from "./Card.jsx";

const Player = ({player}) => {
    return (
        <div>
            <Grid container spacing={2} alignItems="center">
                <Grid item xs={4}>
                    <Typography variant="body1" display="inline">
                        <Position position={player.position}/> {player.name}:
                    </Typography>
                </Grid>
                <Grid item xs={8}>
                    {player.cards.map((card, cardIndex) => (
                        <Card key={cardIndex} card={card}/>
                    ))}
                </Grid>
            </Grid>
        </div>
    );
}

export default Player;