package no.novari.flyt.archive.simulator.wiremock.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.archive.simulator.simulation.BehaviorKeys
import no.novari.flyt.archive.simulator.simulation.BehaviorMode
import no.novari.flyt.archive.simulator.simulation.ResourceCollection
import no.novari.flyt.archive.simulator.simulation.SimulatorBehaviorStore
import no.novari.flyt.archive.simulator.wiremock.InMemoryStore
import org.springframework.http.HttpStatus
import java.time.Duration

class AdminUnitCollectionTransformer(
    private val store: InMemoryStore,
    private val behaviorStore: SimulatorBehaviorStore,
    private val defaultDelay: Duration,
    private val objectMapper: ObjectMapper,
) : ResponseTransformerV2 {
    private val logger = KotlinLogging.logger {}

    override fun getName(): String {
        return "admin-unit-collection"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

    override fun transform(
        response: Response,
        serveEvent: ServeEvent,
    ): Response {
        val path = "/arkiv/noark/administrativenhet"
        val behavior = behaviorStore.get(BehaviorKeys.resource(path))
        if (behavior.mode == BehaviorMode.FAIL) {
            logger.info {
                "wiremock request method=${serveEvent.request.method} path=$path " +
                    "behavior=${behaviorSummary(behavior, defaultDelay)}"
            }
            return applyBehavior(response, behavior, defaultDelay)
        }

        val body =
            store.getResourceCollection(path)?.let { objectMapper.writeValueAsString(it) }
                ?: objectMapper.writeValueAsString(ResourceCollection(emptyList<Any>()))
        val baseResponse =
            Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.OK.value())
                .headers(withHeaders(response, header("Content-Type", "application/json")))
                .body(body)
                .build()

        val emptyResponse = {
            Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.OK.value())
                .headers(withHeaders(response, header("Content-Type", "application/json")))
                .body(objectMapper.writeValueAsString(ResourceCollection(emptyList<Any>())))
                .build()
        }

        logger.info {
            "wiremock request method=${serveEvent.request.method} path=$path " +
                "behavior=${behaviorSummary(behavior, defaultDelay)}"
        }

        return applyBehavior(baseResponse, behavior, defaultDelay, emptyResponse)
    }
}
