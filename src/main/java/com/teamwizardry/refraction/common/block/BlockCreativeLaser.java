package com.teamwizardry.refraction.common.block;

import com.teamwizardry.librarianlib.client.util.TooltipHelper;
import com.teamwizardry.librarianlib.common.base.block.BlockMod;
import com.teamwizardry.refraction.api.ILightSource;
import com.teamwizardry.refraction.api.PosUtils;
import com.teamwizardry.refraction.common.light.Beam;
import com.teamwizardry.refraction.common.light.ReflectionTracker;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.Random;

/**
 * Created by Saad on 10/6/2016.
 */
public class BlockCreativeLaser  extends BlockMod implements ILightSource {

	public static final PropertyEnum<EnumFacing> FACING = PropertyEnum.create("facing", EnumFacing.class);

	public BlockCreativeLaser() {
		super("creative_laser", Material.IRON);
		setHardness(1F);
		setSoundType(SoundType.METAL);

		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		ReflectionTracker.getInstance(worldIn).addSource(pos, this);
		worldIn.scheduleUpdate(pos, this, 20);
	}

	@Override
	public void updateTick(@NotNull World worldIn, @NotNull BlockPos pos, @NotNull IBlockState state, @NotNull Random random) {
		ReflectionTracker.getInstance(worldIn).addSource(pos, this);
		worldIn.scheduleUpdate(pos, this, 20);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
		TooltipHelper.addToTooltip(tooltip, "simple_name.refraction:" + getRegistryName().getResourcePath());
	}

	@Override
	public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		if (placer.rotationPitch > 45) return this.getStateFromMeta(meta).withProperty(FACING, EnumFacing.UP);
		if (placer.rotationPitch < -45) return this.getStateFromMeta(meta).withProperty(FACING, EnumFacing.DOWN);

		return this.getStateFromMeta(meta).withProperty(FACING, placer.getAdjustedHorizontalFacing().getOpposite());
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.getFront(meta & 7));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getIndex();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public boolean canRenderInLayer(BlockRenderLayer layer) {
		return layer == BlockRenderLayer.CUTOUT;
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
	public void generateBeam(@NotNull World world, @Nonnull BlockPos pos) {
		Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		EnumFacing face = world.getBlockState(pos).getValue(BlockDirectional.FACING);
		Vec3d vec = PosUtils.getVecFromFacing(face);
		new Beam(world, center, vec, Color.WHITE).spawn();
	}
}
