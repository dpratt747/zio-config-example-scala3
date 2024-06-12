package domain.newtypes

import zio.prelude.Newtype
object Newtypes {
  object StringNewtype extends Newtype[String]
  type StringNewtype = StringNewtype.Type
}
