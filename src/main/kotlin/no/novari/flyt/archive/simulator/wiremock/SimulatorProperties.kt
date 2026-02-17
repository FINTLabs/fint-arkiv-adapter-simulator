package no.novari.flyt.archive.simulator.wiremock

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "simulator")
data class SimulatorProperties(
    var postCaseTimeout: Duration = Duration.ofSeconds(130),
    var timeoutBuffer: Duration = Duration.ofSeconds(5),
)
