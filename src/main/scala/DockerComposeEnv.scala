package japgolly.sbt

import sbt._

object DockerComposeEnv {
  import sys.process._

  def envFileValue(envRoot: File, key: String): String = {
    val k = key + "="
    val f = envRoot / ".env"
    IO.readLines(f)
      .find(_.startsWith(k))
      .getOrElse(sys error s"Can't find $k in ${f.absolutePath}")
      .drop(k.length)
  }

  final case class JavaOptions(asList: List[String]) extends AnyVal {

    def add(k: String, v: String): JavaOptions =
      JavaOptions(s"-D$k=$v" :: asList.filterNot(_ startsWith s"-D$k="))

    def ++(other: JavaOptions): JavaOptions =
      JavaOptions(asList ++ other.asList)
  }

  object JavaOptions {

    def fromDockerCompose(serviceName: String, envRoot: File, filename: String = "docker-compose.yml"): JavaOptions = {
      val service = s"  $serviceName:"
      val envVar = "\\$\\{?([A-Za-z0-9_]+)\\}?".r
      def processEntry(e: String) =
        envVar.replaceAllIn(e, m => envFileValue(envRoot, m group 1))
      var inService = false
      var inEnv = false
      val b = List.newBuilder[String]
      IO.readLines(envRoot / filename) foreach {
        case `service`                                           => inService = true
        case s if s.matches("^  [a-z].*:")                       => inService = false; inEnv = false
        case "    environment:"                                  => inEnv = true
        case s if s.matches("^    [a-z].*:")                     => inEnv = false
        case s if inService && inEnv && s.startsWith("      - ") => b += "-D" + processEntry(s.drop(8).trim)
        case _                                                   => ()
      }
      apply(b.result())
    }

    def fromProps(props: File): JavaOptions =
      fromProps(IO readLines props)

    def fromProps(propsLines: List[String]): JavaOptions = {
      val list = propsLines
        .iterator
        .map(_.replaceFirst(" *#.+", "").trim.replaceFirst(" *= *", "="))
        .filter(_.nonEmpty)
        .map("-D" + _)
        .toList
      apply(list)
    }
  }

  final class Services(startFn: () => Unit, stopFn: () => Unit) {
    private val lock = AnyRef
    private var up = false

    val start: () => Unit =
      () => lock.synchronized {
        if (!up) {
          startFn()
          up = true
        }
      }

    val stop: () => Unit =
      () => lock.synchronized {
        up = false
        stopFn()
      }
  }

  object Services {
    def fromDockerCompose(env: String, only: Iterable[String] = Nil): Services = {
      val names = only.mkString(" ")
      new Services(
        () => s"bin/env $env up -d $names".!,
        () => s"bin/env $env stop $names".!)
    }
  }

}
