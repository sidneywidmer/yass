import React from "react";

const Position = ({position}) => {
    let representation = "";

    switch (position) {
        case "NORTH":
            representation = "🔼";
            break;
        case "EAST":
            representation = "▶️";
            break;
        case "SOUTH":
            representation = "🔽";
            break;
        case "WEST":
            representation = "◀️";
            break;
        default:
            representation = position;
    }

    return <span>{representation}</span>;
}

export default Position;