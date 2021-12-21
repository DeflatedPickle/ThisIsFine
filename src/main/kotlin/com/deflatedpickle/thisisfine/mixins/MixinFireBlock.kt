/* Copyright (c) 2021 DeflatedPickle under the CC0 license */

@file:Suppress("unused")

package com.deflatedpickle.thisisfine.mixins

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.FireBlock
import net.minecraft.block.TntBlock
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.GameRules
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite
import org.spongepowered.asm.mixin.Shadow
import java.util.Random
import kotlin.math.min

@Mixin(FireBlock::class)
abstract class MixinFireBlock {
    @Shadow abstract fun getSpreadChance(state: BlockState): Int
    @Shadow abstract fun getStateWithAge(world: WorldAccess, pos: BlockPos, age: Int): BlockState?
    @Shadow abstract fun isRainingAround(world: World, pos: BlockPos): Boolean
    @Shadow abstract fun isFlammable(state: BlockState?): Boolean
    @Shadow abstract fun getBurnChance(world: WorldView, pos: BlockPos): Int

    @Overwrite
    fun areBlocksAroundFlammable(world: BlockView, pos: BlockPos) = true

    @Overwrite
    // mostly copied from vanilla
    // edited to stop floating fire spread
    open fun scheduledTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        var state = state
        var blockPos: Boolean
        world.createAndScheduleBlockTick(pos, (this as Block), FireBlock.getFireTickDelay(world.random))
        if (!world.gameRules.getBoolean(GameRules.DO_FIRE_TICK)) {
            return
        }
        if (!state.canPlaceAt(world, pos)) {
            world.removeBlock(pos, false)
        }
        val blockState = world.getBlockState(pos.down())
        val bl = blockState.isIn(world.dimension.infiniburnBlocks)
        val i = state.get(FireBlock.AGE)
        if (!bl && world.isRaining && this.isRainingAround(
                world,
                pos
            ) && random.nextFloat() < 0.2f + i.toFloat() * 0.03f
        ) {
            world.removeBlock(pos, false)
            return
        }
        val j = min(15, i + random.nextInt(3) / 2)
        if (i != j) {
            state = state.with(FireBlock.AGE, j) as BlockState
            world.setBlockState(pos, state, Block.NO_REDRAW)
        }
        if (!bl) {
            if (!areBlocksAroundFlammable(world, pos)) {
                val blockPos2 = pos.down()
                if (!world.getBlockState(blockPos2).isSideSolidFullSquare(world, blockPos2, Direction.UP) || i > 3) {
                    world.removeBlock(pos, false)
                }
                return
            }
            if (i == 15 && random.nextInt(4) == 0 && !this.isFlammable(world.getBlockState(pos.down()))) {
                world.removeBlock(pos, false)
                return
            }
        }
        val k = if (world.hasHighHumidity(pos).also { blockPos = it }) -50 else 0

        if (!world.getBlockState(pos.east().down()).isAir) {
            trySpreadingFire(world, pos.east(), 300 + k, random, i)
        }

        if (!world.getBlockState(pos.west().down()).isAir) {
            trySpreadingFire(world, pos.west(), 300 + k, random, i)
        }

        trySpreadingFire(world, pos.down(), 250 + k, random, i)

        // removing this if enables vertical fire spread
        // vertical fire spread causes fire piles
        // fire piles will freeze your computer
        if (!world.getBlockState(pos.up()).isAir) {
            trySpreadingFire(world, pos.up(), 250 + k, random, i)
        }

        if (!world.getBlockState(pos.north().down()).isAir) {
            trySpreadingFire(world, pos.north(), 300 + k, random, i)
        }

        if (!world.getBlockState(pos.south().down()).isAir) {
            trySpreadingFire(world, pos.south(), 300 + k, random, i)
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
                    val p: Int = this.getBurnChance(world, mutable)
                    if (p <= 0) continue
                    var q = (p + 40 + world.difficulty.id * 7) / (i + 30)
                    if (blockPos) {
                        q /= 2
                    }
                    if (q <= 0 || random.nextInt(o) > q || world.isRaining && this.isRainingAround(
                            world,
                            mutable
                        )
                    ) continue
                    val r = min(15, i + random.nextInt(5) / 4)
                    world.setBlockState(mutable, getStateWithAge(world, mutable, r), Block.NOTIFY_ALL)
                }
            }
        }
    }

    @Overwrite
    // mostly copied from vanilla
    // edited to stop fire spreading into blocks
    fun trySpreadingFire(world: World, pos: BlockPos, spreadFactor: Int, rand: Random, currentAge: Int) {
        val i: Int = this.getSpreadChance(world.getBlockState(pos))
        if (rand.nextInt(spreadFactor) < i) {
            val blockState = world.getBlockState(pos)
            if (rand.nextInt(currentAge + 10) < 5 && !world.hasRain(pos)) {
                val j = (currentAge + rand.nextInt(5) / 4).coerceAtMost(15)

                if (world.getBlockState(pos).isAir) {
                    world.setBlockState(pos, this.getStateWithAge(world, pos, j), Block.NOTIFY_ALL)
                }
            }
            val j = blockState.block
            if (j is TntBlock) {
                TntBlock.primeTnt(world, pos)
            }
        }
    }
}
