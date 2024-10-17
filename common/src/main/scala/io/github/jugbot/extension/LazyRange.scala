package io.github.jugbot.extension

object LazyRange {
  extension (n: Int) {

    /**
     * Lazy range from start to inclusive end
     */
    def toLazy(end: Int): LazyList[Int] =
      n.untilLazy(end + 1)

    /**
     * Lazy range from start to exclusive end
     */
    def untilLazy(end: Int): LazyList[Int] =
      LazyList.iterate(n, end)(_ + 1)
  }
}
