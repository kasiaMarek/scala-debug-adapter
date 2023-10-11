package ch.epfl.scala.debugadapter.internal.binary
trait ClassType extends Type:
  def name: String
  def isInterface: Boolean
  def superclass: Option[ClassType]
  def interfaces: Seq[ClassType]
  def declaredMethod(name: String, sig: String): Option[Method]

  def isObject = isPackageObject || name.endsWith("$")
  def isPackageObject = name.endsWith(".package") || name.endsWith("$package")
