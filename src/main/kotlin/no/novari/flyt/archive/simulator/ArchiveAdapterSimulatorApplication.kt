package no.novari.flyt.archive.simulator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ArchiveAdapterSimulatorApplication

fun main(args: Array<String>) {
    runApplication<ArchiveAdapterSimulatorApplication>(*args)
}
