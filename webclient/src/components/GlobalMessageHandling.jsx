import React, {useEffect, useState} from 'react';
import {IconButton, Snackbar} from '@mui/material';
import MuiAlert from '@mui/material/Alert';
import CloseIcon from '@mui/icons-material/Close';
import useGameStore from "../store/global.jsx"; // Adjust the path

const GlobalMessageHandling = () => {
    const message = useGameStore((state) => state.messages[0]);
    const removeMessage = useGameStore((state) => state.removeMessage);

    const [open, setOpen] = useState(false);
    const [snackbarKey, setSnackbarKey] = useState(0);

    useEffect(() => {
        if (message) {
            setOpen(true);
            // Increment the key to trigger a re-render and replace the existing Snackbar
            setSnackbarKey((prevKey) => prevKey + 1);
        }
    }, [message]);

    const handleClose = () => {
        setOpen(false);
        setTimeout(() => {
            removeMessage(0);
        }, 300);
    };

    return (
        <>
            {message && (
                <Snackbar
                    key={snackbarKey}
                    open={open}
                    autoHideDuration={5000}
                    onClose={handleClose}
                >
                    <MuiAlert
                        elevation={6}
                        variant="filled"
                        severity={message.severity}
                        sx={{width: '100%'}}
                        action={
                            <IconButton
                                size="small"
                                aria-label="close"
                                color="inherit"
                                onClick={handleClose}
                            >
                                <CloseIcon fontSize="small"/>
                            </IconButton>
                        }
                    >
                        {message.message}
                    </MuiAlert>
                </Snackbar>
            )}
        </>
    );
};

export default GlobalMessageHandling;
