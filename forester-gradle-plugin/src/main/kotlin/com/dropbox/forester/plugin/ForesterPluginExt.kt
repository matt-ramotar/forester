package com.dropbox.forester.plugin

import org.gradle.api.tasks.Input

open class ForesterPluginExt {

    @Input
    var outputDir: String? = null
}