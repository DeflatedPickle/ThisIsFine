/* Copyright (c) 2022 DeflatedPickle under the MIT license */

package com.deflatedpickle.thisisfine.mixins;

import com.deflatedpickle.thisisfine.ThisIsFine;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.random.RandomGenerator;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@SuppressWarnings("UnusedMixin")
@Mixin(FireBlock.class)
public class MixinFireBlock {
  /**
   * @author DeflatedPickle
   * @reason lets fire spread to everything
   */
  @Overwrite
  public final boolean areBlocksAroundFlammable(BlockView world, BlockPos pos) {
    return true;
  }

  /**
   * @author DeflatedPickle
   * @reason adds chances for fire spreading to every block
   */
  @Overwrite
  public final int getSpreadChance(BlockState state) {
    return ThisIsFine.INSTANCE.getSpreadChance((FireBlock) (Object) this, state);
  }

  /**
   * @author DeflatedPickle
   * @reason stops floating fire spread
   */
  @Overwrite
  public void scheduledTick(
      BlockState state, ServerWorld world, BlockPos pos, RandomGenerator random) {
    ThisIsFine.INSTANCE.scheduledTick((FireBlock) (Object) this, state, world, pos, random);
  }

  /**
   * @author DeflatedPickle
   * @reason stops fire spreading into blocks
   */
  @Overwrite
  public final void trySpreadingFire(
      World world, BlockPos pos, int spreadFactor, RandomGenerator rand, int currentAge) {
    ThisIsFine.INSTANCE.trySpreadingFire(
        (FireBlock) (Object) this, world, pos, spreadFactor, rand, currentAge);
  }
}
