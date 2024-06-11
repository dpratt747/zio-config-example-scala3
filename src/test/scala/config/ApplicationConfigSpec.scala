package config

import config.*
import domain.MediaTypes
import domain.MediaTypes.DVD
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.test.*
import zio.test.Assertion.*

import scala.util.{Failure, Success, Try}

object ApplicationConfigSpec extends ZIOSpecDefault {
  override def spec: Spec[Any, Any] =
    suite("ApplicationConfig")(
      test("can load successfully") {
        (for {
          config <- ZIO.service[ApplicationConfig]
          hoconConfig <- config.hoconConfig
          exampleConfig = hoconConfig.exampleConfigKebabCase
        } yield assertTrue(
          exampleConfig.intNumber == 10,
          exampleConfig.bigDecimalNumber == 2000,
          exampleConfig.floatNumber == 2.5F,
          exampleConfig.stringText == "hello world"
        ))
          .provide(
            ApplicationConfig.live,
            ZLayer.succeed(TypesafeConfigProvider.fromResourcePath())
          )
      },
      test("can load successfully with provided hocon string") {
        val invalidConfig = TypesafeConfigProvider
          .fromHoconString(
            s"""
               |example-config-kebab-case {
               |    int-number = 1010
               |    big-decimal-number = 22000
               |    float-number = 5.8
               |    string-text = "hello world hocon string"
               |}
               |media = "DVD"
               |
               |""".stripMargin
          )

        (for {
          config <- ZIO.service[ApplicationConfig]
          hoconConfig <- config.hoconConfig
          exampleConfig = hoconConfig.exampleConfigKebabCase
        } yield assertTrue(
          exampleConfig.intNumber == 1010,
          exampleConfig.bigDecimalNumber == 22000,
          exampleConfig.floatNumber == 5.8F,
          exampleConfig.stringText == "hello world hocon string",
          hoconConfig.media == DVD
        ))
          .provide(
            ApplicationConfig.live,
            ZLayer.succeed(invalidConfig)
          )
      },
      test("does not load successfully when provided invalid enum") {

        val invalidMediaGen = Gen.alphaNumericString.filterNot(string =>
          Try(MediaTypes.valueOf(string)) match
            case Failure(_) => false
            case Success(mediaType) => MediaTypes.values.contains(mediaType)
        )

        check(invalidMediaGen) { mediaString =>

          val invalidConfig = TypesafeConfigProvider
            .fromHoconString(
              s"""
                 |example-config-kebab-case {
                 |    int-number = 1010
                 |    big-decimal-number = 22000
                 |    float-number = 5.8
                 |    string-text = "hello world hocon string"
                 |}
                 |media = "$mediaString"
                 |
                 |""".stripMargin
            )

          val expectedErrorMessage: String = "Invalid data at media"


          (for {
            config <- ZIO.service[ApplicationConfig]
            _ <- config.hoconConfig
          } yield ())
            .provide(
              ApplicationConfig.live,
              ZLayer.succeed(invalidConfig)
            )
            .flip
            .map((error: Config.Error) =>
              assertTrue(error.getMessage().contains(expectedErrorMessage))
            )
        }
      },
      test("cannot load successfully") {
        val invalidConfig = TypesafeConfigProvider
          .fromHoconString(
            s"""
               |example-config {
               |    int-number = 10
               |    big-decimal-number = 2000
               |    float-number = 2.5
               |    string-text = "hello world"
               |}
               |""".stripMargin
          )

        val expectedErrorMessage: String = "Missing data"

        (for {
          appConfig <- ZIO.service[ApplicationConfig]
          _ <- appConfig.hoconConfig
        } yield ())
          .provide(
            ApplicationConfig.live,
            ZLayer.succeed(invalidConfig)
          )
          .flip
          .map((error: Config.Error) =>
            assertTrue(error.getMessage().contains(expectedErrorMessage))
          )
      }
    )
}
