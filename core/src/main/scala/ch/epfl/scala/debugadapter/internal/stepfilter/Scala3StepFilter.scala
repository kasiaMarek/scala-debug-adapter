package ch.epfl.scala.debugadapter.internal.stepfilter

import java.lang.reflect.Method
import ch.epfl.scala.debugadapter.Logger
import com.sun.jdi
import scala.util.Try
import ch.epfl.scala.debugadapter.DebuggeeRunner
import java.util.function.Consumer
import java.nio.file.Path

class Scala3StepFilter(
    bridge: Any,
    skipMethod: Method,
    logger: Logger,
    testMode: Boolean
) extends ScalaVersionStepFilter {
  override def skipMethod(method: jdi.Method): Boolean =
    skipMethod.invoke(bridge, method).asInstanceOf[Boolean]
}

object Scala3StepFilter {
  def tryLoad(
      runner: DebuggeeRunner,
      logger: Logger,
      testMode: Boolean
  ): Option[Scala3StepFilter] = {
    for {
      classLoader <- runner.evaluationClassLoader
      stepFilterTry = Try {
        val className =
          "ch.epfl.scala.debugadapter.internal.stepfilter.StepFilterBridge"
        val cls = classLoader.loadClass(className)
        val ctr = cls.getConstructor(
          classOf[Array[Path]],
          classOf[Consumer[String]],
          classOf[Boolean]
        )
        val debuggeeClasspath = runner.classPath.toArray
        val warnLogger: Consumer[String] = msg => logger.warn(msg)
        val bridge = ctr.newInstance(
          debuggeeClasspath,
          warnLogger,
          testMode: java.lang.Boolean
        )
        val skipMethod = cls.getMethods.find(m => m.getName == "skipMethod").get
        new Scala3StepFilter(bridge, skipMethod, logger, testMode)
      }
      stepFilter <- stepFilterTry.toOption
    } yield stepFilter
  }
}
