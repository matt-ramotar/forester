package com.dropbox.forester.samples.counter.common.forester

import com.dropbox.forester.Forester
import com.dropbox.forester.Shape
import com.dropbox.forester.node
import com.dropbox.forester.samples.counter.common.CounterApi
import com.dropbox.forester.samples.counter.common.CounterRepository
import com.dropbox.forester.samples.counter.common.CounterViewModel

@Forester
object Xplat {
    object Counter {
        val Api = node(CounterApi::class)
        val Repository = node(CounterRepository::class)
        val ViewModel = node(CounterViewModel::class)
        val CounterScreen = node("xplat.counter.CounterScreen", shape = Shape.Parallelogram)
    }
}