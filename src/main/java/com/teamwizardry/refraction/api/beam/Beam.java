package com.teamwizardry.refraction.api.beam;

import com.teamwizardry.librarianlib.common.network.PacketHandler;
import com.teamwizardry.refraction.api.ConfigValues;
import com.teamwizardry.refraction.api.Utils;
import com.teamwizardry.refraction.api.beam.Effect.EffectType;
import com.teamwizardry.refraction.api.beam.modes.BeamMode;
import com.teamwizardry.refraction.api.beam.modes.BeamModeRegistry;
import com.teamwizardry.refraction.api.beam.modes.ModeEffect;
import com.teamwizardry.refraction.api.raytrace.EntityTrace;
import com.teamwizardry.refraction.common.network.PacketBeamParticle1;
import com.teamwizardry.refraction.common.network.PacketBeamParticle2;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Will create a new beam you can spawn.
 * <p>
 * IMPORTANT: DO NOT CREATE A BEAM OBJECT AND SPAWN IT AFTER CHECKING world.isRemote.
 * You WILL break the particles. Leave the world.isRemote checks to this thing.
 */
public class Beam implements INBTSerializable<NBTTagCompound> {

    /**
     * The mode of the beam
     */
    @NotNull
    public BeamMode mode = BeamModeRegistry.NONE;

    /**
     * The initial position the beams comes from.
     */
    public Vec3d initLoc;

    /**
     * The vector that specifies the inclination of the beam.
     * Set it to your final location and it'll work.
     */
    public Vec3d slope;

    /**
     * The destination of the beam. Don't touch this, just set the slope to the final loc
     * and let this class handleBeam it unless you know what you're doing.
     */
    public Vec3d finalLoc;

    /**
     * The color of the beam including it's alpha.
     */
    @NotNull
    public Color color = Color.WHITE;

    /**
     * The world the beam will spawn in.
     */
    @NotNull
    public World world;

    /**
     * The effect the beam will produce across itself or at it's destination
     */
    @Nullable
    public Effect effect;

    /**
     * If true, the beam will phase through entities.
     */
    public boolean ignoreEntities = false;

    /**
     * The raytrace produced from the beam after it spawns.
     * Contains some neat methods you can use.
     */
    public RayTraceResult trace;

    /**
     * The range of the raytrace. Will default to Beam_RANGE unless otherwise specified.
     */
    public double range = ConfigValues.BEAM_RANGE;

    /**
     * The number of times this beam has bounced or been reflected.
     */
    public int bouncedTimes = 0;

    /**
     * The amount of times this beam is allowed to bounce or reflect.
     */
    public int allowedBounceTimes = ConfigValues.BEAM_BOUNCE_LIMIT;

    /**
     * Will spawn a particle at either the beginning or at the end of the beam if any are enabled.
     */
    public boolean enableParticleBeginning = false, enableParticleEnd;

    /**
     * The uuid of the entity that will not be affected by the beam.
     */
    @Nullable
    public UUID uuidToSkip;

    /**
     * The person theoretically casting the beam.
     */
    @Nullable
    public Entity caster;

    /**
     * The custom name of the beam
     */
    public String customName = "";

    public Beam(@NotNull World world, @NotNull Vec3d initLoc, @NotNull Vec3d slope, @NotNull Color color) {
        this.world = world;
        this.initLoc = initLoc;
        this.slope = slope;
        this.finalLoc = slope.normalize().scale(128).add(initLoc);
        this.color = color;
    }

    public Beam(World world, double initX, double initY, double initZ, double slopeX, double slopeY, double slopeZ, Color color) {
        this(world, new Vec3d(initX, initY, initZ), new Vec3d(slopeX, slopeY, slopeZ), color);
    }

    public Beam(World world, double initX, double initY, double initZ, double slopeX, double slopeY, double slopeZ, float red, float green, float blue, float alpha) {
        this(world, initX, initY, initZ, slopeX, slopeY, slopeZ, new Color(red, green, blue, alpha));
    }

    public Beam(NBTTagCompound compound) {
        deserializeNBT(compound);
    }

    public boolean doBeamsMatch(Beam beam) {
        return beam.color.getRGB() == color.getRGB()
                && beam.slope.xCoord == slope.xCoord
                && beam.slope.yCoord == slope.yCoord
                && beam.slope.zCoord == slope.zCoord
                && beam.initLoc.xCoord == initLoc.xCoord
                && beam.initLoc.yCoord == initLoc.yCoord
                && beam.initLoc.zCoord == initLoc.zCoord
                && beam.enableParticleEnd == enableParticleEnd
                && beam.enableParticleBeginning == enableParticleBeginning
                && beam.ignoreEntities == ignoreEntities
                && beam.allowedBounceTimes == allowedBounceTimes
                && beam.bouncedTimes == bouncedTimes
                && beam.range == range
                && beam.mode.equals(mode);
    }

    /**
     * Will create a beam that's exactly like the one passed.
     *
     * @return The new beam created. Can be modified as needed.
     */
    public Beam createSimilarBeam() {
        return createSimilarBeam(initLoc, finalLoc);
    }

    /**
     * Will create a beam that's exactly like the one passed except in color.
     *
     * @return The new beam created. Can be modified as needed.
     */
    public Beam createSimilarBeam(Color color) {
        return createSimilarBeam(initLoc, finalLoc, color);
    }

    /**
     * Will create a similar beam that starts from the position this beam ended at
     * and will set it's slope to the one specified. So it's a new beam from the position
     * you last hit to the new one you specify.
     *
     * @param slope The slope or destination or final location the beam will point to.
     * @return The new beam created. Can be modified as needed.
     */
    public Beam createSimilarBeam(Vec3d slope) {
        return createSimilarBeam(finalLoc, slope);
    }


    /**
     * Will create a similar beam that starts and ends in the positions you specify
     *
     * @param init The initial location or origin to spawn the beam from.
     * @param dir  The direction or slope or final destination or location the beam will point to.
     * @return The new beam created. Can be modified as needed.
     */
    public Beam createSimilarBeam(Vec3d init, Vec3d dir) {
        return createSimilarBeam(init, dir, color);
    }

    /**
     * Will create a similar beam that starts and ends in the positions you specify, with a custom color.
     *
     * @param init The initial location or origin to spawn the beam from.
     * @param dir  The direction or slope or final destination or location the beam will point to.
     * @return The new beam created. Can be modified as needed.
     */
    public Beam createSimilarBeam(Vec3d init, Vec3d dir, Color color) {
        return new Beam(world, init, dir, color)
                .setIgnoreEntities(ignoreEntities)
                .setAllowedBounceTimes(allowedBounceTimes)
                .setBouncedTimes(bouncedTimes)
                .incrementBouncedTimes()
                .setMode(mode)
                .setRange(range)
                .setCaster(caster);
    }

    /**
     * Will change the mode of the beam
     *
     * @param mode Defines the new mode this beam will be.
     * @return The new beam created. Can be modified as needed.
     */
    public Beam setMode(@NotNull BeamMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Will change the name of the beam.
     *
     * @param name Defines the custom name of the beam.
     * @return The new beam created. Can be modified as needed.
     */
    public Beam setName(@NotNull String name) {
        this.customName = name;
        return this;
    }

    /**
     * Will set the theoretical caster of the beam.
     *
     * @param caster Defines the entity casting the beam.
     * @return The new beam created. Can be modified as needed.
     */
    public Beam setCaster(@Nullable Entity caster) {
        this.caster = caster;
        return this;
    }

    /**
     * The RayTrace will skip the first time it hits an entity with this uuid
     *
     * @param uuidToSkip The uuid to skip the first time it's detected
     * @return The new beam created. Can be modified as needed.
     */
    public Beam setUUIDToSkip(UUID uuidToSkip) {
        this.uuidToSkip = uuidToSkip;
        return this;
    }

    /**
     * Will create a tiny particle at the initLoc of the beam
     *
     * @return The new beam created. Can be modified as needed.
     */
    public Beam enableParticleBeginning() {
        this.enableParticleBeginning = true;
        return this;
    }

    /**
     * Will create a tiny particle at the end of the beam
     *
     * @return The new beam created. Can be modified as needed.
     */
    public Beam enableParticleEnd() {
        this.enableParticleEnd = true;
        return this;
    }

    /**
     * Will set the amount of times this beam has already bounced or been reflected
     *
     * @param bouncedTimes The amount of times this beam has bounced or been reflected
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    public Beam setBouncedTimes(int bouncedTimes) {
        this.bouncedTimes = bouncedTimes;
        return this;
    }

    /**
     * Will set the amount of times this beam will be allowed to bounce or reflect.
     *
     * @param allowedBounceTimes The amount of times this beam is allowed to bounce or reflect
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    public Beam setAllowedBounceTimes(int allowedBounceTimes) {
        this.allowedBounceTimes = allowedBounceTimes;
        return this;
    }

    /**
     * Will change the slope or destination or final location the beam will point to.
     *
     * @param slope The final location or destination.
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    public Beam setSlope(@NotNull Vec3d slope) {
        this.slope = slope;
        this.finalLoc = slope.normalize().scale(128).add(initLoc);
        return this;
    }

    /**
     * Will increment the amount of times this beam has bounced or reflected
     *
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    public Beam incrementBouncedTimes() {
        bouncedTimes++;
        return this;
    }

    /**
     * Will change the color of the beam with the alpha.
     *
     * @param color The color of the new beam.
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    public Beam setColor(@NotNull Color color) {
        this.color = color;
        return this;
    }

    /**
     * If set to true, the beam will phase through entities.
     *
     * @param ignoreEntities The boolean that will specify if the beam should phase through gravityProtection or not. Default false.
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    public Beam setIgnoreEntities(boolean ignoreEntities) {
        this.ignoreEntities = ignoreEntities;
        return this;
    }

    /**
     * Will set the beam's new starting position or origin and will continue on towards the slope still specified.
     *
     * @param initLoc The new initial location to set the beam to exciterPos from.
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    public Beam setInitLoc(@NotNull Vec3d initLoc) {
        this.initLoc = initLoc;
        this.finalLoc = slope.normalize().scale(128).add(initLoc);
        return this;
    }

    /**
     * Will set the beam's effect if you don't want it to autodetect the effect by itself from the color
     * you specified.
     *
     * @param effect The new effect this beam will produce.
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    public Beam setEffect(@Nullable Effect effect) {
        this.effect = effect;
        return this;
    }

    /**
     * Will set the range the raytrace will attempt.
     *
     * @param range The new range of the beam. Default: Constants.BEAM_RANGE
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    public Beam setRange(double range) {
        this.range = range;
        return this;
    }

    /**
     * Will initialize all variables left to prepare before the beam actually spawns.
     *
     * @return This beam itself for the convenience of editing a beam in one line/chain.
     */
    private Beam initializeVariables() {
        // EFFECT CHECKING //
        if (effect == null && mode instanceof ModeEffect) {
            Effect tempEffect = EffectTracker.getEffect(this);
            if (tempEffect != null) effect = tempEffect;
        } else if (effect != null && !(mode instanceof ModeEffect)) effect = null;
        // EFFECT CHECKING //

        // BEAM PHASING CHECKS //
        EntityTrace entityTrace = new EntityTrace(world, initLoc, slope).setUUIDToSkip(uuidToSkip).setRange(range);
        if (ignoreEntities || (effect != null && effect.getType() == EffectType.BEAM)) // If anyone of these are true, phase beam
            trace = entityTrace.setIgnoreEntities(true).cast();
        else trace = entityTrace.setIgnoreEntities(false).cast();
        // BEAM PHASING CHECKS //

        if (trace != null && trace.hitVec != null) this.finalLoc = trace.hitVec;
        return this;
    }

    /**
     * Will spawn the final complete beam.
     */
    public void spawn() {
        if (world.isRemote) return;
        if (color.getAlpha() <= 1) return;
        if (bouncedTimes > allowedBounceTimes) return;

        initializeVariables();

        if (trace == null) return;
        if (trace.hitVec == null) return;
        if (finalLoc == null) return;

        // EFFECT HANDLING //
        boolean pass = true;

        boolean traceCompleted = false;

        // Making sure we don't recur //
        int tries = 0;

        // IBeamHandler handling
        while (!traceCompleted && tries < 100) {
            tries++;
            if (trace == null)
                return;
            else if (trace.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos pos = trace.getBlockPos();
                IBlockState state = world.getBlockState(pos);
                BeamHitEvent event = new BeamHitEvent(world, this, pos, state);
                MinecraftForge.EVENT_BUS.post(event);
                if (event.getResult() == Event.Result.DEFAULT) {
                    traceCompleted = true;
                    if (state.getBlock() instanceof IBeamHandler) {
                        traceCompleted = (((IBeamHandler) state.getBlock()).handleBeam(world, pos, this));
                        pass = false;
                    }
                } else {
                    traceCompleted = event.getResult() == Event.Result.DENY;
                    pass = event.getResult() == Event.Result.ALLOW;
                }
            } else {
                traceCompleted = trace.typeOfHit != RayTraceResult.Type.ENTITY ||
                        !MinecraftForge.EVENT_BUS.post(new BeamHitEntityEvent(world, this, trace.entityHit));
            }
            if (!traceCompleted) traceCompleted = recast();
        }

        // Effect handling
        if (mode instanceof ModeEffect && effect != null) {
            if (effect.getType() == EffectType.BEAM)
                EffectTracker.addEffect(world, this);

            else if (pass) {
                if (effect.getType() == EffectType.SINGLE) {
                    if (trace.typeOfHit != RayTraceResult.Type.MISS) {
                        EffectTracker.addEffect(world, trace.hitVec, effect);

                    } else if (trace.typeOfHit == RayTraceResult.Type.BLOCK) {
                        BlockPos pos = trace.getBlockPos();
                        EffectTracker.addEffect(world, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), effect);
                    }
                }
            }
        }
        // EFFECT HANDLING

        // ENTITY REFLECTING
        if (trace.typeOfHit == RayTraceResult.Type.ENTITY && trace.entityHit instanceof EntityLivingBase) {
            EntityLivingBase entity = (EntityLivingBase) trace.entityHit;
            boolean flag = true;
            for (ItemStack armor : entity.getArmorInventoryList()) {
                if (armor == null) {
                    flag = false;
                    break;
                }
                if (!(armor.getItem() instanceof IReflectiveArmor)) {
                    flag = false;
                    break;
                }
            }
            if (flag)
                createSimilarBeam(entity.getLook(1)).setUUIDToSkip(entity.getUniqueID()).enableParticleBeginning().spawn();
        }
        // ENTITY REFLECTING

        // Particle packet sender
        Utils.HANDLER.fireLaserPacket(this);

        // PARTICLES
        if (enableParticleBeginning) {
            if (ThreadLocalRandom.current().nextInt(10) == 0)
                PacketHandler.NETWORK.sendToAllAround(new PacketBeamParticle1(initLoc, color), new NetworkRegistry.TargetPoint(world.provider.getDimension(), initLoc.xCoord, initLoc.yCoord, initLoc.zCoord, 30));
            if (ThreadLocalRandom.current().nextInt(100) == 0)
                PacketHandler.NETWORK.sendToAllAround(new PacketBeamParticle2(initLoc, color), new NetworkRegistry.TargetPoint(world.provider.getDimension(), initLoc.xCoord, initLoc.yCoord, initLoc.zCoord, 30));
        }
        if (trace.hitVec != null && enableParticleEnd) {
            if (ThreadLocalRandom.current().nextInt(10) == 0)
                PacketHandler.NETWORK.sendToAllAround(new PacketBeamParticle1(finalLoc, color), new NetworkRegistry.TargetPoint(world.provider.getDimension(), finalLoc.xCoord, finalLoc.yCoord, finalLoc.zCoord, 30));
            if (ThreadLocalRandom.current().nextInt(100) == 0)
                PacketHandler.NETWORK.sendToAllAround(new PacketBeamParticle2(finalLoc, color), new NetworkRegistry.TargetPoint(world.provider.getDimension(), finalLoc.xCoord, finalLoc.yCoord, finalLoc.zCoord, 30));
        }
        // PARTICLES
    }

    private boolean recast() {
        EntityTrace entityTrace = new EntityTrace(world, this).setUUIDToSkip(uuidToSkip);
        if (entityTrace.range <= 0) return true;

        if (entityTrace.rayTraceResult != null)
            trace = entityTrace.rayTraceResult;
        else if (ignoreEntities || (effect != null && effect.getType() == EffectType.BEAM)) // If anyone of these are true, phase beam
            trace = entityTrace.setIgnoreEntities(true).cast();
        else trace = entityTrace.setIgnoreEntities(false).cast();

        if (trace != null && trace.hitVec != null) this.finalLoc = trace.hitVec;

        return false;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setDouble("init_loc_x", initLoc.xCoord);
        compound.setDouble("init_loc_y", initLoc.yCoord);
        compound.setDouble("init_loc_z", initLoc.zCoord);
        compound.setDouble("slope_x", slope.xCoord);
        compound.setDouble("slope_y", slope.yCoord);
        compound.setDouble("slope_z", slope.zCoord);
        compound.setInteger("color", color.getRGB());
        compound.setInteger("world", world.provider.getDimension());
        compound.setInteger("bounce_times", bouncedTimes);
        compound.setInteger("allowed_bounce_times", allowedBounceTimes);
        compound.setDouble("range", range);
        compound.setString("name", customName);
        compound.setBoolean("ignore_entities", ignoreEntities);
        compound.setBoolean("enable_particle_beginning", enableParticleBeginning);
        compound.setBoolean("enable_particle_end", enableParticleEnd);
        compound.setString("mode", mode.getName());
        if (uuidToSkip != null) compound.setUniqueId("uuid_to_skip", uuidToSkip);
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("world")) {
            world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(nbt.getInteger("dim"));
        } else throw new NullPointerException("'world' key not found or missing in deserialized beam object.");
        if (nbt.hasKey("init_loc_x") && nbt.hasKey("init_loc_y") && nbt.hasKey("init_loc_z")) {
            initLoc = new Vec3d(nbt.getDouble("init_loc_x"), nbt.getDouble("init_loc_y"), nbt.getDouble("init_loc_z"));
        } else throw new NullPointerException("'init_loc' key not found or missing in deserialized beam object.");
        if (nbt.hasKey("slope_x") && nbt.hasKey("slope_y") && nbt.hasKey("slope_z")) {
            slope = new Vec3d(nbt.getDouble("slope_x"), nbt.getDouble("slope_y"), nbt.getDouble("slope_z"));
            finalLoc = slope.normalize().scale(128).add(initLoc);
        } else throw new NullPointerException("'slope' key not found or missing in deserialized beam object.");

        if (nbt.hasKey("color")) {
            color = new Color(nbt.getInteger("color"), true);
        } else
            throw new NullPointerException("'color' or 'color_alpha' keys not found or missing in deserialized beam object.");

        if (nbt.hasKey("name")) customName = nbt.getString("name");
        if (nbt.hasKey("uuid_to_skip")) uuidToSkip = nbt.getUniqueId("uuid_to_skip");
        if (nbt.hasKey("ignore_entities")) ignoreEntities = nbt.getBoolean("ignore_entities");
        if (nbt.hasKey("range")) range = nbt.getDouble("range");
        if (nbt.hasKey("bounce_times")) bouncedTimes = nbt.getInteger("bounce_times");
        if (nbt.hasKey("allowed_bounce_times")) allowedBounceTimes = nbt.getInteger("allowed_bounce_times");
        if (nbt.hasKey("enable_particle_beginning"))
            enableParticleBeginning = nbt.getBoolean("enable_particle_beginning");
        mode = BeamModeRegistry.getMode(nbt.getString("mode"));
        if (nbt.hasKey("enable_particle_end")) enableParticleEnd = nbt.getBoolean("enable_particle_end");
    }
}
