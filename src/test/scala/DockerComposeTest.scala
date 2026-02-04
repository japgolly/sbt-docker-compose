package japgolly.sbt

import sbt.{Tests => _, _}
import utest._
import DockerCompose._

object DockerComposeTest extends TestSuite {

  private val root = file("src/test/data")

  override def tests = Tests {

    test("test-env") {
      test("postgres") {
        val options = JavaOptions.fromDockerCompose("postgres", root / "test-env")
        assert(options.asList.sorted == List(
          "-DPOSTGRES_DB=example_test",
          "-DPOSTGRES_PASSWORD=sqd",
          "-DPOSTGRES_USER=dev",
        ))
      }

      test("customise") {
        val options = JavaOptions.fromDockerCompose("postgres", root / "test-env")
          .add("POSTGRES_DB", "x")
          .remove("POSTGRES_USER")
        assert(options.asList.sorted == List(
          "-DPOSTGRES_DB=x",
          "-DPOSTGRES_PASSWORD=sqd",
        ))
      }

      test("redis") {
        val options = JavaOptions.fromDockerCompose("redis", root / "test-env")
        assert(options.asList == List(
          "-DREDIS_PASSWORD=sqt"
        ))
      }

      test("empty") {
        val options = JavaOptions.fromDockerCompose("empty", root / "test-env")
        assert(options.asList == List())
      }
    }
  }
}
