package com.dropbox.forester

import kotlinx.serialization.Serializable

@Serializable
data class ForesterEdge(
    val u: ForesterNode,
    val v: ForesterNode,
    val edgeType: Type
) {

    @Serializable
    enum class Type(val value: String) {
        Directed("Directed"),
        Undirected("Undirected")
    }
}