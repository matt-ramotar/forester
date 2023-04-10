package com.dropbox.forester.plugin

import org.objectweb.asm.AnnotationVisitor

class ForesterAnnotationVisitor(api: Int) : AnnotationVisitor(api) {
    val values = mutableMapOf<String, Any?>()
    override fun visit(name: String?, value: Any?) {
        if (name != null) {
            values[name] = value
        }
        super.visit(name, value)
    }
}