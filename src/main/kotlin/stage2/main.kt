package stage2

import java.lang.IllegalArgumentException

class Deck {

    private val ranks = setOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    private val suits = setOf('♠', '♥', '♦', '♣')

    private lateinit var deck: MutableList<String>

    init {
        reset()
    }

    private fun originalDeck() : List<String> {
        return cartesianProduct(ranks, suits).map { pair -> pair.first + pair.second }
    }

    fun shuffle() {
        deck = deck.shuffled().toMutableList()
    }

    fun reset() {
        deck = originalDeck().toMutableList()
    }

    fun get(n: Int): List<String> {
        if (n !in 1..52)
            throw IllegalArgumentException("Invalid number of cards.")
        if (n > deck.size)
            throw  IllegalArgumentException("The remaining cards are insufficient to meet the request.")

        val cards = deck.take(n)
        deck = deck.drop(n).toMutableList()
        return cards
    }

    override fun toString(): String {
        return deck.joinToString(" ")
    }
}

fun main() {
    val deck = Deck()

    while (true) {
        when(getValidAction()) {
            "exit" -> {
                println("Bye")
                return
            }
            "reset" -> reset(deck)
            "shuffle" -> shuffle(deck)
            "get" -> get(deck)
        }
    }
}

fun getValidAction(): String {
    val validActions = listOf("reset", "shuffle", "get", "exit")

    println("Choose an action (${validActions.joinToString(", ")}):")

    val input = readln()
    return if (input in validActions) {
        input
    } else {
        println("Wrong action.")
        getValidAction()
    }
}

fun reset(deck: Deck) {
    deck.reset()
    println("Card deck is reset.")
}

fun shuffle(deck: Deck) {
    deck.shuffle()
    println("Card deck is shuffled.")
}


fun get(deck: Deck) {
    println("Number of cards:")

    val countInput = readln()
    if (!countInput.matches(Regex("(\\d)+"))) {
        println("Invalid number of cards.")
        return
    }

    val count = countInput.toInt()

    try {
        val taken = deck.get(count)
        println(taken.joinToString(" "))
    } catch (ex: IllegalArgumentException) {
        println(ex.message)
    }
}

fun <T, U> cartesianProduct(c1: Collection<T>, c2: Collection<U>): List<Pair<T, U>> {
    return c1.flatMap { lhsElem -> c2.map { rhsElem -> lhsElem to rhsElem } }
}