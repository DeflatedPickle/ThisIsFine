/* Copyright (c) 2021-2022 DeflatedPickle under the MIT license */

package com.deflatedpickle.thisisfine

import net.minecraft.block.AbstractFireBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.FireBlock
import net.minecraft.block.FluidBlock
import net.minecraft.block.TntBlock
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.GameRules
import net.minecraft.world.World
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.spongepowered.asm.mixin.Overwrite
import java.util.Random
import kotlin.math.min
import kotlin.math.roundToInt

@Suppress("UNUSED")
object ThisIsFine : ModInitializer {
    private const val MOD_ID = "$[id]"
    private const val NAME = "$[name]"
    private const val GROUP = "$[group]"
    private const val AUTHOR = "$[author]"
    private const val VERSION = "$[version]"

    override fun onInitialize(mod: ModContainer) {
        println(listOf(MOD_ID, NAME, GROUP, AUTHOR, VERSION))
    }

    fun getSpreadChance(fireBlock: FireBlock, state: BlockState) =
        if (state.contains(Properties.WATERLOGGED) && state.get(Properties.WATERLOGGED)) 0
        else if (
            state.block is AbstractFireBlock ||
            state.block is FluidBlock ||
            state.block.blastResistance > 100
        ) 0
        else if (state.block in fireBlock.spreadChances) fireBlock.spreadChances.getInt(state.block)
        else 100 - (state.block.settings.hardness / 10).roundToInt() - (state.block.settings.resistance).roundToInt()

    // mostly copied from vanilla
    // edited to stop floating fire spread
    fun scheduledTick(fireBlock: FireBlock, state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        var tempState = state
        var blockPos: Boolean
        world.scheduleBlockTick(pos, fireBlock, FireBlock.getFireTickDelay(world.random))
        if (!world.gameRules.getBoolean(GameRules.DO_FIRE_TICK)) {
            return
        }
        if (!tempState.canPlaceAt(world, pos)) {
            world.removeBlock(pos, false)
        }
        val blockState = world.getBlockState(pos.down())
        val bl = blockState.isIn(world.dimension.infiniburnBlocks)
        val i = tempState.get(FireBlock.AGE)
        if (!bl && world.isRaining && fireBlock.isRainingAround(
                world,
                pos
            ) && random.nextFloat() < 0.2f + i.toFloat() * 0.03f
        ) {
            world.removeBlock(pos, false)
            return
        }
        val j = min(15, i + random.nextInt(3) / 2)
        if (i != j) {
            tempState = tempState.with(FireBlock.AGE, j) as BlockState
            world.setBlockState(pos, tempState, Block.NO_REDRAW)
        }
        if (!bl) {
            if (!fireBlock.areBlocksAroundFlammable(world, pos)) {
                val blockPos2 = pos.down()
                if (!world.getBlockState(blockPos2).isSideSolidFullSquare(world, blockPos2, Direction.UP) || i > 3) {
                    world.removeBlock(pos, false)
                }
                return
            }
            if (i == 15 && random.nextInt(4) == 0 && !(fireBlock as AbstractFireBlock).isFlammable(world.getBlockState(pos.down()))) {
                world.removeBlock(pos, false)
                return
            }
        }
        val k = if (world.hasHighHumidity(pos).also { blockPos = it }) -50 else 0

        if (!world.getBlockState(pos.east().down()).isAir) {
            fireBlock.trySpreadingFire(world, pos.east(), 300 + k, random, i)
        }

        if (!world.getBlockState(pos.west().down()).isAir) {
            fireBlock.trySpreadingFire(world, pos.west(), 300 + k, random, i)
        }

        fireBlock.trySpreadingFire(world, pos.down(), 250 + k, random, i)

        // removing this if enables vertical fire spread
        // vertical fire spread causes fire piles
        // fire piles will freeze your computer
        if (!world.getBlockState(pos.up()).isAir) {
            fireBlock.trySpreadingFire(world, pos.up(), 250 + k, random, i)
        }

        if (!world.getBlockState(pos.north().down()).isAir) {
            fireBlock.trySpreadingFire(world, pos.north(), 300 + k, random, i)
        }

        if (!world.getBlockState(pos.south().down()).isAir) {
            fireBlock.trySpreadingFire(world, pos.south(), 300 + k, random, i)
        }

        val mutable = BlockPos.Mutable()
        for (l in -1..1) {
            for (m in -1..1) {
                for (n in -1..4) {
                    if (l == 0 && n == 0 && m == 0) continue
                    var o = 100
                    if (n > 1) {
                        o += (n - 1) * 100
                    }
                    mutable[pos, l, n] = m
                    val p: Int = fireBlock.getBurnChance(world, mutable)
                    if (p <= 0) continue
                    var q = (p + 40 + world.difficulty.id * 7) / (i + 30)
                    if (blockPos) {
                        q /= 2
                    }
                    if (q <= 0 || random.nextInt(o) > q || world.isRaining && fireBlock.isRainingAround(
                            world,
                            mutable
                        )
                    ) continue
                    val r = min(15, i + random.nextInt(5) / 4)
                    world.setBlockState(mutable, fireBlock.getStateWithAge(world, mutable, r), Block.NOTIFY_ALL)
                }
            }
        }
    }

    @Overwrite
    // mostly copied from vanilla
    // edited to stop fire spreading into blocks
    fun trySpreadingFire(fireBlock: FireBlock, world: World, pos: BlockPos, spreadFactor: Int, rand: Random, currentAge: Int) {
        val i: Int = this.getSpreadChance(fireBlock, world.getBlockState(pos))
        if (rand.nextInt(spreadFactor) < i) {
            val blockState = world.getBlockState(pos)
            if (rand.nextInt(currentAge + 10) < 5 && !world.hasRain(pos)) {
                val j = (currentAge + rand.nextInt(5) / 4).coerceAtMost(15)

                if (world.getBlockState(pos).isAir) {
                    world.setBlockState(pos, fireBlock.getStateWithAge(world, pos, j), Block.NOTIFY_ALL)
                }
            }
            val j = blockState.block
            if (j is TntBlock) {
                TntBlock.primeTnt(world, pos)
            }
        }
    }
}
