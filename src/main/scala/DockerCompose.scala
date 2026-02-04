package japgolly.sbt

import sbt._

object DockerCompose {
  import sys.process._

  def envFileValue(envRoot: File, key: String): String = {
    val k = key + "="
    val f = envRoot / ".env"
    IO.readLines(f)
      .find(_.startsWith(k))
      .getOrElse(sys error s"Can't find $k in ${f.absolutePath}")
      .drop(k.length)
  }

  final case class JavaOptions(asMap: Map[String, String]) extends AnyVal {

    def add(k: String, v: String): JavaOptions =
      JavaOptions(asMap.updated(k, v))

    def remove(k: String): JavaOptions =
      JavaOptions(asMap - k)

    def ++(other: JavaOptions): JavaOptions =
      JavaOptions(asMap ++ other.asMap)

    def asList: List[String] =
      asMap.iterator.map { case (k, v) => s"-D$k=$v" }.toList
  }

  object JavaOptions {

    def fromDockerCompose(serviceName: String, envRoot: File, filename: String = "docker-compose.yml"): JavaOptions = {
      val service = s"  $serviceName:"
      val envVar = "\\$\\{?([A-Za-z0-9_]+)\\}?".r
      def processEntry(e: String): (String, String) = {
        val line = envVar.replaceAllIn(e, m => envFileValue(envRoot, m group 1))
        val i = line.indexOf('=')
        if (i < 0)
          sys error s"Can't parse environment variable: $line"
        else
          (line.substring(0, i), line.substring(i + 1))
      }
      var inService = false
      var inEnv = false
      val b = Map.newBuilder[String, String]
      IO.readLines(envRoot / filename) foreach {
        case `service`                                           => inService = true
        case s if s.matches("^  [a-z].*:")                       => inService = false; inEnv = false
        case "    environment:"                                  => inEnv = true
        case s if s.matches("^    [a-z].*:")                     => inEnv = false
        case s if inService && inEnv && s.startsWith("      - ") => b += processEntry(s.drop(8).trim)
        case _                                                   => ()
      }
      apply(b.result())
    }

/*
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
*/
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
    def apply(dir: File, only: Iterable[String] = Nil): Services = {
      val dockerCompose = "docker-compose"
      val names = only.mkString(" ")
      def run(cmd: String) = Process(cmd, dir).!
      new Services(
        () => run(s"$dockerCompose up -d $names"),
        () => run(s"$dockerCompose stop $names"))
    }
  }

}
