package com.dropbox.forester

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass


@Serializable
class ForesterGraph(
    val edges: MutableList<Edge> = mutableListOf()
) {

    fun undirected(head: Node, tail: Node): ForesterGraph {
        edges.add(Edge(head, tail, Edge.Type.Undirected))
        return this
    }


    fun directed(head: Node, tail: Node): ForesterGraph {
        edges.add(Edge(head, tail, Edge.Type.Directed))
        return this
    }
}

fun forester(graph: ForesterGraph.() -> Unit): ForesterGraph = ForesterGraph().apply {
    graph()
}

fun node(qualifiedName: String? = null, shape: Shape = Shape.Rectangle, loadable: Boolean = true) =
    Node(qualifiedName, shape, loadable)

fun node(path: String? = null, shape: Shape = Shape.Rectangle): Node {
    return Node(path, shape, loadable = false)
}

fun node(clazz: KClass<*>, shape: Shape = Shape.Rectangle): Node {
    return Node(clazz.qualifiedName!!, shape, loadable = true)
}