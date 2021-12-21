/* Copyright (c) 2021 DeflatedPickle under the CC0 license */

package com.deflatedpickle.thisisfine

import net.fabricmc.api.ModInitializer
import net.minecraft.block.AbstractFireBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.FireBlock
import net.minecraft.block.FluidBlock
import kotlin.math.roundToInt

@Suppress("UNUSED")
object ThisIsFine : ModInitializer {
    private const val MOD_ID = "$[id]"
    private const val NAME = "$[name]"
    private const val GROUP = "$[group]"
    private const val AUTHOR = "$[author]"
    private const val VERSION = "$[version]"

    override fun onInitialize() {
        println(listOf(MOD_ID, NAME, GROUP, AUTHOR, VERSION))

        // blocks need to be added to this list of spreading chances
        // otherwise fire wont spread to them
        for (i in Blocks::class.java.declaredFields) {
            val b = i.get(Blocks::class.objectInstance) as Block
            if (/*b == Blocks.AIR || b == Blocks.BEDROCK ||*/
                b is AbstractFireBlock ||
                b is FluidBlock ||
                b.blastResistance > 100
            ) continue
            (Blocks.FIRE as FireBlock).spreadChances[b] =
                100 - (b.settings.hardness / 10).roundToInt() - (b.settings.resistance).roundToInt()
        }
    }
}
