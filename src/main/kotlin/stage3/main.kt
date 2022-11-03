package stage3



const val DECK_SIZE = 52
const val INITIAL_CARD_COUNT = 4
const val HAND_SIZE = 6

class Deck {
    private val ranks = setOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    private val suits = setOf('♠', '♥', '♦', '♣')

    // maintain internal state of Deck
    private lateinit var deck: MutableList<String>

    init {
        reset()
    }

    private fun originalDeck(): List<String> {
        return cartesianProduct(ranks, suits).map { pair -> pair.first + pair.second }
    }

    fun shuffle() {
        deck = deck.shuffled().toMutableList()
    }

    private fun reset() {
        deck = originalDeck().toMutableList()
    }

    fun get(n: Int): List<String> {
        if (n !in 1..DECK_SIZE) {
            throw IllegalArgumentException("Invalid number of cards.")
        }
        if (n > deck.size) {
            throw IllegalArgumentException("The remaining cards are insufficient to meet the request.")
        }

        val cards = deck.take(n)
        deck = deck.drop(n).toMutableList()
        return cards
    }

    override fun toString(): String {
        return deck.joinToString(" ")
    }
}

enum class Turn {
    Computer, Player
}

fun main() {
    println("Indigo Card Game")

    try {
        gameLoop()
    } catch (ex: GameOverException) {
        println("Game Over")
    }
}

fun gameLoop() {
    val playerStartsFirst = playerStartsFirst()

    val deck = Deck()
    deck.shuffle()

    val cardsOnTable = mutableListOf(*deck.get(INITIAL_CARD_COUNT).toTypedArray())

    println("Initial cards on the table: ${cardsOnTable.joinToString(" ")}")
    println("")
    println("${cardsOnTable.size} cards on the table, and the top card is ${cardsOnTable.last()}")

    val player = deck.get(HAND_SIZE).toMutableList()
    val computer = deck.get(HAND_SIZE).toMutableList()

    var currentPlayer = if (playerStartsFirst) Turn.Player else Turn.Computer

    while (true) {
        val playedCard = if (currentPlayer == Turn.Computer) {
            ifEmptyRefillHandFor(computer, deck)

            val cardPlayedByComputer = computer.first() // computer always picks first card
            println("Computer plays $cardPlayedByComputer")
            computer.remove(cardPlayedByComputer)

            cardPlayedByComputer
        } else {
            ifEmptyRefillHandFor(player, deck)

            val cardPlayedByPlayer = player[getValidIndexOfPlayersChosenCard(player)]
            player.remove(cardPlayedByPlayer)

            cardPlayedByPlayer
        }
        cardsOnTable.add(playedCard)

        println("${cardsOnTable.size} cards on the table, and the top card is ${cardsOnTable.last()}")

        if (cardsOnTable.size == DECK_SIZE) {
            throw GameOverException()
        }

        // toggle player and continue with next round
        currentPlayer = if (currentPlayer == Turn.Player) Turn.Computer else Turn.Player
    }
}

fun ifEmptyRefillHandFor(hand: MutableList<String>, deck: Deck) {
    // pick 6 new cards if hand is empty
    if (hand.isEmpty()) {
        hand.addAll(deck.get(HAND_SIZE))
    }
}

fun getValidIndexOfPlayersChosenCard(playerCards: List<String>): Int {
    println("Cards in hand: " + playerCards.withIndex().joinToString(" ") {
            indexedValue -> "${indexedValue.index + 1})${indexedValue.value}"})

    while (true) {
        println("Choose a card to play (1-${playerCards.size}):")

        val input = readLine()!!
        if (input == "exit") {
            throw GameOverException()
        } else if (input.matches("\\d".toRegex())) {
            val chosenCard = input.toInt()
            if (chosenCard in 1..playerCards.size) {
                return chosenCard - 1
            }
        }
    }
}

fun playerStartsFirst(): Boolean {
    println("Play first?")
    return when (readLine()!!) {
        "yes" -> true
        "no" -> false
        else -> playerStartsFirst()
    }
}

// https://gist.github.com/kiwiandroiddev/fef957a69f91fa64a46790977d98862b
fun <T, U> cartesianProduct(c1: Collection<T>, c2: Collection<U>): List<Pair<T, U>> {
    return c1.flatMap { lhsElem -> c2.map { rhsElem -> lhsElem to rhsElem } }
}

class GameOverException: RuntimeException()