package com.teamwizardry.refraction.common.tile;

import com.google.common.collect.Lists;
import com.teamwizardry.librarianlib.common.base.block.TileMod;
import com.teamwizardry.librarianlib.common.util.autoregister.TileRegister;
import com.teamwizardry.librarianlib.common.util.saving.Save;
import com.teamwizardry.refraction.api.Utils;
import com.teamwizardry.refraction.api.beam.Beam;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Saad on 9/11/2016.
 */
@TileRegister("spectrometer")
public class TileSpectrometer extends TileMod implements ITickable {

    @Save
    public Color maxColor = new Color(0, 0, 0, 0);
    public Color currentColor = new Color(0, 0, 0, 0);
    @Save
    public boolean beamUpdate = false;
    @NotNull
    private List<Beam> beams = new ArrayList<>();

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        this.currentColor = new Color(compound.getInteger("current_color"), true);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound, boolean sync) {
        compound.setInteger("current_color", currentColor.getRGB());
    }

    public void handle(Beam... beams) {
        if (this.beams.isEmpty()) {
            this.beams.addAll(Lists.newArrayList(beams));
            return;
        }
        ArrayList<Beam> tempBeams = new ArrayList<>();
        tempBeams.addAll(this.beams);
        for (Beam beam1 : beams) {
            beamUpdate = true;
            for (Beam beam2 : tempBeams)
                if (beam1.doBeamsMatch(beam2)) return;
            this.beams.add(beam1);
        }
        markDirty();
    }

    @Override
    public void update() {
        if (worldObj.isRemote) return;

        if (currentColor.getRGB() != maxColor.getRGB()) {
            currentColor = Utils.mixColors(currentColor, maxColor, 0.9);
            worldObj.notifyBlockUpdate(pos, worldObj.getBlockState(pos), worldObj.getBlockState(pos), 3);
        }

        if (beams.isEmpty()) {
            maxColor = new Color(0, 0, 0, 0);
            worldObj.notifyBlockUpdate(pos, worldObj.getBlockState(pos), worldObj.getBlockState(pos), 3);
            markDirty();
            return;
        }

        if (!beamUpdate) return;
        beamUpdate = false;

        int red = 0;
        int green = 0;
        int blue = 0;
        int alpha = 0;

        for (Beam beam : beams) {
            Color color = beam.color;
            red += color.getRed() * (color.getAlpha() / 255f);
            green += color.getGreen() * (color.getAlpha() / 255f);
            blue += color.getBlue() * (color.getAlpha() / 255f);
            alpha += color.getAlpha();
        }
        red = Math.min(red / beams.size(), 255);
        green = Math.min(green / beams.size(), 255);
        blue = Math.min(blue / beams.size(), 255);

        float[] hsbvals = Color.RGBtoHSB(red, green, blue, null);
        Color color = new Color(Color.HSBtoRGB(hsbvals[0], hsbvals[1], 1));
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(alpha, 255));

        if (color.getRGB() == maxColor.getRGB()) return;
        this.maxColor = color;

        beams.clear();
        markDirty();
    }
}
