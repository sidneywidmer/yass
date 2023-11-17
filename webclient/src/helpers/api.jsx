import axios from "axios";
import {useEffect, useState} from "react";
import AnalyzeGame from "./api/analyze-game.js";
import PlayGame from "./api/play-game.js";
import useGameStore from "../store/global.jsx";

const handleError = (error) => {
    if (error.response) {
        const status = error.response.status;
        switch (status) {
            case 401:
                useGameStore.getState().addMessage({severity: 'error', message: 'Unauthorized: Please log in'});
                break;
            case 500:
                const errorMessage = error.response.data.payload.domainError || 'Internal Server Error';
                useGameStore.getState().addMessage({severity: 'error', message: errorMessage});
                break;
            default:
                useGameStore.getState().addMessage({
                    severity: 'error',
                    message: `Unhandled Response Status: ${status}`
                });
        }
    } else if (error.request) {
        useGameStore.getState().addMessage({severity: 'error', message: 'No Response Received'});
    } else {
        useGameStore.getState().addMessage({severity: 'error', message: `Request Setup Error: ${error.message}`});
    }
}

export const api = (fetchFunction) => {
    return fetchFunction()
        .then((data) => {
            return data
        })
        .catch((error) => handleError(error));
};

export const useApi = (fetchFunction, dependencies) => {
    const [fetchedData, setFetchedData] = useState(null)

    useEffect(() => {
        fetchFunction()
            .then((data) => {
                setFetchedData(data)
            })
            .catch((error) => handleError(error));
    }, [...dependencies])

    return fetchedData
};

const getData = async (url) => {
    const response = await axios.get(`http://127.0.0.1:8080/${url}`, {withCredentials: true})
    return response.data
};

const postData = async (url, data) => {
    const response = await axios.post(`http://127.0.0.1:8080/${url}`, data, {
        withCredentials: true,
        headers: {'Content-Type': 'application/json'}
    });

    return response.data;
};

export const fetchAnalyzeGame = async (gameCode) => {
    const result = await getData(`admin/analyze/game/${gameCode}`)
    return new AnalyzeGame(result.hands, result.points, result.gameUuid)
};

export const fetchPlayGame = async (gameCode) => {
    const result = await getData(`admin/play/game/${gameCode}`)
    return new PlayGame(result.gameUuid, result.seats, result.cardsPlayed)
};

export const playCard = async (data) => {
    return await postData(`game/play`, data)
};

export const chooseTrump = async (data) => {
    return await postData(`game/trump`, data)
};

export const schiebe = async (data) => {
    return await postData(`game/schiebe`, data)
};