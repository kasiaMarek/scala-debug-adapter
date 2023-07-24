package ch.epfl.scala.debugadapter.internal.binary
import java.lang.Class
trait ClassType extends Type:
  def name: String

  def isObject = isPackageObject || name.endsWith("$")
  def isPackageObject = name.endsWith(".package") || name.endsWith("$package")
  def superclass: ClassType
  def interfaces: Seq[ClassType]
