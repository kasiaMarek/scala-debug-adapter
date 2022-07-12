package ch.epfl.scala.debugadapter.internal

import ch.epfl.scala.debugadapter.internal.scalasig._
import com.microsoft.java.debug.core.adapter.{
  StepFilterProvider => JavaStepFilterProvider
}
import com.microsoft.java.debug.core.protocol.Requests.StepFilters
import com.sun.jdi.LocalVariable
import com.sun.jdi.Method
import com.sun.jdi.ReferenceType

import scala.collection.JavaConverters._

class StepFilterProvider(sourceLookUp: SourceLookUpProvider)
    extends JavaStepFilterProvider() {
  override def skip(method: Method, filters: StepFilters): Boolean = {
    if (method.isBridge || super.skip(method, filters)) {
      true
    } else if (isLocalMethod(method) || isLocalClass(method.declaringType())) {
      false
    } else {
      val fqcn = method.declaringType().name()
      val res =
        sourceLookUp.getScalaSig(fqcn).map(skip(method, _))

      res match {
        case None => println(s"No ScalaSig found for $method")
        case Some(true) => println(s"Skipping $method")
        case Some(false) => ()
      }

      res.getOrElse(false)
    }
  }

  private def isLocalMethod(method: Method): Boolean =
    method.name().contains("$anonfun$")

  private def isLocalClass(tpe: ReferenceType): Boolean =
    tpe.name().contains("$anon$")

  private def skip(method: Method, scalaSig: ScalaSig): Boolean = {
    val matchingSymbols = scalaSig.entries
      .collect { case m: MethodSymbol if m.isMethod => m }
      .filter(matchSymbol(method, _))

    if (matchingSymbols.size > 1) {
      println(s"WARNING: found ${matchingSymbols.size} matching symbols")
      matchingSymbols.foreach { s =>
        println(s"${s.info.info.get}")
      }
    }

    matchingSymbols.headOption.forall(skip)
  }

  private def skip(scalaMethod: MethodSymbol): Boolean = {
    scalaMethod.isAccessor
  }

  private def matchSymbol(
      javaMethod: Method,
      scalaMethod: MethodSymbol
  ): Boolean = {
    if (scalaMethod.aliasRef.nonEmpty)
      println(
        s"aliasRef for ${scalaMethod.name}: ${scalaMethod.aliasRef}"
      )
    if (scalaMethod.isSyntheticMethod)
      println(s"${scalaMethod.name} isSyntheticMethod")
    if (scalaMethod.isMonomorphic)
      println(s"${scalaMethod.name} isMonomorphic")
    if (scalaMethod.isMixedIn) println(s"${scalaMethod.name} isMixedIn")

    javaMethod.name == scalaMethod.name &&
    matchArguments(javaMethod, scalaMethod.info.info.get) &&
    matchOwner(javaMethod.declaringType(), scalaMethod.parent.get)
  }

  private def matchOwner(
      javaClass: ReferenceType,
      scalaOwner: Symbol
  ): Boolean = {
    // println(s"matchOwner(${javaClass.name()}, ${scalaOwner.name})")
    val fqcn = javaClass.name()
    // TODO improve
    getOwners(scalaOwner).reverse
      .foldLeft(Option(fqcn)) { (acc, sym) =>
        for (fqcn <- acc if fqcn.contains(sym.name)) yield {
          fqcn
            .split(sym.name)
            .drop(1)
            .mkString(sym.name)
        }
      }
      .exists { remainder =>
        remainder.forall(c => c.isDigit || c == '$')
      }
  }

  private def getOwners(sym: Symbol): Seq[Symbol] = {
    Iterator
      .iterate(Option(sym))(opt => opt.flatMap(_.parent))
      .takeWhile(_.isDefined)
      .flatten
      .toSeq
  }

  private def matchArguments(
      javaMethod: Method,
      methodType: scalasig.Type
  ): Boolean = {
    val javaArgs = javaMethod.arguments().asScala.toSeq
    val scalaArgs = extractArguments(methodType)
    javaArgs.size == scalaArgs.size &&
    javaArgs.zip(scalaArgs).forall { case (javaArg, scalaArg) =>
      matchArgument(javaArg, scalaArg)
    }
  }

  private def matchArgument(
      javaArg: LocalVariable,
      scalaArg: Symbol
  ): Boolean = {
    javaArg.name() == scalaArg.name
  }

  private def extractArguments(methodType: scalasig.Type): Seq[Symbol] = {
    methodType match {
      case m: MethodType => m.paramRefs
      case m: NullaryMethodType => Seq.empty
      case m: PolyType => extractArguments(m.typeRef.get)
      case other =>
        val className = other.getClass.getSimpleName()
        throw new Exception(s"unexpected type found: $className")
    }
  }
}
