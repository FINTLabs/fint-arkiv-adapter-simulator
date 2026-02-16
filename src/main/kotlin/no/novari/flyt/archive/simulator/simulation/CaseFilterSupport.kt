package no.novari.flyt.archive.simulator.simulation

import no.fint.model.resource.Link
import no.fint.model.resource.arkiv.noark.KlasseResource
import no.fint.model.resource.arkiv.noark.SakResource

sealed class CaseFilterCondition {
    data class Equals(
        val field: String,
        val value: String,
    ) : CaseFilterCondition()

    data class Invalid(
        val raw: String,
    ) : CaseFilterCondition()
}

object CaseFilterParser {
    private val conditionRegex = Regex("""^(.+?)\s+eq\s+'(.*)'$""")
    private const val SEPARATOR = " and "

    fun parse(filter: String): List<CaseFilterCondition> {
        val trimmed = filter.trim()
        if (trimmed.isBlank()) {
            return emptyList()
        }

        return splitOutsideQuotes(trimmed)
            .map { segment ->
                val match = conditionRegex.matchEntire(segment)
                if (match == null) {
                    CaseFilterCondition.Invalid(segment)
                } else {
                    CaseFilterCondition.Equals(match.groupValues[1].trim(), match.groupValues[2])
                }
            }
    }

    private fun splitOutsideQuotes(input: String): List<String> {
        val segments = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < input.length) {
            val ch = input[index]
            if (ch == '\'') {
                inQuotes = !inQuotes
                current.append(ch)
                index += 1
                continue
            }

            if (!inQuotes && input.regionMatches(index, SEPARATOR, 0, SEPARATOR.length, ignoreCase = true)) {
                segments.add(current.toString().trim())
                current.setLength(0)
                index += SEPARATOR.length
                continue
            }

            current.append(ch)
            index += 1
        }

        if (current.isNotBlank()) {
            segments.add(current.toString().trim())
        }

        return segments.filter { it.isNotBlank() }
    }
}

object CaseFilterMatcher {
    fun matches(
        caseResource: SakResource,
        conditions: List<CaseFilterCondition>,
    ): Boolean {
        if (conditions.isEmpty()) {
            return true
        }

        return conditions.all { condition ->
            matchesCondition(caseResource, condition)
        }
    }

    private fun matchesCondition(
        caseResource: SakResource,
        condition: CaseFilterCondition,
    ): Boolean {
        return when (condition) {
            is CaseFilterCondition.Invalid -> {
                false
            }

            is CaseFilterCondition.Equals -> {
                val field = condition.field.lowercase()
                val value = condition.value

                when {
                    field == "tittel" -> {
                        caseResource.tittel == value
                    }

                    field == "arkivdel" -> {
                        linksContainValue(caseResource.arkivdel, value)
                    }

                    field == "administrativenhet" -> {
                        linksContainValue(caseResource.administrativEnhet, value)
                    }

                    field == "saksmappetype" -> {
                        linksContainValue(caseResource.saksmappetype, value)
                    }

                    field == "saksstatus" -> {
                        linksContainValue(caseResource.saksstatus, value)
                    }

                    field == "tilgangskode" -> {
                        linksContainValue(caseResource.skjerming?.tilgangsrestriksjon, value)
                    }

                    field.startsWith("klassifikasjon/") -> {
                        matchesKlassifikasjon(caseResource.klasse, field, value)
                    }

                    else -> {
                        false
                    }
                }
            }
        }
    }

    private fun matchesKlassifikasjon(
        klassering: List<KlasseResource>?,
        field: String,
        value: String,
    ): Boolean {
        val parts = field.split("/")
        if (parts.size != 3) {
            return false
        }

        val order =
            when (parts[1]) {
                "primar" -> 1
                "sekundar" -> 2
                "tertiar" -> 3
                else -> null
            }
                ?: return false

        val klasse =
            klassering
                ?.firstOrNull { it.rekkefolge == order }
                ?: return false

        return when (parts[2]) {
            "ordning" -> {
                linksContainValue(klasse.klassifikasjonssystem, value)
            }

            "verdi" -> {
                klasse.klasseId == value
            }

            else -> {
                false
            }
        }
    }

    private fun linksContainValue(
        links: List<Link>?,
        value: String,
    ): Boolean {
        return links?.any { link -> linkMatchesValue(link, value) } ?: false
    }

    private fun linkMatchesValue(
        link: Link,
        value: String,
    ): Boolean {
        val href = link.href ?: return false
        if (href == value) {
            return true
        }
        val trimmed = href.trimEnd('/')
        val lastSegment = trimmed.substringAfterLast('/')
        return lastSegment == value
    }
}
