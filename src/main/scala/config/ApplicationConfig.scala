package config

import config.ApplicationConfig.*
import domain.MediaTypes
import zio.{Config, ConfigProvider, IO, ZLayer}
import zio.config.*
import zio.config.magnolia.*

trait ApplicationConfigAlg {
  def hoconConfig: IO[Config.Error, HoconConfig]
}

final case class ApplicationConfig(
                                    private val source: ConfigProvider
                                  ) extends ApplicationConfigAlg {

  private val exampleConfigAutomaticDerivation: zio.Config[HoconConfig] = deriveConfig[HoconConfig].mapKey(toKebabCase) //defaults to CamelCase

  val hoconConfig: IO[Config.Error, HoconConfig] = source.load(exampleConfigAutomaticDerivation)

}

object ApplicationConfig {
  final case class ExampleConfig(
                                  intNumber: Int,
                                  bigDecimalNumber: BigDecimal,
                                  floatNumber: Float,
                                  stringText: String
                                )

  final case class HoconConfig(
                                exampleConfigKebabCase: ExampleConfig,
                                media: MediaTypes
                              )

  val live: ZLayer[ConfigProvider, Nothing, ApplicationConfigAlg] =
    zio.ZLayer.fromFunction((source: ConfigProvider) => ApplicationConfig(source) )
}
