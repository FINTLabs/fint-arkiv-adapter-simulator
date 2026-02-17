package no.novari.flyt.archive.simulator.wiremock.transformer

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.archive.simulator.wiremock.InMemoryStore
import org.springframework.http.HttpStatus

class FileStatusTransformer(
    private val store: InMemoryStore,
) : ResponseTransformerV2 {
    private val logger = KotlinLogging.logger {}

    override fun getName(): String {
        return "file-status"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

    override fun transform(
        response: Response,
        serveEvent: ServeEvent,
    ): Response {
        val request = serveEvent.request
        val statusId = request.url.substringAfter("/_status/dokumentfil/")
        val fileId = store.resolveFileStatus(statusId)

        return if (fileId != null) {
            logger.info {
                "wiremock request method=${request.method} path=${request.url} behavior=default statusId=$statusId fileId=$fileId"
            }
            Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.CREATED.value())
                .headers(withHeaders(response, header("Location", "/arkiv/noark/dokumentfil/$fileId")))
                .build()
        } else {
            logger.info {
                "wiremock request method=${request.method} path=${request.url} behavior=default statusId=$statusId result=not_found"
            }
            Response.Builder
                .like(response)
                .but()
                .status(HttpStatus.NOT_FOUND.value())
                .build()
        }
    }
}
