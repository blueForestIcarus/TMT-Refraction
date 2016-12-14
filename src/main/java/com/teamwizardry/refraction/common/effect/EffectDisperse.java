package com.teamwizardry.refraction.common.effect;

import com.teamwizardry.refraction.api.beam.Effect;
import com.teamwizardry.refraction.api.beam.EffectTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.awt.*;

/**
 * Created by LordSaad44
 * Will disperse any entities that intersect with the beam. < 128 only disperses item entities.
 */
public class EffectDisperse extends Effect {

    @Override
    public EffectType getType() {
        return EffectType.BEAM;
    }

    private void setEntityMotion(Entity entity, double potency) {
        Vec3d pullDir;
        if (beam.finalLoc == null) return;
        pullDir = beam.finalLoc.subtract(beam.initLoc).normalize();

        entity.setNoGravity(true);
        entity.motionX = pullDir.xCoord * potency / 255.0;
        entity.motionY = pullDir.yCoord * potency / 255.0;
        entity.motionZ = pullDir.zCoord * potency / 255.0;
        entity.fallDistance = 0;
    }

    @Override
    public void runEntity(World world, Entity entity, int potency) {
        setEntityMotion(entity, potency);
        EffectTracker.gravityReset.put(entity, 30);
        if (entity instanceof EntityPlayer)
            ((EntityPlayer) entity).velocityChanged = true;
        if (entity instanceof EntityItem) {
            for (BlockPos pos : BlockPos.getAllInBoxMutable(entity.getPosition().add(-1, -1, -1), entity.getPosition().add(1, 1, 1))) {
                TileEntity tileEntity = world.getTileEntity(pos);

                if (tileEntity == null)
                    continue;

                if (!tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, beam.trace.sideHit))
                    continue;

                IItemHandler handler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, beam.trace.sideHit);

                ItemStack newStack = ItemHandlerHelper.insertItem(handler, ((EntityItem) entity).getEntityItem(), false);
                if (newStack == null || newStack.stackSize == 0) entity.setDead();
                ((EntityItem) entity).setEntityItemStack(newStack);
            }
        }
    }

    @Override
    public Color getColor() {
        return Color.MAGENTA;
    }
}
