package examples.calgos

import scala.scalanative.native
import native._, Nat._

/**
  * See [http://en.cppreference.com/w/c/algorithm/qsort] for type signatures
  *
  * void qsort( void *ptr, size_t count, size_t size, int (*comp)(const void *, const void *) );
  */
@native.extern
object CSortAlgos {
  def qsort(data  : Ptr[Unit],
            count : CSize,
            size  : CSize, 
            cmp   : CFunctionPtr2[Ptr[Unit], Ptr[Unit], CInt]) : Unit = native.extern
}

object Comparators {
  def compare_fn(a: Ptr[Unit], b: Ptr[Unit]) : CInt = {
    val _a = a.cast[Ptr[Int]]
    val _b = b.cast[Ptr[Int]]
    if (!_a < !_b) -1
    else if (!_a > !_b) 1
    else 0
  }
}

/** 
  * Allocates data on the stack and invokes the native `qsort` from the
  * `stdlib` library.
  */
object QuickSortNative {

  def elementGen(limit: Int) : Int = scala.util.Random.nextInt(limit)

  def print_array[SIZE <: Digit[_,_]](prefix: String, a: Ptr[CArray[Int, SIZE]], size : Int) = {
    var index = 0
    while(index < size) {
      println(s"$prefix -> element:[${!(a._1 + index)}]")
      index += 1
    }
  }

  def main(args: Array[String]) {
    val this_array_size = 10
    var index = 0

    // Prepare an array of random data
    val data = native.stackalloc[CArray[Int, _10]]
    while(index < this_array_size) {
      !(data._1 + index) = elementGen(100)
      index += 1
    }

    print_array[_10]("before", data, this_array_size)

    // Invoke the sorting function
    CSortAlgos.qsort(data.cast[Ptr[Unit]], 10, 4, CFunctionPtr.fromFunction2(Comparators.compare_fn))

    print_array[_10]("after", data, this_array_size)

  }
}
