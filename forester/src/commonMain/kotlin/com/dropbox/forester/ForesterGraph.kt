package com.dropbox.forester

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class ForesterGraph(
    val edges: MutableSet<ForesterEdge> = mutableSetOf()
) {
    fun undirected(head: ForesterNode, tail: ForesterNode): ForesterGraph {
        edges.add(ForesterEdge(head, tail, ForesterEdge.Type.Undirected))
        return this
    }


    fun directed(head: ForesterNode, tail: ForesterNode): ForesterGraph {
        edges.add(ForesterEdge(head, tail, ForesterEdge.Type.Directed))
        return this
    }

    fun directed(head: KClass<*>, tail: KClass<*>): ForesterGraph {
        edges.add(
            ForesterEdge(
                ForesterNode(head.qualifiedName),
                ForesterNode(tail.qualifiedName),
                ForesterEdge.Type.Directed
            )
        )
        return this
    }

    fun directed(head: KClass<*>, tail: ForesterNode): ForesterGraph {
        edges.add(ForesterEdge(ForesterNode(head.qualifiedName), tail, ForesterEdge.Type.Directed))
        return this
    }

    fun directed(head: ForesterNode, tail: KClass<*>): ForesterGraph {
        edges.add(ForesterEdge(head, ForesterNode(tail.qualifiedName), ForesterEdge.Type.Directed))
        return this
    }
}

fun graph(graph: ForesterGraph.() -> Unit): ForesterGraph = ForesterGraph().apply {
    graph()
}

fun node(qualifiedName: String? = null, shape: Shape = Shape.Rectangle, loadable: Boolean = true) =
    ForesterNode(qualifiedName, shape, loadable)

fun node(path: String? = null, shape: Shape = Shape.Rectangle): ForesterNode {
    return ForesterNode(path, shape, loadable = false)
}

fun node(clazz: KClass<*>, shape: Shape = Shape.Rectangle): ForesterNode {
    return ForesterNode(clazz.qualifiedName!!, shape, loadable = true)
}