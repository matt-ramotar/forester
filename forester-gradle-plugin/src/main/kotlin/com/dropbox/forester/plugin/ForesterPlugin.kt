package com.dropbox.forester.plugin

import com.dropbox.forester.ForesterEdge
import com.dropbox.forester.ForesterGraph
import com.dropbox.forester.ForesterNode
import com.dropbox.forester.Shape
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.objectweb.asm.ClassReader
import java.io.FileInputStream
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KType
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

class ForesterPlugin : Plugin<Project> {

    private val AnnotatedClass.simpleName
        get() = className.replace("/", ".")

    private data class AnnotatedClass(
        val url: URL,
        val className: String
    )

    private fun Project.generateUrls(): Array<URL> {
        val compiledDirPath = this.buildDir.resolve("classes/kotlin/jvm/main")
        val files = compiledDirPath
            .walk()
            .flatMap { file -> listOf(file, file.parentFile) }
            .filter { file -> file.isFile && file.name.endsWith(".class") }
            .toList()

        return files
            .filter { it.isFile && it.name.endsWith(".class") }
            .map { it.toURI().toURL() }
            .toTypedArray()
    }

    private fun Project.generateClasspath(): Array<URL> {
        return arrayOf(buildDir.toURI().toURL()) + generateUrls()
    }

    private fun URL.getEdges(edges: MutableSet<ForesterEdge>) {
        val classReader = ClassReader(FileInputStream(path))
        val visitorDirected = ForesterClassVisitor(ForesterAnnotation.Directed)
        val visitorUndirected = ForesterClassVisitor(ForesterAnnotation.Undirected)

        classReader.accept(visitorDirected, 0)
        edges.addAll(visitorDirected.edges)

        classReader.accept(visitorUndirected, 0)
        edges.addAll(visitorUndirected.edges)
    }

    private fun URL.annotatedClassOrNull(annotation: ForesterAnnotation): AnnotatedClass? = try {
        val classReader = ClassReader(FileInputStream(path))
        val visitor = ForesterClassVisitor(annotation)
        classReader.accept(visitor, 0)

        val className = classReader.className

        if (visitor.hasAnnotation) {
            AnnotatedClass(this, className)
        } else {
            null
        }
    } catch (error: Throwable) {
        println(error)
        null
    }

    private fun Project.loadAnnotatedClasses(annotation: ForesterAnnotation): MutableList<AnnotatedClass> =
        generateUrls().mapNotNull { url -> url.annotatedClassOrNull(annotation) }.toMutableList()


    private fun generateEdgesD2(edges: MutableSet<ForesterEdge>) =
        edges.joinToString("\n") { edge ->
            val arrow = when (edge.edgeType) {
                ForesterEdge.Type.Directed -> "->"
                ForesterEdge.Type.Undirected -> "<->"
            }

            "${edge.u} $arrow ${edge.v}"
        }

    private val KType.simpleName
        get() = toString().replace("kotlin.", "")


    private fun generateForesterNodesD2(
        urlClassLoader: URLClassLoader,
        nodes: MutableSet<ForesterNode>
    ) =
        nodes.joinToString("\n") { node ->
            try {
                val clazz = urlClassLoader.loadClass(requireNotNull(node.qualifiedName))
                val properties = clazz.kotlin.memberProperties
                val methods = clazz.kotlin.memberFunctions

                val propertiesD2 = properties.map { property ->
                    "${property.name}: ${property.returnType.simpleName}"
                }

                val methodsD2 = methods
                    .filter { method ->
                        !setOf(
                            "equals",
                            "hashCode",
                            "toString"
                        ).contains(method.name)
                    }
                    .joinToString("\n") { method ->
                        val args = method.parameters.map { it.type.jvmErasure.simpleName }.drop(1)
                        "${method.name}(${args.joinToString(", ")}): ${method.returnType.simpleName}"
                    }

                """
                    ${node.qualifiedName}: {
                        shape: class
                        $propertiesD2
                        $methodsD2
                    }
                """.trimIndent()
            } catch (error: Throwable) {
                """
                    ${node.qualifiedName}: {
                        shape: ${node.shape.name.lowercase()}
                    }
                """.trimIndent()
            }
        }


    override fun apply(target: Project) {
        val extension = target.extensions.create("forester", ForesterPluginExt::class.java)


        val compileTaskName = "compileKotlinMetadata"
        val compileTask = target.tasks.named(compileTaskName, KotlinCompile::class.java)


        target.tasks.register("forester") { task ->
            task.dependsOn(compileTask)

            task.group = "Forester"
            task.description = "Map the forest"

            task.doFirst { _ ->
                if (extension.update) {
                    target.exec { execSpec ->
                        execSpec.executable("sh")
                        execSpec.args("-c", "curl -fsSL https://d2lang.com/install.sh | sh -s")
                    }
                }
            }

            task.extensions.add("forester", extension)

            val outputDir = extension.outputDir ?: "${target.buildDir.path}/forester"
            val outputPath = outputDir + "/${target.name}"

            task.doLast { _ ->
                val classpath = target.generateClasspath()
                val urlClassLoader = URLClassLoader(classpath, javaClass.classLoader)
                val annotatedForesterNodeClasses =
                    target.loadAnnotatedClasses(ForesterAnnotation.Node)
                val annotatedForesterNodes = annotatedForesterNodeClasses
                    .map { ForesterNode(it.className, shape = Shape.Class) }
                    .toMutableList()
                val annotatedForestClasses = target.loadAnnotatedClasses(ForesterAnnotation.Forest)
                val annotatedGraphClasses = target.loadAnnotatedClasses(ForesterAnnotation.Graph)

                val nodes: MutableSet<ForesterNode> = annotatedForesterNodes.toMutableSet()
                val edges: MutableSet<ForesterEdge> = mutableSetOf()
                val visited: MutableSet<Class<*>> = mutableSetOf()

                annotatedForestClasses.forEach { annotatedClass ->
                    try {
                        val clazz = urlClassLoader.loadClass(annotatedClass.simpleName)
                        clazz.walk(nodes, visited)
                    } catch (error: Throwable) {
                        println(error)
                    }
                }

                annotatedForesterNodeClasses.forEach { annotatedClass ->
                    annotatedClass.url.getEdges(edges)
                }

                annotatedGraphClasses.forEach { annotatedClass ->
                    val clazz = urlClassLoader.loadClass(annotatedClass.simpleName)
                    clazz.methods.forEach { method ->
                        try {
                            val instance = clazz.newInstance()
                            val graph = method.invoke(instance) as ForesterGraph
                            edges.addAll(graph.edges)
                        } catch (_: Throwable) {
                            // Do nothing
                        }
                    }
                }

                val nodesD2 = generateForesterNodesD2(urlClassLoader, nodes)
                val edgesD2 = generateEdgesD2(edges)
                target.writeD2(nodesD2, edgesD2)
            }

            task.doLast {
                target.exec {
                    try {
                        target.file("${target.buildDir.path}/forester").mkdir()
                        target.file("${target.buildDir.path}/forester/${target.name}.d2")
                            .createNewFile()

                        val path = target.file("${target.buildDir.path}/forester/${target.name}.d2")

                        it.commandLine(
                            "/opt/homebrew/Cellar/d2/0.4.0/bin/d2",
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


private fun Project.writeD2(nodesD2: String, edgesD2: String) {

    if (!file(buildDir.path).exists()) {
        file(buildDir.path).mkdir()
    }

    if (!file("${buildDir.path}/forester").exists()) {
        file("${buildDir.path}/forester").mkdir()
    }

    val path = file("${buildDir.path}/forester/${name}.d2")
    path.delete()

    path.appendText(nodesD2)
    path.appendText("\n")
    path.appendText(edgesD2)
}

private fun Class<*>.walk(
    nodes: MutableSet<ForesterNode>,
    visited: MutableSet<Class<*>> = mutableSetOf()
) {
    if (visited.contains(this)) {
        return
    }

    visited.add(this)

    declaredFields.forEach { field ->
        if (field.type.isAssignableFrom(ForesterNode::class.java)) {
            try {
                field.isAccessible = true
                nodes.add(field.get(null) as ForesterNode)
            } catch (error: Throwable) {
                println(error)
            }

        } else if (field.type.declaredClasses.isNotEmpty()) {
            field.type.declaredClasses.forEach { subClass ->
                subClass.walk(nodes, visited)
            }
        }
    }
}