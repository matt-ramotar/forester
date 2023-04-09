package com.dropbox.forester.samples.counter.common.forester

import com.dropbox.forester.Graph
import com.dropbox.forester.graph
import com.dropbox.forester.samples.counter.common.CounterApi
import com.dropbox.forester.samples.counter.common.CounterViewModel

@Graph
class CommonGraph {
    fun provide() = graph {
        directed(Server.Counter.subscribe, CounterApi::class)
        directed(CounterViewModel::class, Xplat.Counter.CounterScreen)
    }
}

