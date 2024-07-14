package io.github.jugbot.util

import scala.deriving.Mirror
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.quoted.*
import dev.architectury.hooks.level.biome.BiomeProperties.Mutable
import scala.collection.mutable.ArrayBuffer

def prettyPrint(input: String): String = {
  val stack = scala.collection.mutable.Stack[Char]()
  var res = ""
  var indent = 0

  for (c <- input) {
    c match {
      case '(' => 
        stack.push(c)
        res += s"$c\n${" " * stack.size}"
      case ')' => 
        stack.pop()
        res += "\n" + " " * stack.size + c
      case _ =>
        res += c
    }
  }
  res
}

inline def debugAST(inline a: Any) = ${debugASTImpl('a)}

def debugASTImpl(a: Expr[Any])(using Quotes): Expr[String] = {
  import quotes.reflect.*

  report.info(Printer.TreeStructure.show(a.asTerm))

  Literal(StringConstant(prettyPrint(Printer.TreeStructure.show(a.asTerm)))).asExprOf[String]
}

inline def valueOf[T](arg: String, map: Map[String, String]): Option[T] =
  ${ valueOfImpl[T]('arg, 'map) }

def valueOfImpl[T: Type](arg: Expr[String], map: Expr[Map[String, String]])(using Quotes): Expr[Option[T]] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T]
  val cases = tpe.typeSymbol.children

  def symbolInitExpr(sym: Symbol) = {
    val tpeExpr = TypeIdent(sym)
    val ctorSym = sym.primaryConstructor
    val newExpr = Apply(Select(New(tpeExpr), ctorSym), Nil)
    newExpr.asExprOf[T]
  }

  def matchCaseReducer(acc: Expr[Option[T]], child: Symbol): Expr[Option[T]] = {
    val childExpr = symbolInitExpr(child)
    val childName = Literal(StringConstant(child.name)).asExpr
    '{
      if ($arg == $childName) then 
        Some($childExpr) 
      else $acc
    }
  }

  val result = cases.foldLeft('{None})(matchCaseReducer)

  report.info(result.show)

  result.asExprOf[Option[T]]
}
