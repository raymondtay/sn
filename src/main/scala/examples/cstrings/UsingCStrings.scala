package examples.cstrings

import scala.scalanative.native
import native._
import java.nio.charset.Charset

/** 
  * This example illustrates how to use `toCString` in the current
  * implementation and the key thing here is that an implicit `Zone`
  * is needed. See [http://www.scala-native.org/en/latest/user/interop.html#memory-management]
  * for semi-auto memory management.
  *
  * The pattern here is to separate the Scala-ish components from the objects
  * that interfaces with the native libraries - seems like a good idea.
  */
object PrintToConsole {
  def print(s: String) : Unit = {
    Zone { implicit z => 
      PrintToConsoleNative.printf(toCString(s))
    }
  }

  def main(args: Array[String]) {
    print("Works!")
  }
}

@native.extern
object PrintToConsoleNative {
  def printf(format: native.CString, args: native.CVararg*) : native.CInt = native.extern
}

