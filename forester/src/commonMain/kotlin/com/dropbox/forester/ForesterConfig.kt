package com.dropbox.forester

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass


@Serializable
class ForesterConfig(
    val nodes: MutableMap<String, Node> = mutableMapOf(),
    val edges: MutableList<Edge> = mutableListOf()
) {

    fun node(node: Node): ForesterConfig {
        nodes[node.qualifiedName!!] = node
        return this
    }

    fun node(clazz: KClass<*>, shape: Shape = Shape.Rectangle): ForesterConfig {
        nodes[clazz.qualifiedName!!] = Node(shape = shape, loadable = true)
        return this
    }

    fun node(path: String? = null, shape: Shape = Shape.Rectangle): ForesterConfig {
        nodes[path!!] = Node(shape = shape, loadable = false)
        return this
    }


    fun undirected(u: String?, v: String?): ForesterConfig {
        edges.add(Edge(u!!, v!!, Edge.Type.Undirected))
        return this
    }

    fun directed(head: Node, tail: Node): ForesterConfig {
        edges.add(Edge(head.qualifiedName!!, tail.qualifiedName!!, Edge.Type.Directed))
        return this
    }

    fun directed(head: String?, tail: String?): ForesterConfig {
        edges.add(Edge(head!!, tail!!, Edge.Type.Directed))
        return this
    }

    fun directed(head: KClass<*>, tail: KClass<*>): ForesterConfig {
        edges.add(Edge(head.qualifiedName!!, tail.qualifiedName!!, Edge.Type.Directed))
        return this
    }

    fun directed(head: KClass<*>, tail: String?): ForesterConfig {
        edges.add(Edge(head.qualifiedName!!, tail!!, Edge.Type.Directed))
        return this
    }

    fun directed(head: String?, tail: KClass<*>): ForesterConfig {
        edges.add(Edge(head!!, tail.qualifiedName!!, Edge.Type.Directed))
        return this
    }

    fun undirected(u: KClass<*>, v: KClass<*>): ForesterConfig {
        edges.add(Edge(u.qualifiedName!!, v.qualifiedName!!, Edge.Type.Undirected))
        return this
    }

}


fun forester(config: ForesterConfig.() -> Unit): ForesterConfig = ForesterConfig().apply {
    config()
}


fun node(qualifiedName: String? = null, shape: Shape = Shape.Rectangle, loadable: Boolean = true) =
    Node(qualifiedName, shape, loadable)

fun node(path: String? = null, shape: Shape = Shape.Rectangle): Node {
    return Node(path, shape, loadable = false)
}

fun node(clazz: KClass<*>, shape: Shape = Shape.Rectangle): Node {
    return Node(clazz.qualifiedName!!, shape, loadable = true)
}