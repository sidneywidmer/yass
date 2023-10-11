import React from "react"

const Gschobe = ({gschobe}) => {
    let emoji = ""

    switch (gschobe) {
        case "YES":
            emoji = "️🔄"
            break
        case "NO":
            emoji = ""
            break
        default:
            emoji = gschobe
    }

    return <span>{emoji}</span>
}

export default Gschobe