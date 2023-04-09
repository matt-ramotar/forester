package com.dropbox.forester

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass


@Serializable
class ForesterGraph(
    val edges: MutableList<Edge> = mutableListOf()
) {

    fun undirected(head: GraphNode, tail: GraphNode): ForesterGraph {
        edges.add(Edge(head, tail, Edge.Type.Undirected))
        return this
    }


    fun directed(head: GraphNode, tail: GraphNode): ForesterGraph {
        edges.add(Edge(head, tail, Edge.Type.Directed))
        return this
    }

    fun directed(head: KClass<*>, tail: KClass<*>): ForesterGraph {
        edges.add(Edge(GraphNode(head.qualifiedName), GraphNode(tail.qualifiedName), Edge.Type.Directed))
        return this
    }

    fun directed(head: KClass<*>, tail: GraphNode): ForesterGraph {
        edges.add(Edge(GraphNode(head.qualifiedName), tail, Edge.Type.Directed))
        return this
    }

    fun directed(head: GraphNode, tail: KClass<*>): ForesterGraph {
        edges.add(Edge(head, GraphNode(tail.qualifiedName), Edge.Type.Directed))
        return this
    }
}

fun graph(graph: ForesterGraph.() -> Unit): ForesterGraph = ForesterGraph().apply {
    graph()
}

fun node(qualifiedName: String? = null, shape: Shape = Shape.Rectangle, loadable: Boolean = true) =
    GraphNode(qualifiedName, shape, loadable)

fun node(path: String? = null, shape: Shape = Shape.Rectangle): GraphNode {
    return GraphNode(path, shape, loadable = false)
}

fun node(clazz: KClass<*>, shape: Shape = Shape.Rectangle): GraphNode {
    return GraphNode(clazz.qualifiedName!!, shape, loadable = true)
}