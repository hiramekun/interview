package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.domain.model.CurrencyPairList
import forex.http.external.oneframe.OneFrameClient
import forex.services.rates.interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def http[F[_]: Sync](client: OneFrameClient[F]): Algebra[F] =
    new OneFrameImplOnMemory[F](CurrencyPairList.all, client)
}
