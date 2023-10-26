import axios from "axios";
import {useLoading} from "../contexts/Loading.jsx";
import {useError} from "../contexts/Error.jsx";
import {useEffect, useState} from "react";
import AnalyzeGame from './api/fetch-game/dto';

export const useFetchData = (fetchFunction, dependencies) => {
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

const fetchData = async (url) => {
    const response = await axios.get(`http://127.0.0.1:8080/${url}`, {withCredentials: true})
    return response.data

};

export const fetchGame = async (gameCode) => {
    const result = await fetchData(`admin/analyze/game/${gameCode}`)
    return new AnalyzeGame(result.hands, result.points, result.gameUuid)
};