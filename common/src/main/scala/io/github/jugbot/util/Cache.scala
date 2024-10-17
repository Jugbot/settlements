package io.github.jugbot.util

def memoizedValue[V](getter: () => V, validator: V => Boolean): () => V = {
  var value: Option[V] = None
  () =>
    value match {
      case Some(value) if validator(value) => value
      case _ =>
        val newValue = getter()
        value = Some(newValue)
        newValue
    }
}
