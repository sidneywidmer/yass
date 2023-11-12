import React from 'react';
import {useParams} from 'react-router-dom';
import {fetchAnalyzeGame, useApi} from "../helpers/api.jsx";
import {Accordion, AccordionDetails, AccordionSummary, Chip, Divider, Typography} from "@mui/material";
import Player from "../components/analyze/Player.jsx";
import Trick from "../components/analyze/Trick.jsx";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import Trump from "../components/common/Trump.jsx";
import Gschobe from "../components/analyze/Gschobe.jsx";
import Container from "@mui/material/Container";

const Analyze = (params) => {
    let {gameCode} = useParams();
    const data = useApi(() => fetchAnalyzeGame(gameCode), [gameCode]);

    if (!data) {
        return null;
    }

    return (
        <Container maxWidth="md">
            <h1>Analyze Game {gameCode}</h1>
            <h2>NS: {data.points.NORTH + data.points.SOUTH}, WE: {data.points.EAST + data.points.WEST}</h2>
            {data.hands.map((hand, index) => (
                <Accordion key={index}>
                    <AccordionSummary expandIcon={<ExpandMoreIcon/>}>
                        <Typography sx={{width: '33%', flexShrink: 0}}>
                            Hand {index + 1}
                        </Typography>
                        <Typography sx={{width: '33%', color: 'text.secondary'}}>
                            Lead: {hand.startingPlayer.name},
                            Trump: <Trump trump={hand.trump}/>
                        </Typography>
                        <Typography sx={{color: 'text.secondary'}}>
                            NS: {hand.points.NORTH + hand.points.SOUTH},
                            WE: {hand.points.EAST + hand.points.WEST},
                        </Typography>
                        <Gschobe gschobe={hand.gschobe}/>
                    </AccordionSummary>
                    <AccordionDetails>
                        {hand.players.map((player, playerIndex) => (
                            <Player key={playerIndex} player={player}/>
                        ))}
                        <div style={{margin: '20px 0'}}>
                            <Divider>
                                <Chip label="Tricks"/>
                            </Divider>
                        </div>
                        {hand.tricks.map((trick, trickIndex) => (
                            <Trick key={trickIndex} trick={trick}/>
                        ))}
                    </AccordionDetails>
                </Accordion>
            ))}
        </Container>
    );
}

export default Analyze;