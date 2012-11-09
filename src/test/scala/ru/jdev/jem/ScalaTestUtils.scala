package ru.jdev.jem

object ScalaTestUtils {

  def SSet[T, C <: java.util.Set[T]](javaSet: C): Set[T] = Set() //Set(javaSet.toArray(new Array(javaSet.size)) : _*)

}
