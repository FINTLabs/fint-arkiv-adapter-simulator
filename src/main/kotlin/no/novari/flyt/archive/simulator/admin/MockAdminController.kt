package no.novari.flyt.archive.simulator.admin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.archive.simulator.simulation.BehaviorConfig
import no.novari.flyt.archive.simulator.simulation.BehaviorKeys
import no.novari.flyt.archive.simulator.simulation.BehaviorMode
import no.novari.flyt.archive.simulator.simulation.ResourceCatalog
import no.novari.flyt.archive.simulator.simulation.SimulatorBehaviorStore
import no.novari.flyt.archive.simulator.wiremock.InMemoryStore
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

@RestController
@RequestMapping("/internal/mock")
class MockAdminController(
    private val wireMockServer: WireMockServer,
    private val store: InMemoryStore,
    private val behaviorStore: SimulatorBehaviorStore,
    private val resourceCatalog: ResourceCatalog,
) {
    private val logger = KotlinLogging.logger {}

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetAll() {
        logger.info { "admin request action=reset-all" }
        store.reset()
        behaviorStore.resetAll()
        WireMock.configureFor("localhost", wireMockServer.port())
        WireMock.resetAllRequests()
    }

    @PostMapping("/arm-timeout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun armNextTimeout(
        @RequestParam(required = false) group: String?,
        @RequestParam(required = false) delay: Duration?,
    ) {
        val key = resolveOneShotKey(group)
        logger.info { "admin request action=arm-timeout target=$key delay=$delay" }
        behaviorStore.armTimeoutOnce(key, delay)
    }

    @PostMapping("/arm-fail")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun armNextFail(
        @RequestParam(required = false) group: String?,
        @RequestParam(required = false) status: Int?,
        @RequestParam(required = false) body: String?,
    ) {
        val key = resolveOneShotKey(group)
        logger.info { "admin request action=arm-fail target=$key status=$status bodyLength=${body?.length ?: 0}" }
        behaviorStore.armFailOnce(key, status, body)
    }

    @GetMapping("/behavior")
    fun getBehaviorSnapshot(): BehaviorSnapshotResponse {
        logger.info { "admin request action=get-behavior-snapshot" }
        return BehaviorSnapshotResponse(
            behaviors = behaviorStore.snapshot(),
            resources = resourceCatalog.definitions.associate { it.name to it.path },
        )
    }

    @PutMapping("/behavior")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setBehavior(
        @RequestBody request: BehaviorUpdateRequest,
    ) {
        logger.info {
            "admin request action=set-behavior group=${request.group} resource=${request.resource} " +
                "mode=${request.mode} status=${request.status} delay=${request.delay} " +
                "bodyLength=${request.body?.length ?: 0}"
        }
        val target = resolveBehaviorTarget(request.group, request.resource)
        behaviorStore.set(
            target.key,
            BehaviorConfig(
                mode = request.mode,
                status = request.status,
                body = request.body,
                delay = request.delay,
            ),
        )
        target.resourcePath?.let(store::touchResource)
    }

    @DeleteMapping("/behavior")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetBehavior(
        @RequestParam group: String,
        @RequestParam(required = false) resource: String?,
    ) {
        logger.info { "admin request action=reset-behavior group=$group resource=$resource" }
        val target = resolveBehaviorTarget(group, resource)
        behaviorStore.reset(target.key)
        target.resourcePath?.let(store::touchResource)
    }

    private fun resolveBehaviorTarget(
        group: String,
        resource: String?,
    ): BehaviorTarget =
        when (group.lowercase()) {
            "case", "case-create" -> {
                BehaviorTarget(BehaviorKeys.CASE_CREATE, null)
            }

            "journalpost", "journalpost-create" -> {
                BehaviorTarget(BehaviorKeys.JOURNALPOST_CREATE, null)
            }

            "file", "file-create" -> {
                BehaviorTarget(BehaviorKeys.FILE_CREATE, null)
            }

            "query", "case-query" -> {
                BehaviorTarget(BehaviorKeys.CASE_QUERY, null)
            }

            "resource", "resources" -> {
                val resourcePath =
                    resourceCatalog.resolvePath(resource ?: "")
                        ?: throw ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Unknown resource '${resource ?: ""}'. Use /internal/mock/behavior to list resources.",
                        )
                BehaviorTarget(BehaviorKeys.resource(resourcePath), resourcePath)
            }

            else -> {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown group '$group'. Use case, journalpost, file, query, or resource.",
                )
            }
        }

    private fun resolveOneShotKey(group: String?): String {
        val normalized =
            group
                ?.lowercase()
                ?.trim()
                .orEmpty()
                .ifBlank { "case" }
        return when (normalized) {
            "case", "case-create", "sak" -> BehaviorKeys.CASE_CREATE

            "journalpost", "journalpost-create" -> BehaviorKeys.JOURNALPOST_CREATE

            "file", "file-create", "dokumentfil" -> BehaviorKeys.FILE_CREATE

            else -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unknown group '$normalized'. Use case, journalpost, or file/dokumentfil.",
            )
        }
    }
}

data class BehaviorSnapshotResponse(
    val behaviors: Map<String, BehaviorConfig>,
    val resources: Map<String, String>,
)

data class BehaviorUpdateRequest(
    val group: String,
    val mode: BehaviorMode,
    val status: Int? = null,
    val body: String? = null,
    val delay: Duration? = null,
    val resource: String? = null,
)

data class BehaviorTarget(
    val key: String,
    val resourcePath: String?,
)
