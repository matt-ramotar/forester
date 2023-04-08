package com.dropbox.forester.samples.counter.common.forester

import com.dropbox.forester.Forester
import com.dropbox.forester.Shape
import com.dropbox.forester.node

@Forester
object Server {
    object Counter {
        val subscribe = node("server.counter.subscribe", Shape.Cloud)
    }
}