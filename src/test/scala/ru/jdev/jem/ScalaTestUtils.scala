package ru.jdev.jem

import scala.collection.JavaConversions._

object ScalaTestUtils {

  def SSet[T, C <: java.util.Set[T]](javaSet: C): scala.collection.Set[T] = javaSet.toSet[T]
  def SList[T, C <: java.util.List[T]](javaList: C): scala.collection.Iterable[T] = javaList.toList

}
