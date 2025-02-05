package forex.http.external.oneframe

import cats.ApplicativeError
import cats.effect.Sync
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import forex.domain.model.Rate
import forex.http.external.oneframe.Converters.RateOps
import forex.http.external.oneframe.Protocol.RateResponse
import forex.http.external.oneframe.QueryParams.ratePairQueryParamEncoder
import org.http4s.client.Client
import org.http4s.{Header, Method, Request, Uri}

class OneFrameHttpClient[F[_]: Sync](token: String, httpClient: Client[F])(
    implicit applicativeError: ApplicativeError[F, Throwable]
) extends OneFrameClient[F] {

  private val baseUri   = Uri.unsafeFromString("http://localhost:8080")
  private val authToken = Header("token", token)

  def getRates(pairs: Seq[Rate.Pair]): F[Seq[Rate]] = {
    val uri     = baseUri.withPath("/rates").withMultiValueQueryParams(Map("pair" -> pairs))
    val request = Request[F](Method.GET, uri).withHeaders(authToken)

    httpClient
      .expect[List[RateResponse]](request)
      .handleErrorWith { e =>
        applicativeError.raiseError(new Exception(s"Error fetching rates: ${e.getMessage}", e))
      }
      .map(_.map(_.asRate))
  }
}

object OneFrameHttpClient {
  def apply[F[_]: Sync](token: String, httpClient: Client[F]): OneFrameClient[F] =
    new OneFrameHttpClient[F](token, httpClient)
}
