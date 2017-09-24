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

  // Note: Apparently, producing a stable value is necessarily because of the following
  // issue:
  //
  // https://github.com/scala-native/scala-native/issues/910
  //
  // and we can then use it in the `Zone` otherwise, scala-native would
  // actually produce compilation errors related to failing to codegen due to
  // detected closures.
  // 
  //  [error]   Can't get function pointer to a closure with captures: List(anonfun$main$3.this) in application scala.scalanative.native.CFunctionPtr.fromFunction2({
  // [error]   {
  // [error]     (new <$anon: Function2>(anonfun$main$3.this): Function2)
  // [error]   }
  // [error] })
  // [error]      while compiling: /Users/raymondtay/ScalaNative/src/main/scala/examples/calgos/CSort.scala
  // [error]         during phase: nir
  // [error]      library version: version 2.11.11
  // 
  val cmp_fn = CFunctionPtr.fromFunction2(compare_fn)
}

object MemoryAlloc {

  def elementGen(limit: Int) : Int = scala.util.Random.nextInt(limit)

  // Note: Best thing not to create functions like this (though the abstraction
  // is appreciated); its usefulness is zero. However, if you want to have
  // functions perform throw-away computations that leverage native memory then
  // its one option though i'm not sure how good this option is.
  def generateRandomSmallArray = {
    var index = 0
    val size = 10
    val data = native.stackalloc[CArray[Int, _10]]
    while(index < size) {
      !(data._1 + index) = elementGen(100)
      index += 1
    }
    data
  }
 
  // Note: Returning a tuple of type `(Ptr[Int], Int)` is apparently not
  // allowed and instead i needed to return only the pointer to the data which
  // is of type `Ptr[Int]`.
  //
  // It is pretty canonical i guess to have memory allocated on the heap and
  // return a pointer to it which you have to free it later else there's a
  // memory-leak !
  //
  def generateRandomLargeArray = {
    var index = 0
    val size = 1000000
    val data = stdlib.malloc(sizeof[Int] * size).cast[Ptr[Int]]
    while(index < size) {
      val value = elementGen(100)
      !(data + index) = value
      index += 1
    }
    data
  }

}
/*
  * Allocates data on the stack and invokes the native `qsort` from the
  * `stdlib` library.
  */
object QuickSortNative {

  def print_array[SIZE <: Digit[_,_]](prefix: String, a: Ptr[CArray[Int, SIZE]], size : Int) = {
    var index = 0
    while(index < size) {
      println(s"$prefix -> element:[${!(a._1 + index)}]")
      index += 1
    }
  }

  def print_array2[SIZE <: Digit[_,_]](prefix: String, a: Ptr[Int], size : Int) = {
    var index = 0
    while(index < size) {
      println(s"$prefix -> element:[${!(a + index)}]")
      index += 1
    }
  }

  def main(args: Array[String]) {

    /***************************************************
      *
      *  STACK
      *
      */////////

    // Prepare an array of random data, memory should be free once it goes out
    // of scope. Canonical approach in unmanaged languages like C/C++
    val local_array_size = 10
    var index = 0
    val dataStack = native.stackalloc[CArray[Int, _10]]
    while(index < local_array_size) {
      !(dataStack._1 + index) = MemoryAlloc.elementGen(100)
      index += 1
    }
    print_array[_10]("before", dataStack, local_array_size)
    // Invoke the sorting function
    CSortAlgos.qsort(dataStack.cast[Ptr[Unit]], 10, 4, CFunctionPtr.fromFunction2(Comparators.compare_fn))
    print_array[_10]("after", dataStack, local_array_size)

    /***************************************************
      *
      *  MANUAL HEAP
      *
      */////////

    // Data arrays generated on heap; processed on main thread and freed in the
    // main thread. Uncomment the printlns if you are really keen to look at
    // it.
    val dataSize = 1000000
    val data = MemoryAlloc.generateRandomLargeArray
    //print_array2[_1000000]("before", data, dataSize)
    CSortAlgos.qsort(data.cast[Ptr[Unit]], dataSize, 4, CFunctionPtr.fromFunction2(Comparators.compare_fn))

    //print_array2[_1000000]("after", data, dataSize)
    stdlib.free(data.cast[Ptr[Byte]])

    /***************************************************
      *
      *  Zone (is my favourite approach)
      *
      */////////

    // Anonymous zone for semi-memory management
    Zone { implicit z =>
      var index = 0
      val size = 10
      val xs : Ptr[Int] = alloc[Int](size)
      while(index < size) {
        val value = MemoryAlloc.elementGen(100)
        !(xs + index) = value
        index += 1
      }
   
      print_array2[_10]("before", xs, size)
      CSortAlgos.qsort(xs.cast[Ptr[Unit]], size, 4, Comparators.cmp_fn)
      print_array2[_10]("after", xs, size)

    } // end of zone


  } // end of main
}

