package io.github.jugbot.meta

import scala.deriving.Mirror
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.quoted.*
import dev.architectury.hooks.level.biome.BiomeProperties.Mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Maps string names and a mao of parameters to their corresponding case class
  * @param arg Name of the case object e.g. case class Thing() means you need "Thing"
  * @param map Parameters e.g. case class Thing(valueA: String) means you need Map("valueA" -> "<anything>")
  * @return
  */
inline def valueOf[T](arg: String, map: Map[String, Any]): Option[T] =
  ${ valueOfImpl[T]('arg, 'map) }

def valueOfImpl[T: Type](arg: Expr[String], m: Expr[Map[String, Any]])(using Quotes): Expr[Option[T]] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T]
  val cases = tpe.typeSymbol.children
  def strExpr(f: String) = Literal(StringConstant(f)).asExprOf[String]

  def symbolInitExpr(sym: Symbol) = {
    val tpeExpr = TypeIdent(sym)
    val ctorSym = sym.primaryConstructor
    val initFields = sym.caseFields.map(f => '{ $m(${ strExpr(f.name) }) }.asTerm)
    val newExpr = Apply(Select(New(tpeExpr), ctorSym), initFields)
    newExpr.asExprOf[T]
  }

  def matchCaseReducer(acc: Expr[Option[T]], child: Symbol): Expr[Option[T]] = {
    val childExpr = symbolInitExpr(child)
    val childName = strExpr(child.name)
    val checkName = '{ $arg == $childName }
    val checkParam = (s: Expr[String]) => '{ $m.contains($s) }
    val checks = child.caseFields
      .map(f => f.name)
      .map(strExpr)
      .map(checkParam)
      .foldLeft[Expr[Boolean]](checkName)((acc: Expr[Boolean], b: Expr[Boolean]) => '{ $acc && $b })
    '{
      if $checks then Some($childExpr)
      else $acc
    }
  }

  val result = cases.foldLeft('{ None })(matchCaseReducer)

  report.info(prettyPrint(Printer.TreeShortCode.show(result.asTerm)))

  result.asExprOf[Option[T]]
}
