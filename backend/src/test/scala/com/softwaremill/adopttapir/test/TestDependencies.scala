package com.softwaremill.adopttapir.test

import cats.effect.IO
import com.softwaremill.adopttapir.Dependencies
import org.scalatest.{BeforeAndAfterAll, Suite}
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter

import java.io.File

trait TestDependencies extends BeforeAndAfterAll {
  self: Suite with BaseTest =>
  var dependencies: Dependencies = _

  private val stub: SttpBackendStub[IO, Fs2Streams[IO]] = AsyncHttpClientFs2Backend
    .stub[IO]
    .whenRequestMatches(_ => true)
    .thenRespond(new File("src/main/resources/temporal.zip"))

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    dependencies = {
      import cats.effect.unsafe.implicits.global

      Dependencies
        .wire(
          config = TestConfig,
          clock = testClock
        )
        .allocated
        .unsafeRunSync()
        ._1
    }
  }

  private lazy val serverStub: SttpBackend[IO, Any] =
    TapirStubInterpreter[IO, Any](stub)
      .whenServerEndpointsRunLogic(dependencies.api.allEndpoints)
      .backend()

  lazy val requests = new Requests(serverStub)
}
