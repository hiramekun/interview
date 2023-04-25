package forex

import cats.data.{Kleisli, OptionT}
import cats.effect.{Concurrent, Timer}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import forex.config.ApplicationConfig
import forex.http.external.oneframe.{RateClient, RateHttpClient}
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.{AutoSlash, Timeout}
import org.http4s.util.CaseInsensitiveString

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig, client: Client[F]) {

  private val oneFrameToken: String   = "10dc303535874aeccc86a8251e6992f5"
  private val forexValidToken: String = "token" // TODO: encrypt

  private val ratesClient: RateClient[F] = RateHttpClient[F](oneFrameToken, client)

  private val ratesService: RatesService[F] = RatesServices.http[F](ratesClient)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: AuthedRoutes[String, F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]
  private val authMiddleware: AuthMiddleware[F, String] = tokenAuthMiddleware(forexValidToken)

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: AuthedRoutes[String, F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(authMiddleware(http)).orNotFound)

  private def tokenAuthMiddleware(validToken: String): AuthMiddleware[F, String] = {
    val dsl = Http4sDsl[F]
    import dsl._

    val onFailure: AuthedRoutes[String, F] = Kleisli { (_: AuthedRequest[F, String]) =>
      OptionT.liftF(Forbidden("Invalid token"))
    }

    val tokenAuthenticator: Kleisli[F, Request[F], Either[String, String]] = Kleisli { (req: Request[F]) =>
      req.headers.get(CaseInsensitiveString("token")) match {
        case Some(header) if header.value == validToken => header.value.asRight[String].pure
        case _                                          => "Invalid token".asLeft[String].pure
      }
    }

    AuthMiddleware(tokenAuthenticator, onFailure)
  }
}
