package com.dropbox.forester.plugin

import com.dropbox.forester.Edge
import com.dropbox.forester.ForesterGraph
import com.dropbox.forester.Node
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.FileInputStream
import java.net.URL
import java.net.URLClassLoader

class ForesterPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("forester", ForesterPluginExt::class.java)

        target.tasks.register("forester") {
            it.group = "Forester"
            it.description = "Map the forest"

            it.doFirst {
                target.exec {
                    it.executable("sh")
                    it.args("-c", "curl -fsSL https://d2lang.com/install.sh | sh -s")
                }
            }

            it.extensions.add("forester", extension)

            val outputDir = extension.outputDir ?: "${target.buildDir.path}/forester"
            val outputPath = outputDir + "/${target.name}"

            it.doLast {
                val buildDir = target.buildDir.resolve("classes/kotlin/jvm/main")

                val files = buildDir
                    .walk()
                    .flatMap { file -> listOf(file, file.parentFile) }
                    .filter { file -> file.isFile && file.name.endsWith(".class") }
                    .toList()

                val urls = files
                    .filter { it.isFile && it.name.endsWith(".class") }
                    .map { it.toURI().toURL() }
                    .toTypedArray()

                val classpath = arrayOf(buildDir.toURI().toURL()) + urls
                val urlClassLoader = URLClassLoader(classpath, javaClass.classLoader)

                val annotatedClasses = mutableListOf<Pair<URL, String>>()

                urls.forEach { url ->
                    try {
                        val classReader = org.objectweb.asm.ClassReader(FileInputStream(url.path))
                        val visitor = ForesterAnnotationVisitor()

                        classReader.accept(visitor, 0)

                        val className = classReader.className

                        if (visitor.hasAnnotation) {
                            annotatedClasses.add(Pair(url, className))
                        }
                    } catch (error: Throwable) {
                        println(error)
                    }
                }

                val nodes: MutableSet<Node> = mutableSetOf()

                annotatedClasses.forEach { (url, className) ->
                    val name = className.replace("/", ".")
                    val clazz = urlClassLoader.loadClass(name)

                    nodes.addAll(getNodes(clazz))
                }


                annotatedClasses.forEach { (url, className) ->
                    val name = className.replace("/", ".")

                    nodes.forEach {
                        println(it)
                    }

                    try {

                        val clazz = urlClassLoader.loadClass(name)

                        val method = clazz.methods.firstOrNull()

                        if (method != null) {
                            val instance = clazz.newInstance()
                            val config = method.invoke(instance) as? ForesterGraph

                            if (config != null) {

                                val edgesD2 = config.edges.map { edge ->
                                    when (edge.edgeType) {
                                        Edge.Type.Directed -> {
                                            """
                                ${edge.u.qualifiedName} -> ${edge.v.qualifiedName}
                            """.trimIndent()
                                        }

                                        Edge.Type.Undirected -> {
                                            """
                                ${edge.u.qualifiedName} <-> ${edge.v.qualifiedName}
                            """.trimIndent()
                                        }
                                    }
                                }


                                val nodesD2 = nodes.toMutableList().map { node ->
                                    """
                               ${node.qualifiedName ?: ""}: {
                                    shape: ${node.shape.name.lowercase()}
                                }
                            """.trimIndent()
                                }


                                if (!target.file(target.buildDir.path).exists()) {
                                    target.file(target.buildDir.path).mkdir()
                                }

                                if (!target.file("${target.buildDir.path}/forester").exists()) {
                                    target.file("${target.buildDir.path}/forester").mkdir()
                                }

                                target.file("${outputPath}.d2").delete()
                                val outputFile = target.file("${outputPath}.d2")
                                nodesD2.forEach { node ->
                                    outputFile.appendText(node)
                                    outputFile.appendText("\n")
                                }

                                edgesD2.forEach { edge ->
                                    outputFile.appendText(edge)
                                    outputFile.appendText("\n")
                                }
                            }
                        }
                    } catch (error: Throwable) {
                        println(error)
                    }
                }

                target.exec {
                    try {
                        it.commandLine(
                            "/opt/homebrew/Cellar/d2/0.3.0/bin/d2",
                            "${outputPath}.d2",
                            "${outputPath}.svg",
                            "--sketch"
                        )
                    } catch (error: Throwable) {

                        println("Run ./gradlew :${target}:installD2")
                    }
                }
            }
        }
    }
}


fun getNodes(clazz: Class<*>): MutableList<Node> {
    val nodes = mutableSetOf<Node>()
    val visited = mutableSetOf<Class<*>>()
    walk(clazz, nodes, visited)
    return nodes.toMutableList()
}

fun walk(clazz: Class<*>, nodes: MutableSet<Node>, visited: MutableSet<Class<*>> = mutableSetOf()) {

    if (visited.contains(clazz)) {
        return
    }

    visited.add(clazz)

    clazz.declaredFields.forEach {
        if (it.type.isAssignableFrom(Node::class.java)) {
            println("ADDING")
            try {
                it.isAccessible = true
                nodes.add(it.get(null) as Node)
            } catch (error: Throwable) {
                println(error)
            }

        } else if (it.type.declaredClasses.isNotEmpty()) {
            it.type.declaredClasses.forEach { subClass ->
                walk(subClass, nodes, visited)
            }
        }

    }
}