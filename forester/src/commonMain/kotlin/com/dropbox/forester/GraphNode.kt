package com.dropbox.forester

import kotlinx.serialization.Serializable

@Serializable
data class GraphNode(
    val qualifiedName: String? = null,
    val shape: Shape = Shape.Rectangle,
    val loadable: Boolean = true,
)

