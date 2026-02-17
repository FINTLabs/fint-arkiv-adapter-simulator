package no.novari.flyt.archive.simulator.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import no.novari.flyt.archive.simulator.simulation.ResourceCatalog
import no.novari.flyt.archive.simulator.simulation.SimulatorBehaviorStore
import no.novari.flyt.archive.simulator.wiremock.transformer.AdminUnitCollectionTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.ArkivdelCollectionTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.CaseGetTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.CaseQueryTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.CaseStatusTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.CreateCaseTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.CreateFileTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.FileStatusTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.JournalpostGetTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.JournalpostPutTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.LastUpdatedTransformer
import no.novari.flyt.archive.simulator.wiremock.transformer.ResourceCollectionTransformer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(SimulatorProperties::class)
class WireMockConfig(
    @param:Value("\${wiremock.port:9090}") private val wireMockPort: Int,
    private val properties: SimulatorProperties,
) {
    @Bean
    fun resourceCatalog(): ResourceCatalog {
        return ResourceCatalog()
    }

    @Bean
    fun behaviorStore(): SimulatorBehaviorStore {
        return SimulatorBehaviorStore()
    }

    @Bean
    fun store(resourceCatalog: ResourceCatalog): InMemoryStore {
        return InMemoryStore(resourceCatalog)
    }

    @Bean
    fun createCaseTransformer(
        store: InMemoryStore,
        behaviorStore: SimulatorBehaviorStore,
    ): CreateCaseTransformer {
        return CreateCaseTransformer(store, behaviorStore, defaultDelay())
    }

    @Bean
    fun createFileTransformer(
        store: InMemoryStore,
        behaviorStore: SimulatorBehaviorStore,
    ): CreateFileTransformer {
        return CreateFileTransformer(store, behaviorStore, defaultDelay())
    }

    @Bean
    fun fileStatusTransformer(store: InMemoryStore): FileStatusTransformer {
        return FileStatusTransformer(store)
    }

    @Bean
    fun caseStatusTransformer(store: InMemoryStore): CaseStatusTransformer {
        return CaseStatusTransformer(store)
    }

    @Bean
    fun caseGetTransformer(
        store: InMemoryStore,
        objectMapper: ObjectMapper,
    ): CaseGetTransformer {
        return CaseGetTransformer(store, objectMapper)
    }

    @Bean
    fun journalpostGetTransformer(
        store: InMemoryStore,
        objectMapper: ObjectMapper,
    ): JournalpostGetTransformer {
        return JournalpostGetTransformer(store, objectMapper)
    }

    @Bean
    fun lastUpdatedTransformer(store: InMemoryStore): LastUpdatedTransformer {
        return LastUpdatedTransformer(store)
    }

    @Bean
    fun adminUnitCollectionTransformer(
        store: InMemoryStore,
        behaviorStore: SimulatorBehaviorStore,
        objectMapper: ObjectMapper,
    ): AdminUnitCollectionTransformer {
        return AdminUnitCollectionTransformer(store, behaviorStore, defaultDelay(), objectMapper)
    }

    @Bean
    fun arkivdelCollectionTransformer(
        store: InMemoryStore,
        behaviorStore: SimulatorBehaviorStore,
        objectMapper: ObjectMapper,
    ): ArkivdelCollectionTransformer {
        return ArkivdelCollectionTransformer(store, behaviorStore, defaultDelay(), objectMapper)
    }

    @Bean
    fun resourceCollectionTransformer(
        store: InMemoryStore,
        behaviorStore: SimulatorBehaviorStore,
        resourceCatalog: ResourceCatalog,
        objectMapper: ObjectMapper,
    ): ResourceCollectionTransformer {
        return ResourceCollectionTransformer(store, behaviorStore, resourceCatalog, defaultDelay(), objectMapper)
    }

    @Bean
    fun caseQueryTransformer(
        store: InMemoryStore,
        behaviorStore: SimulatorBehaviorStore,
        objectMapper: ObjectMapper,
    ): CaseQueryTransformer {
        return CaseQueryTransformer(store, behaviorStore, defaultDelay(), objectMapper)
    }

    @Bean
    fun journalpostPutTransformer(
        store: InMemoryStore,
        behaviorStore: SimulatorBehaviorStore,
    ): JournalpostPutTransformer {
        return JournalpostPutTransformer(store, behaviorStore, defaultDelay())
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun wireMockServer(
        createCaseTransformer: CreateCaseTransformer,
        createFileTransformer: CreateFileTransformer,
        fileStatusTransformer: FileStatusTransformer,
        caseStatusTransformer: CaseStatusTransformer,
        caseGetTransformer: CaseGetTransformer,
        journalpostGetTransformer: JournalpostGetTransformer,
        lastUpdatedTransformer: LastUpdatedTransformer,
        adminUnitCollectionTransformer: AdminUnitCollectionTransformer,
        arkivdelCollectionTransformer: ArkivdelCollectionTransformer,
        resourceCollectionTransformer: ResourceCollectionTransformer,
        caseQueryTransformer: CaseQueryTransformer,
        journalpostPutTransformer: JournalpostPutTransformer,
    ): WireMockServer {
        return WireMockServer(
            wireMockConfig()
                .port(wireMockPort)
                .extensions(
                    createCaseTransformer,
                    createFileTransformer,
                    fileStatusTransformer,
                    caseStatusTransformer,
                    caseGetTransformer,
                    journalpostGetTransformer,
                    lastUpdatedTransformer,
                    adminUnitCollectionTransformer,
                    arkivdelCollectionTransformer,
                    resourceCollectionTransformer,
                    caseQueryTransformer,
                    journalpostPutTransformer,
                ),
        )
    }

    @Bean
    fun registerStubs(
        server: WireMockServer,
        resourceCatalog: ResourceCatalog,
    ): ApplicationRunner {
        return ApplicationRunner {
            server.stubFor(
                post(urlPathEqualTo("/arkiv/noark/sak"))
                    .willReturn(aResponse().withTransformers("create-case")),
            )

            server.stubFor(
                post(urlPathEqualTo("/arkiv/noark/dokumentfil"))
                    .willReturn(aResponse().withTransformers("create-file")),
            )

            server.stubFor(
                put(urlPathMatching("/arkiv/noark/sak/mappeid/.*"))
                    .willReturn(aResponse().withTransformers("journalpost-put")),
            )

            server.stubFor(
                post(urlPathEqualTo("/arkiv/noark/sak/\$query"))
                    .willReturn(aResponse().withTransformers("case-query")),
            )

            server.stubFor(
                get(urlPathMatching("/_status/sak/.*"))
                    .willReturn(aResponse().withTransformers("case-status")),
            )

            server.stubFor(
                get(urlPathMatching("/_status/dokumentfil/.*"))
                    .willReturn(aResponse().withTransformers("file-status")),
            )

            server.stubFor(
                get(urlPathMatching("/arkiv/noark/sak/.*/journalpost/.*"))
                    .atPriority(1)
                    .willReturn(aResponse().withTransformers("journalpost-get")),
            )

            server.stubFor(
                get(urlPathMatching("/arkiv/noark/sak/.*"))
                    .willReturn(aResponse().withTransformers("case-get")),
            )

            resourceCatalog.definitions.forEach { definition ->
                val collectionTransformer =
                    when (definition.path) {
                        "/arkiv/noark/administrativenhet" -> "admin-unit-collection"
                        "/arkiv/noark/arkivdel" -> "arkivdel-collection"
                        else -> "resource-collection"
                    }

                server.stubFor(
                    get(urlPathEqualTo("${definition.path}/last-updated"))
                        .willReturn(aResponse().withTransformers("last-updated")),
                )

                server.stubFor(
                    get(urlPathEqualTo(definition.path))
                        .withQueryParam("sinceTimeStamp", matching(".*"))
                        .willReturn(aResponse().withTransformers(collectionTransformer)),
                )
            }
        }
    }

    private fun defaultDelay(): Duration {
        return properties.postCaseTimeout.plus(properties.timeoutBuffer)
    }
}
