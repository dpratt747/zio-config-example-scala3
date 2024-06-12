package config

import config.ApplicationConfig.*
import domain.MediaTypes
import domain.newtypes.Newtypes
import domain.newtypes.Newtypes.StringNewtype
import io.github.iltotore.iron.constraint.all.Positive
import zio.{Config, ConfigProvider, IO, ZLayer}
import zio.config.*
import zio.config.magnolia.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.*

trait ApplicationConfigAlg {
  def hoconConfig: IO[Config.Error, HoconConfig]
}

final case class ApplicationConfig(
                                    private val source: ConfigProvider
                                  ) extends ApplicationConfigAlg {

  private given stringNewtypeDescriptor: DeriveConfig[Newtypes.StringNewtype.Type] =
    DeriveConfig(zio.Config.string.map(StringNewtype.apply))

  private given positiveIntDescriptor: DeriveConfig[Int :| Positive] =
    DeriveConfig(zio.Config.int.mapOrFail(num =>
      num.refineEither[Positive].left.map(errorString => Config.Error.InvalidData(message = errorString))
    ))

  private val exampleConfigAutomaticDerivation: zio.Config[HoconConfig] = deriveConfig[HoconConfig].mapKey(toKebabCase) //defaults to CamelCase

  val hoconConfig: IO[Config.Error, HoconConfig] = source.load(exampleConfigAutomaticDerivation)

}

object ApplicationConfig {
  final case class ExampleConfig(
                                  intNumber: Int,
                                  bigDecimalNumber: BigDecimal,
                                  floatNumber: Float,
                                  stringText: String,
                                  zioPreludeNewtype: StringNewtype
                                )

  final case class IronConfig(
                               positiveInt: Int :| Positive
                             )

  final case class HoconConfig(
                                exampleConfigKebabCase: ExampleConfig,
                                media: MediaTypes,
                                ironTypes: IronConfig
                              )

  val live: ZLayer[ConfigProvider, Nothing, ApplicationConfigAlg] =
    zio.ZLayer.fromFunction((source: ConfigProvider) => ApplicationConfig(source))
}
