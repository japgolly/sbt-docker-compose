# Installation

Add this to your `project/plugins.sbt` and replace `version`:

```scala
libraryDependencies += "com.github.japgolly.sbt-docker-compose-env" %% "sbt-docker-compose-env" % version
```

# Usage

### Tests

Use this code to start up docker-compose before tests are run.

First create `project/DockerEnv.scala` with the following
(replacing `"envs/test"` with the directory where the `docker-compose.yml` can be found)

```scala
import sbt._
import sbt.Keys._
import japgolly.sbt.DockerComposeEnv

object DockerEnv {

  object test extends (Project => Project) {
    private val services = DockerComposeEnv.Services.fromDockerCompose(file("envs/test"))

    override def apply(p: Project): Project =
      p.settings(
        Test / testOptions += Tests.Setup(services.start),
      )
  }
}
```

Then apply it to your sbt project definition:

```sbt
  .configure(DockerEnv.test)
```
