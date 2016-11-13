package com.teamwizardry.refraction.common.block;

import com.teamwizardry.librarianlib.client.util.TooltipHelper;
import com.teamwizardry.librarianlib.common.base.block.BlockModContainer;
import com.teamwizardry.refraction.api.IBeamHandler;
import com.teamwizardry.refraction.api.ILightSource;
import com.teamwizardry.refraction.common.light.Beam;
import com.teamwizardry.refraction.common.light.ReflectionTracker;
import com.teamwizardry.refraction.common.tile.TileMagnifier;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by LordSaad44
 */
public class BlockMagnifier extends BlockModContainer implements IBeamHandler {

	public BlockMagnifier() {
		super("magnifier", Material.IRON);
		setHardness(1F);
		setSoundType(SoundType.METAL);
	}

	@Override
	public void handleBeams(World world, BlockPos pos, Beam... beams) {
		TileEntity te = world.getTileEntity(pos);
		if (te != null)
			((TileMagnifier) te).handle(beams);
	}
	
	@Override
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.TRANSLUCENT;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
		TooltipHelper.addToTooltip(tooltip, "simple_name.refraction:" + getRegistryName().getResourcePath());
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState blockState) {
		return false;
	}
	
	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		TileEntity entity = world.getTileEntity(pos);
		if (entity instanceof ILightSource)
			ReflectionTracker.getInstance(world).removeSource((ILightSource) entity);
		super.breakBlock(world, pos, state);
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(World world, IBlockState iBlockState) {
		return new TileMagnifier();
	}
}
