# Archive Adapter Simulator

Spring Boot + WireMock‑simulator for archive adapter‑APIet som brukes av `fint-flyt-archive-gateway`.

**Porter**
- `8080`: Spring Boot (actuator + interne/admin‑endepunkter)
- `9090`: WireMock (archive adapter‑endepunkter)

**Kjør**
```bash
./gradlew bootRun
```

**Oversikt over oppførsel**
- `POST /arkiv/noark/sak` og `PUT /arkiv/noark/sak/mappeid/{caseId}` returnerer `202 Accepted` med `Location` til et status‑endepunkt.
- Poll status‑endepunktet til du får `201 Created` med `Location` til selve ressursen.
- Sak‑IDer genereres som `YYYY/sekvens` (eksempel: `2026/1`).
- `POST /arkiv/noark/sak/$query` støtter OData‑filter slik gatewayen bruker (kun `eq` + `and`).

**Hardkodede data og hvor de endres**
- Statiske ressurser/kodeverk: `/Users/janovekongshaug/Repositories/fint-arkiv-adapter-simulator/src/main/kotlin/no/novari/flyt/archive/simulator/simulation/ResourceCatalog.kt` (oppdater `definitions`‑listen; hver entry bruker FINT‑modellklasser og legger til `_links.self`).
- Sak, journalpost og fil‑oppførsel: `/Users/janovekongshaug/Repositories/fint-arkiv-adapter-simulator/src/main/kotlin/no/novari/flyt/archive/simulator/wiremock/InMemoryStore.kt` (default‑titler, journalpost‑nummerering, mapping fra request‑JSON).
- OData‑filter (søk på sak): `/Users/janovekongshaug/Repositories/fint-arkiv-adapter-simulator/src/main/kotlin/no/novari/flyt/archive/simulator/simulation/CaseFilterSupport.kt`.
- Runtime‑konfigurasjon: `/Users/janovekongshaug/Repositories/fint-arkiv-adapter-simulator/src/main/resources/application.yaml` (timeouts styres av `simulator.post-case-timeout` og `simulator.timeout-buffer`).

**Opprett sak (end‑to‑end)**
1. Opprett sak (samme format som gatewayen sender, med `_links`):
```bash
curl -i -X POST http://localhost:9090/arkiv/noark/sak \
  -H "Content-Type: application/json" \
  -d '{
        "tittel": "Test-sak",
        "_links": {
          "administrativenhet": [{ "href": "/arkiv/noark/administrativenhet/adm-1" }],
          "arkivdel": [{ "href": "/arkiv/noark/arkivdel/ARK-1" }],
          "saksmappetype": [{ "href": "/arkiv/kodeverk/saksmappetype/SM-1" }],
          "saksstatus": [{ "href": "/arkiv/kodeverk/saksstatus/SAK-1" }]
        }
      }'
```
2. Poll status:
```bash
curl -i http://localhost:9090/_status/sak/<statusId>
```
3. Hent sak:
```bash
curl -i http://localhost:9090/arkiv/noark/sak/2026/1
```

**Legg til journalpost på sak**
Simulatoren forventer samme wrapper som gatewayen sender:
```bash
curl -i -X PUT "http://localhost:9090/arkiv/noark/sak/mappeid/2026/1" \
  -H "Content-Type: application/json" \
  -d '{
        "journalpost": [
          {
            "tittel": "Inngående brev",
            "offentligTittel": "Brev",
            "journalposttype": [{ "href": "/arkiv/kodeverk/journalposttype/JT-1" }],
            "journalstatus": [{ "href": "/arkiv/kodeverk/journalstatus/JS-1" }],
            "tilgangsgruppe": [{ "href": "/arkiv/kodeverk/tilgangsgruppe/TG-1" }],
            "saksbehandler": [{ "href": "/arkiv/noark/arkivressurs/AR-1" }],
            "administrativEnhet": [{ "href": "/arkiv/noark/administrativenhet/adm-1" }]
          }
        ]
      }'
```
Deretter:
```bash
curl -i http://localhost:9090/_status/sak/<statusId>
curl -i http://localhost:9090/arkiv/noark/sak/2026/1
```

**Hent én journalpost**
```bash
curl -i http://localhost:9090/arkiv/noark/sak/2026/1/journalpost/1
```

**Filopplasting og kobling til journalpost**
1. Opprett fil:
```bash
curl -i -X POST http://localhost:9090/arkiv/noark/dokumentfil \
  -H "Content-Disposition: attachment; filename=test.pdf" \
  --data-binary "Hei Novari"
```
2. Poll status:
```bash
curl -i http://localhost:9090/_status/dokumentfil/<statusId>
```
Du får `Location: /arkiv/noark/dokumentfil/<fileId>`.

3. Knytt filen til journalpost ved å referere til dokumentfilen i `dokumentobjekt`:
```bash
curl -i -X PUT "http://localhost:9090/arkiv/noark/sak/mappeid/2026/1" \
  -H "Content-Type: application/json" \
  -d '{
        "journalpost": [
          {
            "tittel": "Inngående brev",
            "_links": {
              "journalposttype": [{ "href": "/arkiv/kodeverk/journalposttype/JT-1" }],
              "journalstatus": [{ "href": "/arkiv/kodeverk/journalstatus/JS-1" }],
              "tilgangsgruppe": [{ "href": "/arkiv/kodeverk/tilgangsgruppe/TG-1" }],
              "saksbehandler": [{ "href": "/arkiv/noark/arkivressurs/AR-1" }],
              "administrativenhet": [{ "href": "/arkiv/noark/administrativenhet/adm-1" }]
            },
            "dokumentbeskrivelse": [
              {
                "tittel": "Dokument 1",
                "_links": {
                  "dokumenttype": [{ "href": "/arkiv/kodeverk/dokumenttype/DT-1" }],
                  "dokumentstatus": [{ "href": "/arkiv/kodeverk/dokumentstatus/DS-1" }]
                },
                "dokumentobjekt": [
                  {
                    "_links": {
                      "referanseDokumentfil": [{ "href": "/arkiv/noark/dokumentfil/<fileId>" }],
                      "variantformat": [{ "href": "/arkiv/kodeverk/variantformat/VF-1" }],
                      "filformat": [{ "href": "/arkiv/kodeverk/format/F-1" }]
                    }
                  }
                ]
              }
            ]
          }
        ]
      }'
```

**Søk på Sak**
Gatewayen sender OData‑filter som `text/plain` i body til `/arkiv/noark/sak/$query`.
Simulatoren støtter kun `eq` + `and` for feltene gatewayen faktisk bruker.

Støttede felter:
- `arkivdel`
- `administrativenhet`
- `tilgangskode`
- `saksmappetype`
- `saksstatus`
- `tittel`
- `klassifikasjon/primar/ordning`
- `klassifikasjon/primar/verdi`
- `klassifikasjon/sekundar/ordning`
- `klassifikasjon/sekundar/verdi`
- `klassifikasjon/tertiar/ordning`
- `klassifikasjon/tertiar/verdi`

Eksempler:
```bash
curl -i -X POST "http://localhost:9090/arkiv/noark/sak/\$query" \
  -H "Content-Type: text/plain" \
  -d "saksstatus eq 'SAK-1'"
```

```bash
curl -i -X POST "http://localhost:9090/arkiv/noark/sak/\$query" \
  -H "Content-Type: text/plain" \
  -d "administrativenhet eq 'adm-1' and arkivdel eq 'ARK-1'"
```

```bash
curl -i -X POST "http://localhost:9090/arkiv/noark/sak/\$query" \
  -H "Content-Type: text/plain" \
  -d "klassifikasjon/primar/ordning eq 'KSS-1' and klassifikasjon/primar/verdi eq 'A-100'"
```

**Ressurs/kodeverk‑endepunkter**
Alle ressurser krever `sinceTimeStamp` på collection‑oppslag.
```bash
curl -i "http://localhost:9090/arkiv/noark/arkivdel?sinceTimeStamp=0"
curl -i "http://localhost:9090/arkiv/noark/arkivdel/last-updated"
```

Ressurs‑paths:
- `/arkiv/noark/administrativenhet`
- `/arkiv/noark/klassifikasjonssystem`
- `/arkiv/noark/arkivdel`
- `/arkiv/noark/arkivressurs`
- `/arkiv/kodeverk/partrolle`
- `/arkiv/kodeverk/korrespondanseparttype`
- `/arkiv/kodeverk/saksstatus`
- `/arkiv/kodeverk/skjermingshjemmel`
- `/arkiv/kodeverk/tilgangsrestriksjon`
- `/arkiv/kodeverk/dokumentstatus`
- `/arkiv/kodeverk/dokumenttype`
- `/arkiv/kodeverk/journalposttype`
- `/arkiv/kodeverk/journalstatus`
- `/arkiv/kodeverk/variantformat`
- `/arkiv/kodeverk/format`
- `/arkiv/kodeverk/tilgangsgruppe`
- `/arkiv/kodeverk/saksmappetype`
- `/arkiv/kodeverk/tilknyttetregistreringsom`
- `/administrasjon/personal/personalressurs`
- `/administrasjon/personal/person`

**Admin‑endepunkter (instrumentering)**
Alle admin‑endepunkter ligger på port `8080`.

Web‑UI for admin:
- `http://localhost:8080/internal/ui`

Hent aktive overstyringer og liste over ressursnavn/paths:
```bash
curl -i http://localhost:8080/internal/mock/behavior
```

Reset alt (state + overstyringer):
```bash
curl -i -X POST http://localhost:8080/internal/mock/reset
```

**Engangs‑timeout (neste POST /arkiv/noark/sak)**
```bash
curl -i -X POST http://localhost:8080/internal/mock/arm-timeout
```

**Engangs‑timeout for journalpost eller fil**
```bash
curl -i -X POST "http://localhost:8080/internal/mock/arm-timeout?group=journalpost"
curl -i -X POST "http://localhost:8080/internal/mock/arm-timeout?group=file"
```

Med egendefinert delay:
```bash
curl -i -X POST "http://localhost:8080/internal/mock/arm-timeout?group=journalpost&delay=PT10S"
```

**Engangs‑feil (neste kall)**
```bash
curl -i -X POST "http://localhost:8080/internal/mock/arm-fail?group=case&status=500&body=Simulert%20feil"
curl -i -X POST "http://localhost:8080/internal/mock/arm-fail?group=journalpost&status=503"
curl -i -X POST "http://localhost:8080/internal/mock/arm-fail?group=file&status=500"
```

**Sett oppførsel fra og med nå**
Alle requests bruker JSON:
```bash
curl -i -X PUT http://localhost:8080/internal/mock/behavior \
  -H "Content-Type: application/json" \
  -d '{"group":"case","mode":"FAIL","status":500,"body":"Simulated failure"}'
```

```bash
curl -i -X PUT http://localhost:8080/internal/mock/behavior \
  -H "Content-Type: application/json" \
  -d '{"group":"journalpost","mode":"TIMEOUT","delay":"PT30S"}'
```

```bash
curl -i -X PUT http://localhost:8080/internal/mock/behavior \
  -H "Content-Type: application/json" \
  -d '{"group":"resource","resource":"arkivdel","mode":"EMPTY"}'
```

Tilgjengelige grupper:
- `case` (POST /arkiv/noark/sak)
- `journalpost` (PUT /arkiv/noark/sak/mappeid/{caseId})
- `file` (POST /arkiv/noark/dokumentfil)
- `query` (POST /arkiv/noark/sak/$query)
- `resource` (alle resource‑paths listet over)

Oppførselstyper:
- `NORMAL` (default)
- `FAIL` (returner HTTP‑feil med valgfri body)
- `TIMEOUT` (forsink respons; bruk `delay` eller default timeout)
- `EMPTY` (kun for resource‑collections, returnerer tom liste)

`delay` bruker ISO‑8601 varighetsformat i JSON, f.eks. `PT5S`.

**Reset oppførsel**
```bash
curl -i -X DELETE "http://localhost:8080/internal/mock/behavior?group=case"
```

```bash
curl -i -X DELETE "http://localhost:8080/internal/mock/behavior?group=resource&resource=arkivdel"
```

**Logging**
Logger er JSON‑formattert via `kotlin-logging` og `logback.xml`:
- `/Users/janovekongshaug/Repositories/fint-arkiv-adapter-simulator/src/main/resources/logback.xml`

**Gateway Base URL**
Pek gatewayen til WireMock:
```
novari.flyt.archive.gateway.client.fint-archive.base-url=http://archive-adapter-simulator:9090
```
