import React, {useState, useEffect} from 'react';
import axios from 'axios';
import {useParams} from 'react-router-dom';

function Analyze(params) {
    let {gameCode} = useParams();

    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        axios.get("http://127.0.0.1:8080/admin/analyze/game/" + gameCode, {withCredentials: true})
            .then((response) => {
                setData(response.data);
                setLoading(false);
            })
            .catch((error) => {
                setError(error);
                setLoading(false);
            });
    }, []);

    return (
        <div>
            <h1>Data Fetching Example</h1>
            {loading ? (
                <p>Loading...</p>
            ) : error ? (
                <p>Error: {error.message}</p>
            ) : (
                <pre>{JSON.stringify(data, null, 2)}</pre>
            )}
        </div>
    );
}

export default Analyze;