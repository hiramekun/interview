package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits.toFunctorOps
import com.google.common.cache.CacheBuilder
import forex.domain.model.Rate
import forex.http.external.oneframe.OneFrameClient
import forex.services.rates.errors

import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

class OneFrameImplOnMemory[F[_]: Sync](client: OneFrameClient[F]) extends OneFrameImpl[F](client) {
  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] =
    Cache.concurrentHashMap.get(pair) match {
      case Some(rate) => Sync[F].delay(Right(rate))
      case None =>
        super.get(pair).map {
          case Right(rate) =>
            Cache.concurrentHashMap.put(pair, rate)
            Right(rate)
          case Left(error) => Left(error)
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
