package com.dropbox.forester.plugin

import com.dropbox.forester.Forester
import com.dropbox.forester.Shape
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import kotlin.reflect.KClass


@Suppress("UNCHECKED_CAST")
class ForesterClassVisitor(private val annotation: ForesterAnnotation) :
    ClassVisitor(Opcodes.ASM9) {
    var hasAnnotation = false
    lateinit var nodeU: Forester.Node
    val edges: MutableSet<Forester.Edge> = mutableSetOf()

    private fun ForesterAnnotationVisitor.getNodes() = values["nodes"] as? Array<KClass<*>>
    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        nodeU = Forester.Node(name, shape = Shape.Class)
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitAnnotation(
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {

        if (descriptor?.contains(annotation.descriptor) == true) {
            hasAnnotation = true
            val foresterAnnotationVisitor = ForesterAnnotationVisitor(api)


            when (annotation) {
                ForesterAnnotation.Directed -> {
                    val classes = foresterAnnotationVisitor.getNodes()
                    classes?.forEach { clazz ->
                        val nodeV = Forester.Node(clazz.qualifiedName, shape = Shape.Class)
                        edges.add(Forester.Edge(nodeU, nodeV, Forester.Edge.Type.Directed))
                    }
                }

                ForesterAnnotation.Undirected -> {
                    val classes = foresterAnnotationVisitor.getNodes()
                    classes?.forEach { clazz ->
                        val nodeV = Forester.Node(clazz.qualifiedName, shape = Shape.Class)
                        edges.add(Forester.Edge(nodeU, nodeV, Forester.Edge.Type.Undirected))
                    }
                }

                else -> {}
            }
            return foresterAnnotationVisitor
        }

        return super.visitAnnotation(descriptor, visible)
    }


    private class FallbackVisitor : AnnotationVisitor(Opcodes.ASM9) {
        var hasAnnotation = false
    }
}