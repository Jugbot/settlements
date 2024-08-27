package io.github.jugbot.util

import com.mojang.brigadier.StringReader
import dev.architectury.utils.GameInstance
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument
import net.minecraft.commands.arguments.item.ItemPredicateArgument
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.pattern.BlockInWorld

import scala.util.parsing.combinator.JavaTokenParsers

object BooleanLogicParser extends JavaTokenParsers {
  private type Returns = (String => Boolean) => Boolean

  private def expr: Parser[Returns] = orExpr
  private def orExpr: Parser[Returns] =
    andExpr * ("or" ^^^ { (a: Returns, b: Returns) => (x: String => Boolean) => a(x) || b(x) })
  private def andExpr: Parser[Returns] =
    notExpr * ("and" ^^^ { (a: Returns, b: Returns) => (x: String => Boolean) => a(x) && b(x) })
  private def notExpr: Parser[Returns] = "not" ~> simpleExpr ^^ { a => (x: String => Boolean) => !a(x) } | simpleExpr
  // TODO: Handle strings with spaces in it
  private def simpleExpr: Parser[Returns] = "(" ~> expr <~ ")" | """[^\s()]+""".r ^^ { tag => (x: String => Boolean) => x(tag) }

  def parseAndEvaluate(input: String): Returns =
    parseAll(expr, input) match {
      case Success(result, _) => result
      case failure: NoSuccess => throw new IllegalArgumentException(s"Failed to parse expression: '$input'\n${failure.msg}")
    }
}

private def parseOperations(query: String, predicate: Function[String, Boolean]) =
  BooleanLogicParser.parseAndEvaluate(query)(predicate)

private lazy val commandBuildContext =
  CommandBuildContext.simple(GameInstance.getServer.registryAccess(),
                             GameInstance.getServer.getWorldData.enabledFeatures()
  )

private def parseBlockQuery(blockQuery: String) =
  BlockPredicateArgument(commandBuildContext).parse(StringReader(blockQuery))

def blockPredicate(blockQuery: String) = (blockState: BlockInWorld) =>
  parseOperations(blockQuery, parseBlockQuery(_).test(blockState))

private def parseItemQuery(itemQuery: String) =
  ItemPredicateArgument(commandBuildContext).parse(StringReader(itemQuery))

def itemPredicate(itemQuery: String) = (itemStack: ItemStack) =>
  parseOperations(itemQuery, parseItemQuery(_).test(itemStack))
