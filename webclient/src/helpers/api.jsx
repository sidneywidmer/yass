import axios from "axios";
import {useLoading} from "../contexts/Loading.jsx";
import {useError} from "../contexts/Error.jsx";
import {useEffect, useState} from "react";
import AnalyzeGame from "./api/analyze-game.js";
import PlayGame from "./api/play-game.js";

export const useApi = (fetchFunction, dependencies) => {
    const {startLoading, stopLoading} = useLoading()
    const {handleError} = useError()
    const [fetchedData, setFetchedData] = useState(null)

    useEffect(() => {
        startLoading()

        fetchFunction()
            .then((data) => {
                setFetchedData(data)
            })
            .catch((error) => {
                handleError(error)
            })
            .finally(() => {
                stopLoading()
            });
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
    return new PlayGame(result.gameUuid, result.seats)
};

export const playCard = async (data) => {
    return await postData(`game/play`, data)
};