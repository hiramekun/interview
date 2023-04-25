package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId, toFunctorOps}
import com.google.common.cache.CacheBuilder
import forex.domain.model.{CurrencyPairList, Rate}
import forex.http.external.oneframe.OneFrameClient
import forex.services.rates.Algebra
import forex.services.rates.errors.Error

import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

class OneFrameImplOnMemory[F[_]: Sync](client: OneFrameClient[F]) extends Algebra[F] {
  import Cache.{concurrentHashMap => cache}

  /**
    * Get rate from cache or from OneFrame API.
    * If the cache is empty, it will retrieve all rates from OneFrame API and cache them.
    */
  override def get(pair: Rate.Pair): F[Either[Error, Rate]] =
    cache.get(pair) match {
      case Some(rate) => rate.asRight[Error].pure[F]
      case None =>
        client.getRates(CurrencyPairList.all).map {
          case Nil => Left(Error.OneFrameLookupFailed("no pair to retrieve"))
          case allRates =>
            cache.addAll(CurrencyPairList.all zip allRates)
            cache(pair).asRight
        }
    }
}

object Cache {
  val concurrentHashMap: mutable.Map[Rate.Pair, Rate] =
    CacheBuilder
      .newBuilder()
      .expireAfterWrite(90, TimeUnit.SECONDS)
      .build[Rate.Pair, Rate]()
      .asMap
      .asScala
}
