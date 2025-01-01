package ch.yass.game.engine

import ch.yass.game.dto.WeisType

fun weisPoints(type: WeisType): Int {
    return when (type) {
        WeisType.DREI_BLATT -> 20
        WeisType.VIER_BLATT -> 50
        WeisType.FUENF_BLATT -> 100
        WeisType.SECHS_BLATT -> 150
        WeisType.SIEBEN_BLATT -> 200
        WeisType.ACHT_BLATT -> 250
        WeisType.NEUN_BLATT -> 300
        WeisType.VIER_GLEICHE -> 100
        WeisType.VIER_NELL -> 150
        WeisType.VIER_BUUR -> 200
        WeisType.STOECK -> 20
        WeisType.SKIP -> 0
    }
}