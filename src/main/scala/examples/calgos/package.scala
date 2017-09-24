package examples

import scala.scalanative.native
import native._, Nat._

package object calgos {

  type _10     = Digit[_1, _0]
  type _100    = Digit[_1, Digit[_0, _0]]
  type _10000  = Digit[_1, Digit[_0, Digit[_0, Digit[_0, _0]]]]
  type _100000 = Digit[_1, Digit[_0, Digit[_0, Digit[_0, Digit[_0, _0]]]]]
  type _1000000 = Digit[_1, Digit[_0, Digit[_0, Digit[_0, Digit[_0, Digit[_0,_0]]]]]]

}
