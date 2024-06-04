package io.github.jugbot;

import org.apache.logging.log4j.LogManager

object ExampleMod {
    final val MOD_ID = "settlements";

    private val LOGGER = LogManager.getLogger

    def init() = {
        LOGGER.info("Hello World!")
    }
}
