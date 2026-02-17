package no.novari.flyt.archive.simulator.wiremock

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.FintLinks
import no.fint.model.resource.Link
import no.fint.model.resource.arkiv.noark.DokumentfilResource
import no.fint.model.resource.arkiv.noark.JournalpostResource
import no.fint.model.resource.arkiv.noark.SakResource
import no.fint.model.resource.arkiv.noark.SakResources
import no.novari.flyt.archive.simulator.simulation.CaseFilterCondition
import no.novari.flyt.archive.simulator.simulation.CaseFilterMatcher
import no.novari.flyt.archive.simulator.simulation.CaseFilterParser
import no.novari.flyt.archive.simulator.simulation.ResourceCatalog
import no.novari.flyt.archive.simulator.simulation.ResourceCollection
import java.time.Year
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class InMemoryStore(
    resourceCatalog: ResourceCatalog,
) {
    private val logger = KotlinLogging.logger {}
    private val objectMapper =
        ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val currentYear = AtomicInteger(Year.now().value)
    private val nextCaseSequence = AtomicLong(0)
    private val nextFileId = AtomicLong(0)
    private val statusToCaseId = ConcurrentHashMap<String, String>()
    private val statusToFileId = ConcurrentHashMap<String, Long>()
    private val cases = ConcurrentHashMap<String, SakResource>()
    private val files = ConcurrentHashMap<Long, DokumentfilResource>()
    private val resourceLastUpdated = ConcurrentHashMap<String, AtomicLong>()
    private val resources = ConcurrentHashMap<String, List<FintLinks>>()

    init {
        resourceCatalog.definitions.forEach { definition ->
            resources[definition.path] = definition.items
            resourceLastUpdated[definition.path] = AtomicLong(System.currentTimeMillis())
        }
    }

    fun createCase(requestJson: String?): Pair<String, String> {
        val id = nextCaseId()
        val statusId = UUID.randomUUID().toString()
        val parsedCase = parseCaseResource(requestJson)
        val title =
            parsedCase
                ?.tittel
                ?.takeIf { it.isNotBlank() }
                ?: extractTitle(requestJson)
                ?: "POC-sak $id"

        val caseResource = parsedCase ?: SakResource()
        caseResource.mappeId = identifikator(id)
        caseResource.systemId = identifikator(id)
        caseResource.tittel = title
        val selfHref = "/arkiv/noark/sak/$id"
        val existingSelf =
            caseResource
                .linksIfPresent
                ?.get("self")
                ?.any { it.href == selfHref }
                ?: false
        if (!existingSelf) {
            caseResource.addSelf(Link.with(selfHref))
        }

        cases[id] = caseResource
        statusToCaseId[statusId] = id

        logger.info { "store action=create-case caseId=$id statusId=$statusId title=$title" }

        return id to statusId
    }

    fun addJournalpost(
        caseId: String,
        requestJson: String? = null,
    ): String? {
        val caseResource = cases[caseId] ?: return null
        val nextNumber = nextJournalpostNumber(caseResource)

        val journalpost = extractJournalpost(requestJson) ?: JournalpostResource()
        journalpost.journalPostnummer = nextNumber
        val selfHref = "/arkiv/noark/sak/$caseId/journalpost/$nextNumber"
        val existingSelf =
            journalpost
                .linksIfPresent
                ?.get("self")
                ?.any { it.href == selfHref }
                ?: false
        if (!existingSelf) {
            journalpost.addSelf(Link.with(selfHref))
        }
        if (journalpost.tittel.isNullOrBlank()) {
            journalpost.tittel = "Journalpost $nextNumber"
        }

        val journalposter = caseResource.journalpost?.toMutableList() ?: mutableListOf()
        journalposter.add(journalpost)
        caseResource.journalpost = journalposter

        val statusId = UUID.randomUUID().toString()
        statusToCaseId[statusId] = caseId

        logger.info {
            "store action=add-journalpost caseId=$caseId statusId=$statusId journalpostNumber=$nextNumber " +
                "title=${journalpost.tittel}"
        }

        return statusId
    }

    fun createFile(fileName: String?): Pair<Long, String> {
        val id = nextFileId.incrementAndGet()
        val statusId = UUID.randomUUID().toString()

        val fileResource = DokumentfilResource()
        fileResource.systemId = identifikator(id.toString())
        fileResource.filnavn = fileName ?: "file-$id"

        files[id] = fileResource
        statusToFileId[statusId] = id

        logger.info { "store action=create-file fileId=$id statusId=$statusId fileName=${fileResource.filnavn}" }

        return id to statusId
    }

    fun resolveCaseStatus(statusId: String): String? {
        return statusToCaseId[statusId]
    }

    fun resolveFileStatus(statusId: String): Long? {
        return statusToFileId[statusId]
    }

    fun getCase(caseId: String): SakResource? {
        return cases[caseId]
    }

    fun getJournalpost(
        caseId: String,
        journalpostNumber: Long,
    ): JournalpostResource? {
        val caseResource = cases[caseId] ?: return null
        return caseResource.journalpost?.firstOrNull { jp ->
            jp.journalPostnummer == journalpostNumber
        }
    }

    fun getCasesCollection(): SakResources {
        return SakResources(cases.values)
    }

    fun findCases(filter: String?): List<SakResource> {
        val trimmed = filter?.trim().orEmpty()
        if (trimmed.isBlank()) {
            return cases.values.toList()
        }

        val conditions = CaseFilterParser.parse(trimmed)
        val invalidCount = conditions.count { it is CaseFilterCondition.Invalid }
        if (invalidCount > 0) {
            logger.warn { "store action=find-cases filter='$trimmed' invalidConditions=$invalidCount" }
        }

        return cases.values.filter { caseResource ->
            CaseFilterMatcher.matches(caseResource, conditions)
        }
    }

    fun getResourceCollection(path: String): ResourceCollection<FintLinks>? {
        val resourcesForPath = resources[path] ?: return null
        return ResourceCollection(resourcesForPath)
    }

    fun getLastUpdated(path: String): Long {
        return resourceLastUpdated[path]?.get() ?: System.currentTimeMillis()
    }

    fun touchResource(path: String) {
        resourceLastUpdated[path]?.set(System.currentTimeMillis())
    }

    fun reset() {
        currentYear.set(Year.now().value)
        nextCaseSequence.set(0)
        nextFileId.set(0)
        statusToCaseId.clear()
        statusToFileId.clear()
        cases.clear()
        files.clear()
        resourceLastUpdated.values.forEach { it.set(System.currentTimeMillis()) }
        logger.info { "store action=reset" }
    }

    private fun extractTitle(requestJson: String?): String? {
        if (requestJson.isNullOrBlank()) return null
        return runCatching {
            val node = objectMapper.readTree(requestJson)
            node.path("tittel").asText(null)
        }.getOrNull()
    }

    private fun extractJournalpost(requestJson: String?): JournalpostResource? {
        if (requestJson.isNullOrBlank()) return null
        return runCatching {
            objectMapper
                .readValue(requestJson, JournalpostWrapper::class.java)
                .journalpost
                .firstOrNull()
        }.getOrNull()
    }

    private fun parseCaseResource(requestJson: String?): SakResource? {
        if (requestJson.isNullOrBlank()) return null
        return runCatching {
            objectMapper.readValue(requestJson, SakResource::class.java)
        }.getOrNull()
    }

    private fun nextJournalpostNumber(caseResource: SakResource): Long {
        val journalposter = caseResource.journalpost ?: return 1
        var max = 0L
        journalposter.forEach { jp ->
            val current = jp.journalPostnummer ?: 0L
            if (current > max) max = current
        }
        return max + 1
    }

    private fun identifikator(value: String): Identifikator {
        val identifikator = Identifikator()
        identifikator.identifikatorverdi = value
        return identifikator
    }

    private fun nextCaseId(): String {
        val year = Year.now().value
        if (currentYear.get() != year) {
            currentYear.set(year)
            nextCaseSequence.set(0)
        }
        val sequence = nextCaseSequence.incrementAndGet()
        return "$year/$sequence"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class JournalpostWrapper(
        val journalpost: List<JournalpostResource> = emptyList(),
    )
}
