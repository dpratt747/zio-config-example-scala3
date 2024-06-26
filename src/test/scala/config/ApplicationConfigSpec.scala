package config

import domain.MediaTypes
import domain.MediaTypes.DVD
import domain.newtypes.Newtypes.StringNewtype
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
          config <- ZIO.service[ApplicationConfigAlg]
          hoconConfig <- config.hoconConfig
          exampleConfig = hoconConfig.exampleConfigKebabCase
        } yield assertTrue(
          exampleConfig.intNumber == 10,
          exampleConfig.bigDecimalNumber == 2000,
          exampleConfig.floatNumber == 2.5F,
          exampleConfig.stringText == "hello world",
          exampleConfig.zioPreludeNewtype == StringNewtype("This is a custom prelude type"),
          hoconConfig.ironTypes.positiveInt == 20
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
               |    zio-prelude-newtype = "String newtype"
               |}
               |media = "DVD"
               |iron-types {
               |    positive-int = 20
               |}
               |""".stripMargin
          )

        (for {
          config <- ZIO.service[ApplicationConfigAlg]
          hoconConfig <- config.hoconConfig
          exampleConfig = hoconConfig.exampleConfigKebabCase
        } yield assertTrue(
          exampleConfig.intNumber == 1010,
          exampleConfig.bigDecimalNumber == 22000,
          exampleConfig.floatNumber == 5.8F,
          exampleConfig.stringText == "hello world hocon string",
          exampleConfig.zioPreludeNewtype == StringNewtype("String newtype"),
          hoconConfig.media == DVD,
          hoconConfig.ironTypes.positiveInt == 20
        ))
          .provide(
            ApplicationConfig.live,
            ZLayer.succeed(invalidConfig)
          )
      },
      test("does not load successfully when the iron-types positive int is not positive") {
        val invalidConfig = TypesafeConfigProvider
          .fromHoconString(
            s"""
               |example-config-kebab-case {
               |    int-number = 1010
               |    big-decimal-number = 22000
               |    float-number = 5.8
               |    string-text = "hello world hocon string"
               |    zio-prelude-newtype = "String newtype"
               |}
               |media = "DVD"
               |iron-types {
               |    positive-int = -80
               |}
               |""".stripMargin
          )

        val expectedErrorMessage = "Invalid data at iron-types.positive-int: Should be strictly positive"

        (for {
          config <- ZIO.service[ApplicationConfigAlg]
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
                 |iron-types {
                 |    positive-int = 20
                 |}
                 |""".stripMargin
            )

          val expectedErrorMessage: String = "Invalid data at media"


          (for {
            config <- ZIO.service[ApplicationConfigAlg]
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
          config <- ZIO.service[ApplicationConfigAlg]
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
    )
}
