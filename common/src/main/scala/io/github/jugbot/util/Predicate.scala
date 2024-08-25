package io.github.jugbot.util

import com.mojang.brigadier.StringReader
import dev.architectury.utils.GameInstance
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument
import net.minecraft.commands.arguments.item.ItemPredicateArgument

private lazy val commandBuildContext =
  CommandBuildContext.simple(GameInstance.getServer.registryAccess(),
                             GameInstance.getServer.getWorldData.enabledFeatures()
  )

def parseBlockQuery(blockQuery: String) =
  BlockPredicateArgument(commandBuildContext).parse(StringReader(blockQuery))

def blockPredicate(blockQuery: String) = parseBlockQuery(blockQuery).test _

def parseItemQuery(itemQuery: String) =
  ItemPredicateArgument(commandBuildContext).parse(StringReader(itemQuery))

def itemPredicate(itemQuery: String) = parseItemQuery(itemQuery).test _
