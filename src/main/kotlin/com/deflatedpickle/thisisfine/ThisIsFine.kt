/* Copyright (c) 2021 DeflatedPickle under the CC0 license */

package com.deflatedpickle.thisisfine

import net.fabricmc.api.ModInitializer

@Suppress("UNUSED")
object ThisIsFine : ModInitializer {
    private const val MOD_ID = "$[id]"
    private const val NAME = "$[name]"
    private const val GROUP = "$[group]"
    private const val AUTHOR = "$[author]"
    private const val VERSION = "$[version]"

    override fun onInitialize() {
        println(listOf(MOD_ID, NAME, GROUP, AUTHOR, VERSION))
    }
}
