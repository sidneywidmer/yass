import {useError} from "../contexts/Error.jsx";
import {useEffect, useState} from "react";
import {Alert, Snackbar} from "@mui/material";

const GlobalErrorHandling = () => {
    const {error, clearError} = useError();
    const [open, setOpen] = useState(false)

    useEffect(() => {
        if (error) {
            console.error('--> An error occurred:', error)
            setOpen(true)
        }
    }, [error])

    return (
        <Snackbar open={open} autoHideDuration={6000}>
            <Alert severity="error" sx={{width: '100%'}}>
                {`${error}`}
            </Alert>
        </Snackbar>
    )

}

export default GlobalErrorHandling