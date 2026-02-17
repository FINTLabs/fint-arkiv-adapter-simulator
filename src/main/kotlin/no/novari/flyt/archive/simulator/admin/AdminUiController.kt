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
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets
import java.time.Duration

@Controller
@RequestMapping("/internal/ui")
class AdminUiController(
    private val store: InMemoryStore,
    private val behaviorStore: SimulatorBehaviorStore,
    private val resourceCatalog: ResourceCatalog,
    private val wireMockServer: WireMockServer,
) {
    private val logger = KotlinLogging.logger {}

    @GetMapping
    fun index(
        model: Model,
        @RequestParam(required = false) message: String?,
        @RequestParam(required = false) error: String?,
    ): String {
        val behaviorRows = behaviorStore.snapshot().map { (key, config) -> toBehaviorRow(key, config) }
        model.addAttribute("behaviorRows", behaviorRows)
        model.addAttribute("resources", resourceCatalog.definitions)
        model.addAttribute("modes", BehaviorMode.entries)
        model.addAttribute(
            "groups",
            listOf(
                GroupOption("case", "Sak"),
                GroupOption("journalpost", "Journalpost"),
                GroupOption("file", "Fil"),
                GroupOption("query", "Søk på sak"),
                GroupOption("resource", "Ressurs"),
            ),
        )
        model.addAttribute(
            "armGroups",
            listOf(
                GroupOption("case", "Sak"),
                GroupOption("journalpost", "Journalpost"),
                GroupOption("file", "Fil"),
            ),
        )
        model.addAttribute("message", message)
        model.addAttribute("error", error)
        return "internal/index"
    }

    @PostMapping("/reset")
    fun resetAll(): String {
        logger.info { "admin-ui action=reset-all" }
        store.reset()
        behaviorStore.resetAll()
        WireMock.configureFor("localhost", wireMockServer.port())
        WireMock.resetAllRequests()
        return redirectWithMessage("Tilbakestilt: state og overstyringer er nullstilt.")
    }

    @PostMapping("/arm-timeout")
    fun armTimeout(
        @RequestParam group: String,
        @RequestParam(required = false) delay: String?,
    ): String {
        val key =
            resolveOneShotKey(group)
                ?: return redirectWithError("Ugyldig gruppe for engangs-timeout.")
        val delayValue =
            delay
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    runCatching { Duration.parse(it) }.getOrNull()
                        ?: return redirectWithError("Delay må være ISO-8601, f.eks. PT5S.")
                }
        logger.info { "admin-ui action=arm-timeout target=$key delay=$delayValue" }
        behaviorStore.armTimeoutOnce(key, delayValue)
        return redirectWithMessage("Neste kall for $group vil time ut.")
    }

    @PostMapping("/arm-fail")
    fun armFail(
        @RequestParam group: String,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) body: String?,
    ): String {
        val key =
            resolveOneShotKey(group)
                ?: return redirectWithError("Ugyldig gruppe for engangs-feil.")
        val statusValue =
            status
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.toIntOrNull()
        if (status?.isNotBlank() == true && statusValue == null) {
            return redirectWithError("Status må være et tall.")
        }
        logger.info { "admin-ui action=arm-fail target=$key status=$statusValue bodyLength=${body?.length ?: 0}" }
        behaviorStore.armFailOnce(key, statusValue, body?.takeIf { it.isNotBlank() })
        return redirectWithMessage("Neste kall for $group vil feile.")
    }

    @PostMapping("/behavior")
    fun setBehavior(
        @RequestParam group: String,
        @RequestParam mode: BehaviorMode,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) body: String?,
        @RequestParam(required = false) delay: String?,
        @RequestParam(required = false) resource: String?,
    ): String {
        val target =
            resolveBehaviorTarget(group, resource)
                ?: return redirectWithError("Ugyldig gruppe/ressurs. Bruk resource + ressursnavn.")

        val statusValue =
            status
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.toIntOrNull()
        if (status?.isNotBlank() == true && statusValue == null) {
            return redirectWithError("Status må være et tall.")
        }

        val delayValue =
            delay
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    runCatching { Duration.parse(it) }.getOrNull()
                        ?: return redirectWithError("Delay må være ISO-8601, f.eks. PT5S.")
                }

        behaviorStore.set(
            target.key,
            BehaviorConfig(
                mode = mode,
                status = statusValue,
                body = body?.takeIf { it.isNotBlank() },
                delay = delayValue,
            ),
        )
        target.resourcePath?.let(store::touchResource)
        logger.info {
            "admin-ui action=set-behavior group=$group resource=${target.resourcePath ?: "-"} mode=$mode status=$statusValue"
        }
        return redirectWithMessage("Oppførsel oppdatert for $group.")
    }

    @PostMapping("/behavior/reset")
    fun resetBehavior(
        @RequestParam group: String,
        @RequestParam(required = false) resource: String?,
    ): String {
        val target =
            resolveBehaviorTarget(group, resource)
                ?: return redirectWithError("Ugyldig gruppe/ressurs.")
        behaviorStore.reset(target.key)
        target.resourcePath?.let(store::touchResource)
        logger.info { "admin-ui action=reset-behavior group=$group resource=${target.resourcePath ?: "-"}" }
        return redirectWithMessage("Oppførsel resatt for ${groupLabel(group)}.")
    }

    private fun resolveBehaviorTarget(
        group: String,
        resource: String?,
    ): BehaviorTarget? {
        return when (group.lowercase()) {
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
                val resourcePath = resourceCatalog.resolvePath(resource ?: "")
                if (resourcePath == null) {
                    null
                } else {
                    BehaviorTarget(BehaviorKeys.resource(resourcePath), resourcePath)
                }
            }

            else -> {
                null
            }
        }
    }

    private fun resolveOneShotKey(group: String): String? {
        return when (group.lowercase()) {
            "case", "case-create", "sak" -> BehaviorKeys.CASE_CREATE
            "journalpost", "journalpost-create" -> BehaviorKeys.JOURNALPOST_CREATE
            "file", "file-create", "dokumentfil" -> BehaviorKeys.FILE_CREATE
            else -> null
        }
    }

    private fun toBehaviorRow(
        key: String,
        config: BehaviorConfig,
    ): BehaviorRow {
        return if (key.startsWith(BehaviorKeys.RESOURCE_PREFIX)) {
            val path = key.removePrefix(BehaviorKeys.RESOURCE_PREFIX)
            val name = resourceCatalog.definitions.firstOrNull { it.path == path }?.name
            BehaviorRow(
                key = key,
                mode = config.mode,
                status = config.status,
                delay = config.delay,
                body = config.body,
                group = "resource",
                groupLabel = "Ressurs",
                resourceLabel = name?.let { "$it ($path)" } ?: path,
                resourceValue = path,
            )
        } else {
            val group =
                when (key) {
                    BehaviorKeys.CASE_CREATE -> "case"
                    BehaviorKeys.JOURNALPOST_CREATE -> "journalpost"
                    BehaviorKeys.FILE_CREATE -> "file"
                    BehaviorKeys.CASE_QUERY -> "query"
                    else -> key
                }
            BehaviorRow(
                key = key,
                mode = config.mode,
                status = config.status,
                delay = config.delay,
                body = config.body,
                group = group,
                groupLabel = groupLabel(group),
                resourceLabel = null,
                resourceValue = null,
            )
        }
    }

    private fun groupLabel(group: String): String {
        return when (group) {
            "case" -> "Sak"
            "journalpost" -> "Journalpost"
            "file" -> "Fil"
            "query" -> "Søk på sak"
            "resource" -> "Ressurs"
            else -> group
        }
    }

    private fun redirectWithMessage(message: String): String {
        return "redirect:/internal/ui?message=${encode(message)}"
    }

    private fun redirectWithError(message: String): String {
        return "redirect:/internal/ui?error=${encode(message)}"
    }

    private fun encode(value: String): String {
        return UriUtils.encode(value, StandardCharsets.UTF_8)
    }
}

data class BehaviorRow(
    val key: String,
    val mode: BehaviorMode,
    val status: Int?,
    val delay: Duration?,
    val body: String?,
    val group: String,
    val groupLabel: String,
    val resourceLabel: String?,
    val resourceValue: String?,
)

data class GroupOption(
    val value: String,
    val label: String,
)
