import React, {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {fetchPlayGame, useApi} from "../helpers/api.jsx";
import Seat from "../components/play/Seat.jsx";
import {Grid, Tooltip} from "@mui/material";
import Container from "@mui/material/Container";
import Card from "../components/common/Card.jsx";

const Play = (params) => {
    let {gameCode} = useParams();

    const [cardsPlayed, setCardsPlayed] = useState([]);
    const data = useApi(() => fetchPlayGame(gameCode), [gameCode]);

    useEffect(() => {
        if (data) {
            setCardsPlayed(data.cardsPlayed);
        }
    }, [data])

    if (!data) {
        return null;
    }

    return (
        <Container maxWidth="70%" style={{padding: '30px'}}>
            <Grid container>
                <Grid item xs={4}/>
                <Grid item xs={4} className="seat"> <Seat seat={data.seats["NORTH"]} gameUuid={data.gameUuid}
                                                          setCardsPlayed={setCardsPlayed} cardsPlayed={cardsPlayed}/>
                </Grid>
                <Grid item xs={4}/>


                <Grid item xs={4} className="seat"><Seat seat={data.seats["WEST"]} gameUuid={data.gameUuid}
                                                         setCardsPlayed={setCardsPlayed}
                                                         cardsPlayed={cardsPlayed}/></Grid>
                <Grid item xs={4}>
                    {cardsPlayed.map((card, index) => (
                        <Tooltip title={card.position} key={index}>
                            <Card card={card}/>
                        </Tooltip>
                    ))}
                </Grid>
                <Grid item xs={4} className="seat"><Seat seat={data.seats["EAST"]} gameUuid={data.gameUuid}
                                                         setCardsPlayed={setCardsPlayed}
                                                         cardsPlayed={cardsPlayed}/></Grid>


                <Grid item xs={4}/>
                <Grid item xs={4} className="seat"><Seat seat={data.seats["SOUTH"]} gameUuid={data.gameUuid}
                                                         setCardsPlayed={setCardsPlayed}
                                                         cardsPlayed={cardsPlayed}/></Grid>
                <Grid item xs={4}/>
            </Grid>
        </Container>
    );
}

export default Play;