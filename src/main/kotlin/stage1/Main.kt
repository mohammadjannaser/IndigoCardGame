
package stage1



class Deck {
    val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    val suits = listOf('♠', '♥', '♦', '♣')

    fun deck(): List<String> {
        return cartesianProduct(ranks,suits).map { pair -> pair.first + pair.second }
    }
}

fun main() {
    val deck = Deck()
    println(deck.ranks.joinToString(" "))
    println(deck.suits.joinToString(" "))
    println(deck.deck().joinToString(" "))

}

fun <T, U> cartesianProduct(c1: Collection<T>, c2: Collection<U>): List<Pair<T,U>> {
    return c1.flatMap { lhsElem -> c2.map { rhsElem -> lhsElem to rhsElem } }
}

