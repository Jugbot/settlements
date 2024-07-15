package io.github.jugbot.meta

import scala.deriving.Mirror
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.quoted.*
import dev.architectury.hooks.level.biome.BiomeProperties.Mutable
import scala.collection.mutable.ArrayBuffer

def prettyPrint(input: String): String = {
  val stack = scala.collection.mutable.Stack[Char]()
  var res = ""
  var indent = 0

  for c <- input do
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
  res
}

inline def debugAST(inline a: Any) = ${ debugASTImpl('a) }

def debugASTImpl(a: Expr[Any])(using Quotes): Expr[String] = {
  import quotes.reflect.*

  val debugInfo = prettyPrint(Printer.TreeStructure.show(a.asTerm))
  report.info(debugInfo)

  Literal(StringConstant(debugInfo)).asExprOf[String]
}
