package no.novari.flyt.archive.simulator.simulation

import no.fint.model.felles.basisklasser.Begrep
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Personnavn
import no.fint.model.resource.FintLinks
import no.fint.model.resource.Link
import no.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.fint.model.resource.arkiv.kodeverk.DokumentStatusResource
import no.fint.model.resource.arkiv.kodeverk.DokumentTypeResource
import no.fint.model.resource.arkiv.kodeverk.FormatResource
import no.fint.model.resource.arkiv.kodeverk.JournalStatusResource
import no.fint.model.resource.arkiv.kodeverk.JournalpostTypeResource
import no.fint.model.resource.arkiv.kodeverk.KorrespondansepartTypeResource
import no.fint.model.resource.arkiv.kodeverk.PartRolleResource
import no.fint.model.resource.arkiv.kodeverk.SaksmappetypeResource
import no.fint.model.resource.arkiv.kodeverk.SaksstatusResource
import no.fint.model.resource.arkiv.kodeverk.SkjermingshjemmelResource
import no.fint.model.resource.arkiv.kodeverk.TilgangsgruppeResource
import no.fint.model.resource.arkiv.kodeverk.TilgangsrestriksjonResource
import no.fint.model.resource.arkiv.kodeverk.TilknyttetRegistreringSomResource
import no.fint.model.resource.arkiv.kodeverk.VariantformatResource
import no.fint.model.resource.arkiv.noark.AdministrativEnhetResource
import no.fint.model.resource.arkiv.noark.ArkivdelResource
import no.fint.model.resource.arkiv.noark.ArkivressursResource
import no.fint.model.resource.arkiv.noark.KlassifikasjonssystemResource
import no.fint.model.resource.felles.PersonResource

data class ResourceDefinition<T : FintLinks>(
    val name: String,
    val path: String,
    val items: List<T>,
)

class ResourceCatalog {
    val definitions: List<ResourceDefinition<out FintLinks>> =
        listOf(
            resourceDefinition(
                name = "administrativenhet",
                path = "/arkiv/noark/administrativenhet",
                items =
                    listOf(
                        administrativEnhet("adm-1", "Skole og oppvekst"),
                        administrativEnhet("adm-2", "Plan og bygg"),
                    ),
            ),
            resourceDefinition(
                name = "klassifikasjonssystem",
                path = "/arkiv/noark/klassifikasjonssystem",
                items =
                    listOf(
                        klassifikasjonssystem("KSS-1", "Primær klassifikasjon"),
                        klassifikasjonssystem("KSS-2", "Sekundær klassifikasjon"),
                    ),
            ),
            resourceDefinition(
                name = "arkivdel",
                path = "/arkiv/noark/arkivdel",
                items =
                    listOf(
                        arkivdel("ARK-1", "Saksarkiv"),
                        arkivdel("ARK-2", "Elevarkiv"),
                    ),
            ),
            resourceDefinition(
                name = "arkivressurs",
                path = "/arkiv/noark/arkivressurs",
                items =
                    listOf(
                        arkivressurs("AR-1", personalressursId = "PR-100"),
                        arkivressurs("AR-2", personalressursId = "PR-200"),
                    ),
            ),
            resourceDefinition(
                name = "partrolle",
                path = "/arkiv/kodeverk/partrolle",
                items =
                    listOf(
                        begrepResource(PartRolleResource(), "/arkiv/kodeverk/partrolle", "PR-1", "SOKER", "Søker"),
                        begrepResource(
                            PartRolleResource(),
                            "/arkiv/kodeverk/partrolle",
                            "PR-2",
                            "MOTTAKER",
                            "Mottaker",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "korrespondanseparttype",
                path = "/arkiv/kodeverk/korrespondanseparttype",
                items =
                    listOf(
                        begrepResource(
                            KorrespondansepartTypeResource(),
                            "/arkiv/kodeverk/korrespondanseparttype",
                            "KPT-1",
                            "AVS",
                            "Avsender",
                        ),
                        begrepResource(
                            KorrespondansepartTypeResource(),
                            "/arkiv/kodeverk/korrespondanseparttype",
                            "KPT-2",
                            "MOT",
                            "Mottaker",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "saksstatus",
                path = "/arkiv/kodeverk/saksstatus",
                items =
                    listOf(
                        begrepResource(
                            SaksstatusResource(),
                            "/arkiv/kodeverk/saksstatus",
                            "SAK-1",
                            "OPPRETTET",
                            "Opprettet",
                        ),
                        begrepResource(
                            SaksstatusResource(),
                            "/arkiv/kodeverk/saksstatus",
                            "SAK-2",
                            "AVSLUTTET",
                            "Avsluttet",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "skjermingshjemmel",
                path = "/arkiv/kodeverk/skjermingshjemmel",
                items =
                    listOf(
                        begrepResource(
                            SkjermingshjemmelResource(),
                            "/arkiv/kodeverk/skjermingshjemmel",
                            "SH-1",
                            "13",
                            "Offl. §13",
                        ),
                        begrepResource(
                            SkjermingshjemmelResource(),
                            "/arkiv/kodeverk/skjermingshjemmel",
                            "SH-2",
                            "23",
                            "Offl. §23",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "tilgangsrestriksjon",
                path = "/arkiv/kodeverk/tilgangsrestriksjon",
                items =
                    listOf(
                        begrepResource(
                            TilgangsrestriksjonResource(),
                            "/arkiv/kodeverk/tilgangsrestriksjon",
                            "TR-1",
                            "U",
                            "Unntatt",
                        ),
                        begrepResource(
                            TilgangsrestriksjonResource(),
                            "/arkiv/kodeverk/tilgangsrestriksjon",
                            "TR-2",
                            "B",
                            "Begrenset",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "dokumentstatus",
                path = "/arkiv/kodeverk/dokumentstatus",
                items =
                    listOf(
                        begrepResource(
                            DokumentStatusResource(),
                            "/arkiv/kodeverk/dokumentstatus",
                            "DS-1",
                            "F",
                            "Ferdigstilt",
                        ),
                        begrepResource(
                            DokumentStatusResource(),
                            "/arkiv/kodeverk/dokumentstatus",
                            "DS-2",
                            "U",
                            "Under arbeid",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "dokumenttype",
                path = "/arkiv/kodeverk/dokumenttype",
                items =
                    listOf(
                        begrepResource(
                            DokumentTypeResource(),
                            "/arkiv/kodeverk/dokumenttype",
                            "DT-1",
                            "B",
                            "Brev",
                        ),
                        begrepResource(
                            DokumentTypeResource(),
                            "/arkiv/kodeverk/dokumenttype",
                            "DT-2",
                            "N",
                            "Notat",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "journalposttype",
                path = "/arkiv/kodeverk/journalposttype",
                items =
                    listOf(
                        begrepResource(
                            JournalpostTypeResource(),
                            "/arkiv/kodeverk/journalposttype",
                            "JT-1",
                            "I",
                            "Inngående",
                        ),
                        begrepResource(
                            JournalpostTypeResource(),
                            "/arkiv/kodeverk/journalposttype",
                            "JT-2",
                            "U",
                            "Utgående",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "journalstatus",
                path = "/arkiv/kodeverk/journalstatus",
                items =
                    listOf(
                        begrepResource(
                            JournalStatusResource(),
                            "/arkiv/kodeverk/journalstatus",
                            "JS-1",
                            "F",
                            "Ferdigstilt",
                        ),
                        begrepResource(
                            JournalStatusResource(),
                            "/arkiv/kodeverk/journalstatus",
                            "JS-2",
                            "J",
                            "Journalført",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "variantformat",
                path = "/arkiv/kodeverk/variantformat",
                items =
                    listOf(
                        begrepResource(
                            VariantformatResource(),
                            "/arkiv/kodeverk/variantformat",
                            "VF-1",
                            "A",
                            "Arkiv",
                        ),
                        begrepResource(
                            VariantformatResource(),
                            "/arkiv/kodeverk/variantformat",
                            "VF-2",
                            "P",
                            "Produksjon",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "format",
                path = "/arkiv/kodeverk/format",
                items =
                    listOf(
                        begrepResource(
                            FormatResource(),
                            "/arkiv/kodeverk/format",
                            "F-1",
                            "PDF",
                            "PDF",
                        ),
                        begrepResource(
                            FormatResource(),
                            "/arkiv/kodeverk/format",
                            "F-2",
                            "XML",
                            "XML",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "tilgangsgruppe",
                path = "/arkiv/kodeverk/tilgangsgruppe",
                items =
                    listOf(
                        begrepResource(
                            TilgangsgruppeResource(),
                            "/arkiv/kodeverk/tilgangsgruppe",
                            "TG-1",
                            "SENS",
                            "Sentrale",
                        ),
                        begrepResource(
                            TilgangsgruppeResource(),
                            "/arkiv/kodeverk/tilgangsgruppe",
                            "TG-2",
                            "ALL",
                            "Alle",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "saksmappetype",
                path = "/arkiv/kodeverk/saksmappetype",
                items =
                    listOf(
                        begrepResource(
                            SaksmappetypeResource(),
                            "/arkiv/kodeverk/saksmappetype",
                            "SM-1",
                            "GENERELL",
                            "Generell",
                        ),
                        begrepResource(
                            SaksmappetypeResource(),
                            "/arkiv/kodeverk/saksmappetype",
                            "SM-2",
                            "ELEV",
                            "Elevmappe",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "tilknyttetregistreringsom",
                path = "/arkiv/kodeverk/tilknyttetregistreringsom",
                items =
                    listOf(
                        begrepResource(
                            TilknyttetRegistreringSomResource(),
                            "/arkiv/kodeverk/tilknyttetregistreringsom",
                            "TRO-1",
                            "SAK",
                            "Sak",
                        ),
                        begrepResource(
                            TilknyttetRegistreringSomResource(),
                            "/arkiv/kodeverk/tilknyttetregistreringsom",
                            "TRO-2",
                            "MOTE",
                            "Møte",
                        ),
                    ),
            ),
            resourceDefinition(
                name = "personalressurs",
                path = "/administrasjon/personal/personalressurs",
                items =
                    listOf(
                        personalressurs("PR-100", "ola", personId = "P-100"),
                        personalressurs("PR-200", "kari", personId = "P-200"),
                    ),
            ),
            resourceDefinition(
                name = "person",
                path = "/administrasjon/personal/person",
                items =
                    listOf(
                        person("P-100", "Ola", "Nordmann"),
                        person("P-200", "Kari", "Nordmann"),
                    ),
            ),
        )

    private val byName = definitions.associateBy { it.name }
    private val byPath = definitions.associateBy { it.path }

    fun resolvePath(resource: String): String? {
        if (resource.startsWith("/")) {
            return byPath[resource]?.path
        }
        return byName[resource]?.path
    }

    fun isKnownPath(path: String): Boolean {
        return byPath.containsKey(path)
    }

    private fun <T : FintLinks> resourceDefinition(
        name: String,
        path: String,
        items: List<T>,
    ): ResourceDefinition<T> {
        return ResourceDefinition(
            name = name,
            path = path,
            items = items,
        )
    }

    private fun administrativEnhet(
        id: String,
        navn: String,
    ): AdministrativEnhetResource {
        val resource = AdministrativEnhetResource()
        resource.systemId = identifikator(id)
        resource.navn = navn
        return withSelf(resource, "/arkiv/noark/administrativenhet", id)
    }

    private fun klassifikasjonssystem(
        id: String,
        tittel: String,
    ): KlassifikasjonssystemResource {
        val resource = KlassifikasjonssystemResource()
        resource.systemId = identifikator(id)
        resource.tittel = tittel
        return withSelf(resource, "/arkiv/noark/klassifikasjonssystem", id)
    }

    private fun arkivdel(
        id: String,
        tittel: String,
    ): ArkivdelResource {
        val resource = ArkivdelResource()
        resource.systemId = identifikator(id)
        resource.tittel = tittel
        return withSelf(resource, "/arkiv/noark/arkivdel", id)
    }

    private fun arkivressurs(
        id: String,
        personalressursId: String,
    ): ArkivressursResource {
        val resource = ArkivressursResource()
        resource.systemId = identifikator(id)
        resource.addPersonalressurs(Link.with("/administrasjon/personal/personalressurs/$personalressursId"))
        return withSelf(resource, "/arkiv/noark/arkivressurs", id)
    }

    private fun personalressurs(
        id: String,
        brukernavn: String,
        personId: String,
    ): PersonalressursResource {
        val resource = PersonalressursResource()
        resource.systemId = identifikator(id)
        resource.brukernavn = identifikator(brukernavn)
        resource.addPerson(Link.with("/administrasjon/personal/person/$personId"))
        return withSelf(resource, "/administrasjon/personal/personalressurs", id)
    }

    private fun person(
        id: String,
        fornavn: String,
        etternavn: String,
    ): PersonResource {
        val resource = PersonResource()
        val navn = Personnavn()
        navn.fornavn = fornavn
        navn.etternavn = etternavn
        resource.navn = navn
        return withSelf(resource, "/administrasjon/personal/person", id)
    }

    private fun <T> begrepResource(
        resource: T,
        path: String,
        id: String,
        kode: String,
        navn: String,
    ): T where T : Begrep, T : FintLinks {
        resource.systemId = identifikator(id)
        resource.kode = kode
        resource.navn = navn
        resource.addSelf(Link.with("$path/$id"))
        return resource
    }

    private fun identifikator(value: String): Identifikator {
        val identifikator = Identifikator()
        identifikator.identifikatorverdi = value
        return identifikator
    }

    private fun <T : FintLinks> withSelf(
        resource: T,
        path: String,
        id: String,
    ): T {
        resource.addSelf(Link.with("$path/$id"))
        return resource
    }
}
