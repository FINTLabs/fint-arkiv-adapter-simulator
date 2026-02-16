package no.novari.flyt.archive.simulator.wiremock.transformer

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.archive.simulator.simulation.BehaviorKeys
import no.novari.flyt.archive.simulator.simulation.BehaviorMode
import no.novari.flyt.archive.simulator.simulation.SimulatorBehaviorStore
import no.novari.flyt.archive.simulator.wiremock.InMemoryStore
import org.springframework.http.HttpStatus
import java.time.Duration

class JournalpostPutTransformer(
    private val store: InMemoryStore,
    private val behaviorStore: SimulatorBehaviorStore,
    private val defaultDelay: Duration,
) : ResponseTransformerV2 {
    private val logger = KotlinLogging.logger {}

    override fun getName(): String {
        return "journalpost-put"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

    override fun transform(
        response: Response,
        serveEvent: ServeEvent,
    ): Response {
        val request = serveEvent.request
        val behavior = behaviorStore.get(BehaviorKeys.JOURNALPOST_CREATE)
        val oneShot = behaviorStore.consumeOnce(BehaviorKeys.JOURNALPOST_CREATE)
        val effectiveBehavior =
            if (oneShot != null && behavior.mode == BehaviorMode.NORMAL) {
                oneShot
            } else {
                behavior
            }
        val summary = behaviorSummary(effectiveBehavior, defaultDelay)
        if (effectiveBehavior.mode == BehaviorMode.FAIL) {
            logger.info {
                "wiremock request method=${request.method} path=${request.url} behavior=$summary"
            }
            return applyBehavior(response, effectiveBehavior, defaultDelay)
        }

        val caseId =
            request.url
                .substringAfter("/arkiv/noark/sak/mappeid/")
        val statusId =
            if (caseId.isNotBlank()) {
                store.addJournalpost(caseId, request.bodyAsString)
            } else {
                null
            }

        val baseResponse =
            if (statusId != null) {
                Response.Builder
                    .like(response)
                    .but()
                    .status(HttpStatus.ACCEPTED.value())
                    .headers(withHeaders(response, header("Location", "/_status/sak/$statusId")))
                    .build()
            } else {
                Response.Builder
                    .like(response)
                    .but()
                    .status(HttpStatus.NOT_FOUND.value())
                    .build()
            }

        logger.info {
            "wiremock request method=${request.method} path=${request.url} behavior=$summary " +
                "caseId=$caseId statusId=$statusId"
        }

        return applyBehavior(baseResponse, effectiveBehavior, defaultDelay)
    }
}
