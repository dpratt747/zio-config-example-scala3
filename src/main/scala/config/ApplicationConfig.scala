package config

import domain.MediaTypes
import zio.{Config, ConfigProvider, IO, ZLayer}
import zio.config.*
import zio.config.magnolia.*


final case class ApplicationConfig(
                                    private val source: ConfigProvider
                                  ) {

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

  private val exampleConfigAutomaticDerivation: zio.Config[HoconConfig] = deriveConfig[HoconConfig].mapKey(toKebabCase) //defaults to CamelCase

  val hoconConfig: IO[Config.Error, HoconConfig] = source.load(exampleConfigAutomaticDerivation)

}

object ApplicationConfig {
  val live: ZLayer[ConfigProvider, Nothing, ApplicationConfig] =
    zio.ZLayer.fromFunction((source: ConfigProvider) => ApplicationConfig(source) )
}
