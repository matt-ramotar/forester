package com.dropbox.forester.samples.counter.common

import com.dropbox.forester.Forester
import com.dropbox.forester.ForesterExport
import com.dropbox.forester.forester

@ForesterExport
class Forester() {

    @Forester
    fun map() = forester {
        node(SERVER_COUNTER_SUBSCRIBE)
        node(CounterApi::class)
        node(CounterViewModel::class)
        node(XPLAT_COUNTER_SCREEN)

        directed(SERVER_COUNTER_SUBSCRIBE, CounterApi::class)
        directed(CounterApi::class, CounterRepository::class)
        directed(CounterViewModel::class, XPLAT_COUNTER_SCREEN)
    }
}

private const val SERVER_COUNTER_SUBSCRIBE = "server.counter.subscribe"
private const val XPLAT_COUNTER_SCREEN = "xplat.counter.common.CounterScreen"





