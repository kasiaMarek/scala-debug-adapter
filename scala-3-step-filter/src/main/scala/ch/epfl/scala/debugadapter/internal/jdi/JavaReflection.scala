package ch.epfl.scala.debugadapter.internal.jdi

import scala.reflect.ClassTag

trait JavaReflection:
  def obj: Any

  def invokeMethod[T](name: String): T =
    val method = obj.getClass.getMethod(name)
    method.invoke(obj).asInstanceOf[T]
