package com.dropbox.forester.plugin

import com.dropbox.forester.Edge
import com.dropbox.forester.ForesterGraph
import com.dropbox.forester.Node
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.FileInputStream
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

class ForesterPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("forester", ForesterPluginExt::class.java)

        target.tasks.register("forester") {
            it.group = "Forester"
            it.description = "Map the forest"

            it.doFirst {
                if (extension.update) {
                    target.exec {
                        it.executable("sh")
                        it.args("-c", "curl -fsSL https://d2lang.com/install.sh | sh -s")
                    }
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

                                    try {
                                        val cls = urlClassLoader.loadClass(requireNotNull(node.qualifiedName))
                                        val fields = cls.kotlin.memberProperties


                                        val methods = cls.kotlin.memberFunctions

                                        """${node.qualifiedName ?: ""}: {
                                            shape: class
                                            
                                            ${
                                            fields.map { field ->
                                                "${field.name}: ${field.returnType.toString().replace("kotlin.", "")}"
                                            }.joinToString("\n").trimIndent()
                                        }

                                            ${
                                            methods.filter {
                                                !setOf(
                                                    "equals",
                                                    "hashCode",
                                                    "toString"
                                                ).contains(it.name)
                                            }.map { method ->


                                                val argsList = method.parameters.map { it.type.jvmErasure.simpleName }
                                                val args = argsList.drop(1)

                                                "${method.name}(${args.joinToString(", ").trim()}): ${
                                                    method.returnType.toString().replace("kotlin.", "")
                                                }"
                                            }.joinToString("\n").trimIndent()
                                        }
                                        }""".trimIndent()


                                    } catch (error: Throwable) {
                                        """
                               ${node.qualifiedName ?: ""}: {
                                    shape: ${node.shape.name.lowercase()}
                                }
                            """.trimIndent()
                                    }
                                }

                                if (!target.file(target.buildDir.path).exists()) {
                                    target.file(target.buildDir.path).mkdir()
                                }

                                if (!target.file("${target.buildDir.path}/forester").exists()) {
                                    target.file("${target.buildDir.path}/forester").mkdir()
                                }

                                val path = target.file("${target.buildDir.path}/forester/${target.name}.d2")
                                path.delete()

                                val outputFile = path

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
                        val path = target.file("${target.buildDir.path}/forester/${target.name}.d2")

                        it.commandLine(
                            "/opt/homebrew/Cellar/d2/0.3.0/bin/d2",
                            path,
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

fun kotlinTypeToJavaName(type: String): String {
    return when (type) {
        "kotlin.Int" -> "int"
        "kotlin.Long" -> "long"
        "kotlin.Short" -> "short"
        "kotlin.Byte" -> "byte"
        "kotlin.Float" -> "float"
        "kotlin.Double" -> "double"
        "kotlin.Boolean" -> "boolean"
        "kotlin.Char" -> "char"
        else -> type
    }
}