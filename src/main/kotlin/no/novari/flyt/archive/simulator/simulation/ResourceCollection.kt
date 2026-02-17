package no.novari.flyt.archive.simulator.simulation

import com.fasterxml.jackson.core.type.TypeReference
import no.fint.model.resource.AbstractCollectionResources

class ResourceCollection<T> : AbstractCollectionResources<T> {
    constructor(items: Collection<T>) : super() {
        items.forEach { addResource(it) }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getTypeReference(): TypeReference<List<T>> {
        return object : TypeReference<List<T>>() {}
    }
}
