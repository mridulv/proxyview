package com.proxyview.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }

import java.util.concurrent.TimeUnit
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

case class RetryConfig(
  attempts: Int = 100,
  delay: FiniteDuration = FiniteDuration(10.seconds.toSeconds, TimeUnit.SECONDS)) {

  def overallDelay: FiniteDuration = FiniteDuration(delay.toSeconds * (attempts + 1), TimeUnit.SECONDS)

}

object RetryableHttpClient {

  class SingleRequestClient(implicit system: ActorSystem) {

    def request(request: HttpRequest): Future[HttpResponse] = {
      Http().singleRequest(request)
    }

  }

  def apply(implicit system: ActorSystem): SingleRequestClient = {
    new SingleRequestClient
  }

  def apply(config: RetryConfig)(implicit system: ActorSystem, ex: ExecutionContext): SingleRequestClient = {
    new SingleRequestClient {

      override def request(request: HttpRequest): Future[HttpResponse] = {
        akka.pattern.retry(
          attempt = () => super.request(request),
          attempts = config.attempts,
          delay = config.delay)(ex, system.scheduler)
      }
    }
  }
}
