package com.example.data.model

import kotlin.random.Random

data class TarotCard(
    val id: Int,
    val name: String,
    val arcana: String, // "Major" or "Minor"
    val uprightMeaning: String,
    val reversedMeaning: String,
    val description: String
)

data class DrawnCard(
    val card: TarotCard,
    val isReversed: Boolean
) {
    val displayName: String get() = if (isReversed) "${card.name} (Reversed)" else card.name
    val activeMeaning: String get() = if (isReversed) card.reversedMeaning else card.uprightMeaning
}

object TarotDeck {
    private val majorArcana = listOf(
        "The Fool" to ("New beginnings, spontaneous adventures, unlimited potential" to "Recklessness, risk-taking, holding back, fear of transition"),
        "The Magician" to ("Focus, action, manifestation, capability, intellect" to "Illusions, trickery, misdirected energy, blockages"),
        "The High Priestess" to ("Intuition, cosmic wisdom, divine secrets, subconscious mind" to "Surface-level outlook, ignored inner whispers, secret motives"),
        "The Empress" to ("Creativity, nurture, dynamic beauty, abundance, nature" to "Creative stagnation, domestic friction, dependence"),
        "The Emperor" to ("Authority, structure, establishing limits, protection, control" to "Tyranny, chaotic control, rigidity, structural collapse"),
        "The Hierophant" to ("Tradition, spiritual guidance, seeking counsel, rules" to "Rebellion, custom doctrines, restriction, dogma"),
        "The Lovers" to ("Harmony, deep relationships, absolute choice, alignment" to "Disharmony, misalignments, critical choices postponed"),
        "The Chariot" to ("Willpower, victory, decisive override, direction" to "Lack of direction, losing path control, passive collapse"),
        "Strength" to ("Courage, quiet endurance, resilience, compassion" to "Self-doubt, raw animalistic impulse, weakness"),
        "The Hermit" to ("Inner reflection, isolation, soul seeking, guidance" to "Loneliness, rejection, paranoid withdrawal"),
        "Wheel of Fortune" to ("Good luck, cosmic cycles, decisive pivot points" to "Bad luck, resistance to change, cycle repetition"),
        "Justice" to ("Karma, truth, fairness, legal resolution" to "Injustice, structural bias, dishonesty, denial"),
        "The Hanged Man" to ("Letting go, sacrifice, new intellectual angle" to "Stalling, wasting efforts, useless sacrifice"),
        "Death" to ("Complete transformation, purging, transitions" to "Resistance to change, painful stagnation, decay"),
        "Temperance" to ("Balance, chemistry, moderation, patience" to "Imbalance, excess, clashing energies"),
        "The Devil" to ("Attachment, materialism, toxic cycles, limitation" to "Releasing chains, breaking bonds, self-evaluation"),
        "The Tower" to ("Sudden disruption, shock revelation, baseline shift" to "Avoiding disaster, fear of progress, prolonged ruin"),
        "The Star" to ("Optimism, cosmic hope, healing, core renewal" to "Hopelessness, creative block, faded motivation"),
        "The Moon" to ("Illusion, shadows, psychic visions, fear, unconscious" to "Releasing fear, unmasked secrets, confusion resolved"),
        "The Sun" to ("Radiance, vitality, success, joy, clarity" to "Temporary clouds, arrogance, unrealistic optimism"),
        "Judgement" to ("Awakening, evaluation, cosmic calling, reckoning" to "Self-doubt, ignored insights, indecision"),
        "The World" to ("Completion, fulfillment, travel, absolute success" to "Incomplete goals, shortcuts, delayed closure")
    )

    private val suits = listOf("Wands", "Cups", "Swords", "Pentacles")
    private val ranks = listOf("Ace", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Page", "Knight", "Queen", "King")

    val cards: List<TarotCard> by lazy {
        val list = mutableListOf<TarotCard>()
        var currentId = 1

        // Add 22 Major Arcana
        majorArcana.forEachIndexed { index, (name, meanings) ->
            list.add(
                TarotCard(
                    id = currentId++,
                    name = name,
                    arcana = "Major",
                    uprightMeaning = meanings.first,
                    reversedMeaning = meanings.second,
                    description = "Major Arcana card #$index. Represents spiritual principles and major life transitions."
                )
            )
        }

        // Add 56 Minor Arcana (4 suits * 14 ranks)
        suits.forEach { suit ->
            ranks.forEach { rank ->
                val cardName = "$rank of $suit"
                val upright = when (suit) {
                    "Wands" -> "Energy, inspiration, creative fire, progression, and potential."
                    "Cups" -> "Emotion, intuition, relationships, deep flow, and connection."
                    "Swords" -> "Intellect, action, choices, challenges, and sharp communication."
                    else -> "Material wealth, finances, career focus, growth, and manifestation."
                }
                val reversed = when (suit) {
                    "Wands" -> "Stifled passion, delays, burnt-out energy, or lack of visual direction."
                    "Cups" -> "Emotional block, suppressed feelings, relationship static, or distance."
                    "Swords" -> "Mental clutter, miscommunication, indecision, or betrayal resolved."
                    else -> "Financial stagnation, greed, instability, or lost investment."
                }
                list.add(
                    TarotCard(
                        id = currentId++,
                        name = cardName,
                        arcana = "Minor",
                        uprightMeaning = upright,
                        reversedMeaning = reversed,
                        description = "Minor Arcana suit of $suit focusing on everyday elements and actions."
                    )
                )
            }
        }
        list
    }

    fun drawCard(): DrawnCard {
        val randomIndex = Random.nextInt(cards.size)
        val isReversed = Random.nextBoolean()
        return DrawnCard(cards[randomIndex], isReversed)
    }

    fun getCardById(id: Int): TarotCard {
        return cards.firstOrNull { it.id == id } ?: cards.first()
    }
}
