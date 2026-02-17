package no.novari.flyt.archive.simulator.wiremock.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.archive.simulator.wiremock.InMemoryStore
import org.springframework.http.HttpStatus

class CaseGetTransformer(
    private val store: InMemoryStore,
    private val objectMapper: ObjectMapper,
) : ResponseTransformerV2 {
    private val logger = KotlinLogging.logger {}

    override fun getName(): String {
        return "case-get"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

    override fun transform(
        response: Response,
        serveEvent: ServeEvent,
    ): Response {
        val request = serveEvent.request
        val caseId =
            request.url
                .substringAfter("/arkiv/noark/sak/")
        val caseResource =
            if (caseId.isNotBlank()) {
                store.getCase(caseId)
            } else {
                null
            }

        return if (caseResource != null) {
            val body = objectMapper.writeValueAsString(caseResource)
            logger.info {
                "wiremock request method=${request.method} path=${request.url} behavior=default caseId=$caseId result=found"
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
                "wiremock request method=${request.method} path=${request.url} behavior=default caseId=$caseId result=not_found"
            }
            Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.NOT_FOUND.value())
                .build()
        }
    }
}
