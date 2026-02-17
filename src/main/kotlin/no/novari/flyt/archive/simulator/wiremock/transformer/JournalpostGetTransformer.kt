package no.novari.flyt.archive.simulator.wiremock.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.archive.simulator.wiremock.InMemoryStore
import org.springframework.http.HttpStatus

class JournalpostGetTransformer(
    private val store: InMemoryStore,
    private val objectMapper: ObjectMapper,
) : ResponseTransformerV2 {
    private val logger = KotlinLogging.logger {}

    override fun getName(): String {
        return "journalpost-get"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

    override fun transform(
        response: Response,
        serveEvent: ServeEvent,
    ): Response {
        val request = serveEvent.request
        val path = request.url.substringBefore("?")
        val withoutPrefix = path.substringAfter("/arkiv/noark/sak/")
        val caseId = withoutPrefix.substringBefore("/journalpost/")
        val journalpostNumber =
            withoutPrefix
                .substringAfter("/journalpost/", "")
                .toLongOrNull()

        if (caseId.isBlank() || journalpostNumber == null) {
            logger.info {
                "wiremock request method=${request.method} path=${request.url} behavior=default result=bad_request"
            }
            return Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.NOT_FOUND.value())
                .build()
        }

        val journalpost = store.getJournalpost(caseId, journalpostNumber)
        return if (journalpost != null) {
            val body = objectMapper.writeValueAsString(journalpost)
            logger.info {
                "wiremock request method=${request.method} path=${request.url} behavior=default caseId=$caseId " +
                    "journalpostNumber=$journalpostNumber result=found"
            }
            Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.OK.value())
                .headers(withHeaders(response, header("Content-Type", "application/json")))
                .body(body)
                .build()
        } else {
            logger.info {
                "wiremock request method=${request.method} path=${request.url} behavior=default caseId=$caseId " +
                    "journalpostNumber=$journalpostNumber result=not_found"
            }
            Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.NOT_FOUND.value())
                .build()
        }
    }
}
