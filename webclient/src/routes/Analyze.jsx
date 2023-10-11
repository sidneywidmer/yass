import React from 'react';
import {useParams} from 'react-router-dom';
import {fetchGame, useFetchData} from "../helpers/api.jsx";
import {Accordion, AccordionDetails, AccordionSummary, Chip, Divider, Typography} from "@mui/material";
import Player from "../components/analyze/Player.jsx";
import Trick from "../components/analyze/Trick.jsx";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import Trump from "../components/analyze/Trump.jsx";
import Gschobe from "../components/analyze/Gschobe.jsx";

const Analyze = (params) => {
    let {gameCode} = useParams();
    const data = useFetchData(() => fetchGame(gameCode), [gameCode]);

    if (!data) {
        return null;
    }

    return (
        <div>
            <h1>Analyze Game {gameCode}</h1>
            {data.hands.map((hand, index) => (
                <Accordion key={index}>
                    <AccordionSummary expandIcon={<ExpandMoreIcon/>}>
                        <Typography sx={{width: '33%', flexShrink: 0}}>
                            Hand {index + 1}
                        </Typography>
                        <Typography sx={{color: 'text.secondary'}}>
                            Lead: {hand.startingPlayer.name},
                            Trump: <Trump trump={hand.trump}/>
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
        </div>
    );
}

export default Analyze;