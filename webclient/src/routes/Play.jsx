import React from 'react';
import {useParams} from 'react-router-dom';
import {fetchPlayGame, useFetchData} from "../helpers/api.jsx";
import Seat from "../components/play/Seat.jsx";
import {Grid} from "@mui/material";
import Container from "@mui/material/Container";

const Play = (params) => {
    let {gameCode} = useParams();
    const data = useFetchData(() => fetchPlayGame(gameCode), [gameCode]);

    if (!data) {
        return null;
    }

    return (
        <Container maxWidth="70%" style={{padding: '30px'}}>
            <Grid container>
                <Grid item xs={4}/>
                <Grid item xs={4} className="seat"> <Seat seat={data.seats["NORTH"]}/> </Grid>
                <Grid item xs={4}/>


                <Grid item xs={4} className="seat"><Seat seat={data.seats["WEST"]}/></Grid>
                <Grid item xs={4}/>
                <Grid item xs={4} className="seat"><Seat seat={data.seats["EAST"]}/></Grid>


                <Grid item xs={4}/>
                <Grid item xs={4} className="seat"><Seat seat={data.seats["SOUTH"]}/></Grid>
                <Grid item xs={4}/>
            </Grid>
        </Container>
    );
}

export default Play;