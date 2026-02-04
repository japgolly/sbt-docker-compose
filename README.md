# Installation

Add this to your `project/plugins.sbt` and replace `version`:

```scala
libraryDependencies += "com.github.japgolly.sbt-docker-compose" %% "lib" % version
```

# Usage

### Tests

Use this code to start up docker-compose before tests are run.

First create `project/DockerEnv.scala` with the following
(replacing `"envs/test"` with the directory where the `docker-compose.yml` can be found)

```scala
import sbt._
import sbt.Keys._
import japgolly.sbt.DockerCompose

object DockerEnv {

  object test extends (Project => Project) {
    // Load all services from ./envs/test/docker-compose.yml
    private val services = DockerCompose.Services(file("envs/test"))

    override def apply(p: Project): Project =
      p.settings(
        // Start services before running tests
        Test / testOptions += Tests.Setup(services.start),
      )
  }
}
```

Then apply it to your sbt project definition:

```sbt
  .configure(DockerEnv.test)
```

### Dev

Let's say you had `envs/dev/docker-compose.yml` like this:
```yml
services:
  postgres:
    image: postgres:18
    container_name: example_dev_postgres
    ports:
      - $PORT_POSTGRES:5432
    environment:
      - POSTGRES_DB=example
      - POSTGRES_USER=dev
      - POSTGRES_PASSWORD=pd
```
and `envs/dev/.env` like this:
```properties
PORT_POSTGRES=12345
```

Use the following code to:
1. start up docker-compose before the sbt `run` command is executed
1. load environmental config from docker-compose

First create `project/DockerEnv.scala` with the following:


```scala
import sbt._
import sbt.Keys._
import japgolly.sbt.DockerCompose

object DockerEnv {

  object dev extends (Project => Project) {
    val devEnvStart = taskKey[Unit]("Starts up the dev environment.")
    val devEnvStop = taskKey[Unit]("Stops the dev environment.")

    private val dir = file("envs/dev")

    private val services = DockerCompose.Services(dir)

    private val runOptions = DockerCompose.JavaOptions.fromDockerCompose("postgres", dir)
      .renameKey("POSTGRES_DB", "MYAPP_DB_DATABASE") // loaded from docker-compose.yml
      .renameKey("POSTGRES_USER", "MYAPP_DB_USERNAME") // loaded from docker-compose.yml
      .renameKey("POSTGRES_PASSWORD", "MYAPP_DB_PASSWORD") // loaded from docker-compose.yml
      .add("MYAPP_DB_PORT", DockerCompose.envFileValue(dir, "PORT_POSTGRES")) // loads from .env
      .add("MYAPP_DB_POOL_MAXIMUMPOOLSIZE", "4")
      .asList

    override def apply(p: Project): Project =
      p.settings(
        ThisBuild / devEnvStart := services.start(),
        ThisBuild / devEnvStop := services.stop(),
        Compile / run := (Compile / run).dependsOn(devEnvStart).evaluated,
        Compile / run / fork := true,
        Compile / run / javaOptions ++= runOptions,
      )
  }
}
```

Then apply it to your sbt project definition:

```sbt
  .configure(DockerEnv.dev)
```
