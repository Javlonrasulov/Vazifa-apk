package uz.vazifa.app.util

object UzbekTextSearch {
    private val latinToCyrSingle = mapOf(
        'a' to 'а', 'b' to 'б', 'd' to 'д', 'e' to 'е', 'f' to 'ф', 'g' to 'г',
        'h' to 'ҳ', 'i' to 'и', 'j' to 'ж', 'k' to 'к', 'l' to 'л', 'm' to 'м',
        'n' to 'н', 'o' to 'о', 'p' to 'п', 'q' to 'қ', 'r' to 'р', 's' to 'с',
        't' to 'т', 'u' to 'у', 'v' to 'в', 'x' to 'х', 'y' to 'й', 'z' to 'з',
        'w' to 'в', 'c' to 'к',
    )

    private val cyrToLatinMulti = listOf(
        "ш" to "sh",
        "ч" to "ch",
        "ғ" to "g'",
        "ў" to "o'",
        "қ" to "q",
        "ҳ" to "h",
        "нг" to "ng",
    )

    private val cyrToLatinSingle = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e",
        'ж' to "j", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l",
        'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s",
        'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "x", 'э' to "e", 'ю' to "yu",
        'я' to "ya", 'ё' to "yo", 'ц' to "ts", 'қ' to "q", 'ғ' to "g'", 'ў' to "o'",
        'ҳ' to "h", 'ш' to "sh", 'ч' to "ch",
    )

    fun latinToCyrillic(input: String): String {
        var s = input.lowercase()
        s = s.replace("sh", "ш")
            .replace("ch", "ч")
            .replace("g'", "ғ")
            .replace("o'", "ў")
            .replace("ng", "нг")
        return buildString {
            for (ch in s) append(latinToCyrSingle[ch] ?: ch)
        }
    }

    fun cyrillicToLatin(input: String): String {
        val s = input.lowercase()
        val result = StringBuilder()
        var i = 0
        while (i < s.length) {
            var matched = false
            for ((cyr, lat) in cyrToLatinMulti) {
                if (s.regionMatches(i, cyr, 0, cyr.length)) {
                    result.append(lat)
                    i += cyr.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                result.append(cyrToLatinSingle[s[i]] ?: s[i].toString())
                i++
            }
        }
        return result.toString()
    }

    fun variants(text: String): Set<String> {
        val base = text.lowercase().trim()
        if (base.isEmpty()) return emptySet()
        val latin = cyrillicToLatin(base)
        val cyr = latinToCyrillic(latin)
        return setOf(base, latin, cyr).filter { it.isNotEmpty() }.toSet()
    }

    fun matches(text: String, query: String): Boolean {
        if (query.isBlank()) return true
        val queryVariants = variants(query)
        val textVariants = variants(text)
        return queryVariants.any { q -> textVariants.any { t -> t.contains(q) } }
    }

    fun matchesPhone(phone: String?, login: String?, query: String): Boolean {
        val queryDigits = query.filter { it.isDigit() }
        if (queryDigits.isEmpty()) return false
        val nationalQuery = UzPhoneFormatter.extractNationalDigits(queryDigits)
        val targets = listOfNotNull(phone, login).map { it.filter(Char::isDigit) }
        return targets.any { target ->
            target.contains(queryDigits) ||
                (nationalQuery.length >= 2 && (target.contains(nationalQuery) || target.endsWith(nationalQuery)))
        }
    }

    fun matchesEmployee(fullName: String, login: String, phone: String?, query: String, position: String? = null): Boolean {
        val q = query.trim()
        if (q.isBlank()) return true
        return matches(fullName, q) ||
            matches(login, q) ||
            position?.let { matches(it, q) } == true ||
            matchesPhone(phone, login, q)
    }
}
