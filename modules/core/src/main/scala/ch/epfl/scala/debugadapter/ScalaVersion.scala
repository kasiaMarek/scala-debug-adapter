package ch.epfl.scala.debugadapter

case class ScalaVersion(value: String) {
  def isScala2: Boolean = value.startsWith("2")
  def isScala3: Boolean = value.startsWith("3")
  def isScala212: Boolean = value.startsWith("2.12")
  def isScala213: Boolean = value.startsWith("2.13")
  def isScala33: Boolean = value.startsWith("3.3")
  def isScala34: Boolean = value.startsWith("3.4")

  def binaryVersion: String = if (isScala3) "3" else if (isScala213) "2.13" else "2.12"

  def isRelease: Boolean = !value.contains("-")

  override def toString: String = value
}

object ScalaVersion {
  val `2.11` = ScalaVersion(value = "2.11.12")
  val `2.12` = ScalaVersion(BuildInfo.scala212)
  val `2.13` = ScalaVersion(BuildInfo.scala213)
  val `3.0` = ScalaVersion(BuildInfo.scala30)
  val `3.3` = ScalaVersion(BuildInfo.scala33)
  val `3.4` = ScalaVersion(BuildInfo.scala34)
}
