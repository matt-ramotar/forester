package com.dropbox.forester.plugin

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes

class FallbackAnnotationVisitor : AnnotationVisitor(Opcodes.ASM9) {
    var hasAnnotation = false
}