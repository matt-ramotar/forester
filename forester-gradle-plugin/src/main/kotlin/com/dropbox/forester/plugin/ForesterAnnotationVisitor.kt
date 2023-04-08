package com.dropbox.forester.plugin

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes

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