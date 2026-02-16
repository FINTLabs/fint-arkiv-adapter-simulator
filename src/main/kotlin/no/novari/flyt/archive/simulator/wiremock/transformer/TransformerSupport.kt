package no.novari.flyt.archive.simulator.wiremock.transformer

import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Response
import no.novari.flyt.archive.simulator.simulation.BehaviorConfig
import no.novari.flyt.archive.simulator.simulation.BehaviorMode
import org.springframework.http.HttpStatus
import java.time.Duration

fun applyBehavior(
    baseResponse: Response,
    behavior: BehaviorConfig,
    defaultDelay: Duration,
    emptyResponse: (() -> Response)? = null,
): Response {
    return when (behavior.mode) {
        BehaviorMode.NORMAL -> {
            baseResponse
        }

        BehaviorMode.EMPTY -> {
            emptyResponse?.invoke() ?: baseResponse
        }

        BehaviorMode.FAIL -> {
            Response.Builder
                .like(baseResponse)
                .but()
                .status(behavior.status ?: HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(behavior.body ?: "")
                .build()
        }

        BehaviorMode.TIMEOUT -> {
            Response.Builder
                .like(baseResponse)
                .but()
                .incrementInitialDelay(behavior.delay?.toMillis() ?: defaultDelay.toMillis())
                .build()
        }
    }
}

fun behaviorSummary(
    behavior: BehaviorConfig,
    defaultDelay: Duration,
): String {
    return when (behavior.mode) {
        BehaviorMode.NORMAL -> {
            "default"
        }

        BehaviorMode.FAIL -> {
            val status = behavior.status ?: HttpStatus.INTERNAL_SERVER_ERROR.value()
            val bodyLength = behavior.body?.length ?: 0
            "mocked:fail status=$status bodyLength=$bodyLength"
        }

        BehaviorMode.TIMEOUT -> {
            val delay = behavior.delay ?: defaultDelay
            "mocked:timeout delay=$delay"
        }

        BehaviorMode.EMPTY -> {
            "mocked:empty"
        }
    }
}

fun header(
    name: String,
    value: String,
): HttpHeader {
    return HttpHeader.httpHeader(name, value)
}

fun withHeaders(
    response: Response,
    vararg headers: HttpHeader,
): HttpHeaders {
    return HttpHeaders.copyOf(response.headers).plus(*headers)
}
