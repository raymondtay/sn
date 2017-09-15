package demo

import scala.scalanative.native
import native._
import java.nio.charset.Charset

object myio2 {
  def printToConsole(s: String) : Unit = {
    Zone { implicit z => 
      myio.printf(toCString(s))
    }
  }
}

@native.extern
object myio{
  def printf(format: native.CString, args: native.CVararg*) : native.CInt = native.extern
}

