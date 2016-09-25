package com.teamwizardry.refraction.common.effect;

import java.awt.Color;
import java.util.Set;
import net.minecraft.block.IGrowable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.teamwizardry.refraction.api.Effect;
import com.teamwizardry.refraction.common.light.BeamConstants;

/**
 * Created by LordSaad44
 */
public class EffectBonemeal extends Effect
{

	@Override
	public EffectType getType()
	{
		return EffectType.BEAM;
	}

	@Override
	public void run(World world, Set<BlockPos> locations)
	{
		if (!isExpired()) return;
		for (BlockPos pos : locations)
		{
			int potency = this.potency - this.getDistance(pos)*BeamConstants.DISTANCE_LOSS;
			if (world.getBlockState(pos).getBlock() instanceof IGrowable)
			{
				for (int i = 0; i < (3 * potency / 32); i++)
					ItemDye.applyBonemeal(new ItemStack(Items.DYE), world, pos);
			}
		}
	}

	@Override
	public Color getColor()
	{
		return Color.GREEN;
	}
}
