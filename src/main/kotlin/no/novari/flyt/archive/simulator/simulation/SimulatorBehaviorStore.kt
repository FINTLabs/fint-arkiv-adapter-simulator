package no.novari.flyt.archive.simulator.simulation

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

enum class BehaviorMode {
    NORMAL,
    FAIL,
    TIMEOUT,
    EMPTY,
}

data class BehaviorConfig(
    val mode: BehaviorMode = BehaviorMode.NORMAL,
    val status: Int? = null,
    val body: String? = null,
    val delay: Duration? = null,
)

object BehaviorKeys {
    const val CASE_CREATE = "case-create"
    const val JOURNALPOST_CREATE = "journalpost-create"
    const val FILE_CREATE = "file-create"
    const val CASE_QUERY = "case-query"
    const val RESOURCE_PREFIX = "resource:"

    fun resource(path: String): String {
        return "$RESOURCE_PREFIX$path"
    }
}

class SimulatorBehaviorStore {
    private val behaviors = ConcurrentHashMap<String, BehaviorConfig>()
    private val oneShotBehaviors = ConcurrentHashMap<String, BehaviorConfig>()

    fun get(key: String): BehaviorConfig {
        return behaviors[key] ?: BehaviorConfig()
    }

    fun set(
        key: String,
        config: BehaviorConfig,
    ) {
        if (config.mode == BehaviorMode.NORMAL) {
            reset(key)
        } else {
            behaviors[key] = config
        }
    }

    fun reset(key: String) {
        behaviors.remove(key)
        oneShotBehaviors.remove(key)
    }

    fun resetAll() {
        behaviors.clear()
        oneShotBehaviors.clear()
    }

    fun snapshot(): Map<String, BehaviorConfig> {
        return behaviors.toSortedMap()
    }

    fun armTimeoutOnce(
        key: String,
        delay: Duration? = null,
    ) {
        oneShotBehaviors[key] = BehaviorConfig(mode = BehaviorMode.TIMEOUT, delay = delay)
    }

    fun armFailOnce(
        key: String,
        status: Int? = null,
        body: String? = null,
    ) {
        oneShotBehaviors[key] = BehaviorConfig(mode = BehaviorMode.FAIL, status = status, body = body)
    }

    fun consumeOnce(key: String): BehaviorConfig? {
        return oneShotBehaviors.remove(key)
    }
}
