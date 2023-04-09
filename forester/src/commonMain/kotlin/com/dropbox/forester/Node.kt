package com.dropbox.forester

import kotlin.reflect.KClass

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Graph

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Forest


@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Directed(
    val nodes: Array<KClass<*>>
)

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Undirected(
    val nodes: Array<KClass<*>>
)

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Node(
    val shape: Shape = Shape.Rectangle
)
