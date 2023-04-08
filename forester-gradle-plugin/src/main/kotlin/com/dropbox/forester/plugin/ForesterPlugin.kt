package com.dropbox.forester.plugin

import com.dropbox.forester.Edge
import com.dropbox.forester.Forester
import com.dropbox.forester.ForesterConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes
import java.io.FileInputStream
import java.net.URL
import java.net.URLClassLoader

class ForesterPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.dropbox.forester.plugin")

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

                val files = buildDir.walk().flatMap { file -> listOf(file, file.parentFile) }
                    .filter { file -> file.isFile && file.name.endsWith(".class") }
                    .toList()


                val urls = files.filter {
                    it.isFile && it.name.endsWith(".class")
                }.map {
                    it.toURI().toURL()
                }
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

                annotatedClasses.forEach { (url, className) ->
                    val name = className.replace("/", ".")

                    try {

                        val clazz = urlClassLoader.loadClass(name)
                        val method = clazz.methods.firstOrNull { method ->
                            method.isAnnotationPresent(Forester::class.java)
                        }

                        if (method != null) {
                            val instance = clazz.newInstance()
                            val config = method.invoke(instance) as? ForesterConfig

                            if (config != null) {
                                val edges = config.edges.map { edge ->
                                    edge.copy(
                                        u = config.nodes[edge.u]?.qualifiedName ?: edge.u,
                                        v = config.nodes[edge.v]?.qualifiedName ?: edge.v,
                                    )
                                }
                                val edgesD2 = edges.map { edge ->
                                    when (edge.edgeType) {
                                        Edge.Type.Directed -> {
                                            """
                                ${edge.u} -> ${edge.v}
                            """.trimIndent()
                                        }

                                        Edge.Type.Undirected -> {
                                            """
                                ${edge.u} <-> ${edge.v}
                            """.trimIndent()
                                        }
                                    }
                                }


                                val nodesD2 = config.nodes.entries.map { (path, node) ->
                                    """
                                ${node.qualifiedName ?: path}: {
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


class ForesterAnnotationVisitor : org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
    var hasAnnotation = false
    override fun visitAnnotation(
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor {

        if (descriptor?.contains("Lcom/dropbox/forester/ForesterExport;") == true) {
            hasAnnotation = true
        }

        return super.visitAnnotation(descriptor, visible) ?: FallbackAnnotationVisitor()
    }
}

class FallbackAnnotationVisitor : AnnotationVisitor(Opcodes.ASM9) {
    var hasAnnotation = false
}