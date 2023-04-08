# Forester

A Kotlin library for mapping out the forest.

<img src="./github/mapping_out_the_forest.png" alt="Mapping out the forest"/>

## Problem

A system's architecture must be understood well. Diagrams show structure and components. But code evolves and architecture
changes. And it becomes increasingly difficult to keep the diagrams and code synced. Outdated diagrams can lead to
miscommunication, misconceptions about the system, and ultimately, increased costs of development and maintenance.

## Solution

Forester keeps diagrams aligned with code using these tactics:

1. **Automatic generation**: Forester parses your code and generates architecture diagrams based on your code's
   structure and annotations.
2. **Version control**: Store your architecture diagrams alongside your code in the same version control system.
3. **Continuous integration**: Integrate Forester into your CI pipeline to generate and update diagrams automatically
   whenever significant code changes are made.
4. **Collaboration**: Promote a culture of shared responsibility for keeping architecture diagrams current among your
   engineering team.

## Installation

```shell
plugins {
  id("com.dropbox.forester.plugin")
}

implementation("com.dropbox.forester:forester:0.1.0")

forester {
  outputDirPath = "docs/forester"
}
```

## Sample


```kotlin
@ForesterExport
class Forester {

    @Forester
    fun map() = forester {
        node("server.counter.subscribe")
        node(Api::class)
        node(Repository::class)
        node(ViewModel::class)
        node("xplat.counter.CounterScreen")

        directed("server.counter.subscribe", Api::class)
        directed(Api::class, Repository::class)
        directed(Repository::class, ViewModel::class)
        undirected(ViewModel::class, CounterScreen)
    }
}

```

<img src="./github/sample.png"/>
