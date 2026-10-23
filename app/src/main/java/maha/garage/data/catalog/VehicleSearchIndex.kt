package maha.garage.data.catalog

import java.text.Normalizer
import java.util.Locale

/** Shared, cached aliases; model initials never match arbitrary trim letters. */
object VehicleSearchIndex {
    data class Entry(val fields: List<String>, val aliases: List<String>, val model: String)
    fun entry(brand: String, model: String, lineup: String, trim: String): Entry {
        val aliases = aliases(brand, model).map(::normalize)
        return Entry(listOf(brand, model, lineup, trim).map(::normalize) + aliases, aliases, normalize(model))
    }
    fun tokens(query: String) = query.trim().split(Regex("\\s+")).map(::normalize).filter { it.isNotEmpty() }
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).replace("클래스", "class")
        .replace(Regex("[^\\p{L}\\p{N}]"), "")

    fun isBenz(brand: String): Boolean {
        return normalize(brand) in setOf("벤츠", "mercedesbenz", "mercedes")
    }

    private val mercedesClass = Regex("(?i)(?:^|[^a-z])([abcegsv])(?:[\\s-]*(?:class|클래스))")
    fun aliases(brand: String, model: String): List<String> {
        val result = mutableListOf<String>()
        if (isBenz(brand)) {
            result += listOf("벤츠", "메르세데스", "mercedes", "mercedesbenz")
            mercedesClass.find(model)?.groupValues?.get(1)?.lowercase(Locale.ROOT)?.let { letter ->
                result += letter
                result += "${letter}class"
                if (letter == "g") result += listOf("g바겐", "지바겐", "g왜건", "gwagon", "gwagen")
            }
            Regex("(?i)(?:^|[^a-z])(gla|glb|glc|gle|gls|cla|cle|eqa|eqb|eqc|eqe|eqs|sl|slc|slk|sls|glk|gt)(?:[^a-z]|$)")
                .find(model)?.groupValues?.get(1)?.let { result += it.lowercase(Locale.ROOT) }
        }
        return result
    }

    fun matches(brand: String, model: String, lineup: String, trim: String, query: String): Boolean {
        return matches(entry(brand, model, lineup, trim), tokens(query))
    }
    fun matches(entry: Entry, tokens: List<String>): Boolean {
        return tokens.all { token ->
            if (token.length == 1 && token[0] in 'a'..'z') {
                token in entry.aliases || entry.model.startsWith(token)
            } else entry.fields.any { it.contains(token) }
        }
    }
}
