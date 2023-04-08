package com.dropbox.forester

import kotlinx.serialization.Serializable


@Serializable
data class Edge(
    val u: Node,
    val v: Node,
    val edgeType: Type
) {

    @Serializable
    enum class Type(val value: String) {
        Directed("Directed"),
        Undirected("Undirected")
    }


}
