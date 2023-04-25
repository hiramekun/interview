package forex.http.external.oneframe

import forex.domain.model.Rate
import org.http4s.QueryParamEncoder

object QueryParams {
  implicit val ratePairQueryParamEncoder: QueryParamEncoder[Rate.Pair] = QueryParamEncoder[String].contramap(_.toString)
}
