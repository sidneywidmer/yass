package ch.yass.core.valiktor
import ch.yass.game.dto.Trump as TrumpEnum
import ch.yass.game.dto.WinningConditionType
import org.valiktor.Constraint
import org.valiktor.Validator

object Suit : Constraint
object Trump : Constraint
object WinningConditionType : Constraint

fun <E> Validator<E>.Property<String?>.isSuit(): Validator<E>.Property<String?> = this.validate(Suit) {
    it == null || ch.yass.game.dto.Suit.entries.any { suit -> suit.name == it }
}

fun <E> Validator<E>.Property<String?>.isTrump(): Validator<E>.Property<String?> = this.validate(Trump) {
    it == null || TrumpEnum.playable().any { trump -> trump.name == it }
}

fun <E> Validator<E>.Property<String?>.isWinningConditionType(): Validator<E>.Property<String?> =
    this.validate(WinningConditionType) {
        it == null || WinningConditionType.entries.any { wc -> wc.name == it }
    }