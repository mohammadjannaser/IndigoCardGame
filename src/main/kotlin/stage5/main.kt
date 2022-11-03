package stage5


const val DECK_SIZE = 52
const val INITIAL_CARD_COUNT = 4
const val HAND_SIZE = 6

class Card(val rank: String, val suit: String) {
    val value = if (rank in listOf("A", "10", "J", "Q", "K")) 1 else 0

    override fun toString(): String {
        return rank + suit
    }
}

class Deck {
    private val ranks = setOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    private val suits = setOf('♠', '♥', '♦', '♣')

    // maintain internal state of Deck
    private lateinit var deck: MutableList<Card>

    init {
        reset()
    }

    private fun originalDeck(): List<Card> {
        return cartesianProduct(ranks, suits).map { pair -> Card(pair.first, pair.second.toString()) }
    }

    fun shuffle() {
        deck = deck.shuffled().toMutableList()
    }

    private fun reset() {
        deck = originalDeck().toMutableList()
    }

    fun get(n: Int): List<Card> {
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

    val isEmpty get() = deck.isEmpty()

    val size get() = deck.size
}

abstract class Player(val hand: MutableList<Card>, val name: String) {
    val handIsEmpty get() = hand.isEmpty()

    var score: Int = 0
    var cards: Int = 0

    // choose which card to play based on the top card on the table
    abstract fun playCard(topCard: Card?): Card

    override fun toString(): String {
        return hand.joinToString(" ")
    }

    fun addToScore(n: Int) {
        score += n
    }

    fun addToCards(n: Int) {
        cards += n
    }

    // pick 6 new cards if hand is empty
    fun ifEmptyRefillHandFrom(deck: Deck) {
        if (handIsEmpty) {
            hand.addAll(deck.get(HAND_SIZE))
        }
    }
}

class Human(hand: MutableList<Card>): Player(hand, "Player") {
    override fun playCard(topCard: Card?): Card {
        return hand.removeAt(getValidIndexOfHumanChosenCard())
    }

    // ask player what card from his hand he wants play
    // will return index (not number!) of this card (0 - hand.size-1)
    private fun getValidIndexOfHumanChosenCard(): Int {
        println("Cards in hand: " + hand.withIndex().joinToString(" ") { "${it.index + 1})${it.value}"})

        while (true) {
            println("Choose a card to play (1-${hand.size}):")

            val input = readLine()!!
            if (input == "exit") {
                throw GameOverException()
            } else if (input.matches("\\d".toRegex())) {
                val chosenCard = input.toInt()
                if (chosenCard in 1..hand.size) {
                    return chosenCard - 1
                }
            }
        }
    }
}

class Computer(hand: MutableList<Card>): Player(hand, "Computer") {
    override fun playCard(topCard: Card?): Card {
        println(hand.joinToString(" "))

        val cardPlayed = getCardToPlay(topCard)

        hand.remove(cardPlayed)
        println("Computer plays $cardPlayed")
        return cardPlayed
    }

    private fun getCardToPlay(topCard: Card?): Card {
        if (hand.size == 1)
            return hand.first()

        val candidates = findCandidateCards(topCard)

        if (topCard == null || candidates.isEmpty()) {
            return playCardFrom(hand)
        }

        if (candidates.size == 1) {
            return candidates.first()
        }

        // here we are sure to have multiple candidates
        assert(candidates.size > 1)

        return playCardFrom(candidates)
    }

    private fun playCardFrom(cards: List<Card>): Card {
        // find suits that occur more than once and pick one of those cards at random
        val cardsWithRedundantSuit = groupByRedundantSuit(cards)
        if (cardsWithRedundantSuit.isNotEmpty()) {
            return cardsWithRedundantSuit.random()
        }

        // find ranks that occur more than once and pick one of those cards at random
        val cardsWithRedundantRank = groupByRedundantRank(cards)
        if (cardsWithRedundantRank.isNotEmpty()) {
            return cardsWithRedundantRank.random()
        }

        // all cards have different suit and rank, choose a random one
        return cards.random()
    }

    // find cards that would beat the topCard on table (has same rank or suit)
    private fun findCandidateCards(topCard: Card?): List<Card> {
        return if (topCard != null) {
            hand.filter { it.suit == topCard.suit || it.rank == topCard.rank }
        } else emptyList()
    }

    private fun groupByRedundantSuit(cards: List<Card>): List<Card> {
        return cards.groupBy { it.suit }
            .filter { it.value.size > 1 }.values.flatten()
    }

    private fun groupByRedundantRank(cards: List<Card>): List<Card> {
        return cards.groupBy { it.rank }
            .filter { it.value.size > 1 }.values.flatten()
    }
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
    val humanStartsFirst = humanStartsFirst()

    val deck = Deck()
    deck.shuffle()

    val cardsOnTable = mutableListOf(*deck.get(INITIAL_CARD_COUNT).toTypedArray())

    println("Initial cards on the table: ${cardsOnTable.joinToString(" ")}")
    println("")
    printCardOnTable(cardsOnTable)

    val human = Human(deck.get(HAND_SIZE).toMutableList())
    val computer = Computer(deck.get(HAND_SIZE).toMutableList())
    var currentPlayer = if (humanStartsFirst) human else computer
    var lastWinner = currentPlayer

    while (true) {
        currentPlayer.ifEmptyRefillHandFrom(deck)
        val topCard = if (cardsOnTable.isNotEmpty()) cardsOnTable.last() else null
        val playedCard = currentPlayer.playCard(topCard)

        if (currentPlayerWinsCardsOnTable(playedCard, cardsOnTable)) {
            // currentPlayer wins cards on table AND his played card!
            currentPlayer.addToScore(cardsOnTable.sumOf { it.value } + playedCard.value)
            currentPlayer.addToCards(cardsOnTable.size + 1)
            lastWinner = currentPlayer
            cardsOnTable.clear()

            println("${currentPlayer.name} wins cards")
            printScore(human, computer)
        } else {
            cardsOnTable.add(playedCard)
        }

        printCardOnTable(cardsOnTable)

        // all cards have been played, let's wrap this up!
        if (human.handIsEmpty && computer.handIsEmpty && deck.isEmpty) {
            // remaining cards on table go to last winner
            lastWinner.addToScore(cardsOnTable.sumOf { it.value })
            lastWinner.addToCards(cardsOnTable.size)

            awardFinalThreePoints(human, computer, humanStartsFirst)

            // some invariants, if these fail something went wrong
            assert(human.score + computer.score == 23)
            assert(human.cards + computer.cards == DECK_SIZE)

            printScore(human, computer)

            throw GameOverException()
        }

        // toggle player and continue with next round
        currentPlayer = if (currentPlayer == human) computer else human

        // this invariant should always hold, we always have 52 cards in play
        assert(human.hand.size + human.cards + computer.hand.size + computer.cards + deck.size + cardsOnTable.size == DECK_SIZE)
    }
}

fun printScore(human: Player, computer: Player) {
    println("Score: ${human.name} ${human.score} - ${computer.name} ${computer.score}")
    println("Cards: ${human.name} ${human.cards} - ${computer.name} ${computer.cards}")
}

fun currentPlayerWinsCardsOnTable(played: Card, cardsOnTable: List<Card>): Boolean {
    if (cardsOnTable.isEmpty())
        return false

    val topCard = cardsOnTable.last()
    return played.rank == topCard.rank || played.suit == topCard.suit
}

fun printCardOnTable(cardsOnTable: List<Card>) {
    if (cardsOnTable.isEmpty()) {
        println("No cards on the table")
    } else {
        println("${cardsOnTable.size} cards on the table, and the top card is ${cardsOnTable.last()}")
    }
}

// final three points go to player with most cards or if tied to player who started game
fun awardFinalThreePoints(human: Player, computer: Player, humanStartsFirst: Boolean) {
    val playerThatGetsFinal3Points = listOf(human, computer).maxByOrNull { it.cards }
        ?: if (humanStartsFirst) human else computer

    playerThatGetsFinal3Points.addToScore(3)
}

// ask player if he wants to go first or not
fun humanStartsFirst(): Boolean {
    println("Play first?")
    return when (readLine()!!) {
        "yes" -> true
        "no" -> false
        else -> humanStartsFirst()
    }
}

fun <T, U> cartesianProduct(c1: Collection<T>, c2: Collection<U>): List<Pair<T, U>> {
    return c1.flatMap { lhsElem -> c2.map { rhsElem -> lhsElem to rhsElem } }
}

class GameOverException: RuntimeException()