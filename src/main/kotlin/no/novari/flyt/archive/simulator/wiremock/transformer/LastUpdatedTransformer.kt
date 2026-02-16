package no.novari.flyt.archive.simulator.wiremock.transformer

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.archive.simulator.wiremock.InMemoryStore
import org.springframework.http.HttpStatus

class LastUpdatedTransformer(
    private val store: InMemoryStore,
) : ResponseTransformerV2 {
    private val logger = KotlinLogging.logger {}

    override fun getName(): String {
        return "last-updated"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

    override fun transform(
        response: Response,
        serveEvent: ServeEvent,
    ): Response {
        val request = serveEvent.request
        val path = request.url.substringBefore("/last-updated")
        val body = """{"lastUpdated":${store.getLastUpdated(path)}}"""
        logger.info {
            "wiremock request method=${request.method} path=${request.url} behavior=default resourcePath=$path"
        }
        return Response.Builder
            .like(response)
            .but()
            .status(HttpStatus.OK.value())
            .headers(withHeaders(response, header("Content-Type", "application/json")))
            .body(body)
            .build()
    }
}
