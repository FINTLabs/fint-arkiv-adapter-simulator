package no.novari.flyt.archive.simulator.wiremock.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import no.fint.model.resource.arkiv.noark.SakResources
import no.novari.flyt.archive.simulator.simulation.BehaviorKeys
import no.novari.flyt.archive.simulator.simulation.BehaviorMode
import no.novari.flyt.archive.simulator.simulation.SimulatorBehaviorStore
import no.novari.flyt.archive.simulator.wiremock.InMemoryStore
import org.springframework.http.HttpStatus
import java.time.Duration

class CaseQueryTransformer(
    private val store: InMemoryStore,
    private val behaviorStore: SimulatorBehaviorStore,
    private val defaultDelay: Duration,
    private val objectMapper: ObjectMapper,
) : ResponseTransformerV2 {
    private val logger = KotlinLogging.logger {}

    override fun getName(): String {
        return "case-query"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

    override fun transform(
        response: Response,
        serveEvent: ServeEvent,
    ): Response {
        val request = serveEvent.request
        val behavior = behaviorStore.get(BehaviorKeys.CASE_QUERY)
        val summary = behaviorSummary(behavior, defaultDelay)
        if (behavior.mode == BehaviorMode.FAIL) {
            logger.info {
                "wiremock request method=${request.method} path=${request.url} behavior=$summary"
            }
            return applyBehavior(response, behavior, defaultDelay)
        }

        val filter = request.bodyAsString?.trim().orEmpty()
        val matchedCases = store.findCases(filter)
        val body = objectMapper.writeValueAsString(SakResources(matchedCases))
        val baseResponse =
            Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.OK.value())
                .headers(withHeaders(response, header("Content-Type", "application/json")))
                .body(body)
                .build()

        val emptyResponse = {
            val emptyBody = objectMapper.writeValueAsString(SakResources(emptyList()))
            Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.OK.value())
                .headers(withHeaders(response, header("Content-Type", "application/json")))
                .body(emptyBody)
                .build()
        }

        val filterLabel = filter.ifBlank { "-" }
        logger.info {
            "wiremock request method=${request.method} path=${request.url} behavior=$summary " +
                "filter='$filterLabel' matches=${matchedCases.size}"
        }

        return applyBehavior(baseResponse, behavior, defaultDelay, emptyResponse)
    }
}
