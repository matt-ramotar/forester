package com.dropbox.forester.samples.counter.common.forester

import com.dropbox.forester.Forester
import com.dropbox.forester.forester

@Forester
class Graph() {
    fun map() = forester {
        directed(Server.Counter.subscribe, Xplat.Counter.Api)
        directed(Xplat.Counter.Api, Xplat.Counter.Repository)
        directed(Xplat.Counter.Repository, Xplat.Counter.ViewModel)
        directed(Xplat.Counter.ViewModel, Xplat.Counter.CounterScreen)
    }
}

