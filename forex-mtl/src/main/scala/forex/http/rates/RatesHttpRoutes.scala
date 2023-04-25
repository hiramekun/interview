package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private val httpRoutes: AuthedRoutes[String, F] = AuthedRoutes.of {
    case GET -> Root / "rates" :? FromQueryParam(from) +& ToQueryParam(to) as _ =>
      rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap(Sync[F].fromEither).flatMap { rate =>
        Ok(rate.asGetApiResponse)
      }
  }

  val routes: AuthedRoutes[String, F] = httpRoutes
}
