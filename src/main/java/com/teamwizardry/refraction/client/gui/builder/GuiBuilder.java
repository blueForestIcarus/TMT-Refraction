package com.teamwizardry.refraction.client.gui.builder;

import com.teamwizardry.librarianlib.client.core.ClientTickHandler;
import com.teamwizardry.librarianlib.client.gui.EnumMouseButton;
import com.teamwizardry.librarianlib.client.gui.GuiBase;
import com.teamwizardry.librarianlib.client.gui.GuiComponent;
import com.teamwizardry.librarianlib.client.gui.components.ComponentList;
import com.teamwizardry.librarianlib.client.gui.components.ComponentSprite;
import com.teamwizardry.librarianlib.client.gui.mixin.ButtonMixin;
import com.teamwizardry.librarianlib.client.sprite.Sprite;
import com.teamwizardry.librarianlib.client.sprite.Texture;
import com.teamwizardry.librarianlib.common.util.math.Vec2d;
import com.teamwizardry.refraction.api.Constants;
import com.teamwizardry.refraction.client.gui.LeftSidebar;
import com.teamwizardry.refraction.client.gui.RightSidebar;
import com.teamwizardry.refraction.client.gui.builder.regionoptions.OptionFill;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Created by LordSaad.
 */
public class GuiBuilder extends GuiBase {

	private static final Texture texScreen = new Texture(new ResourceLocation(Constants.MOD_ID, "textures/gui/builder/screen.png"));
	private static final Texture texBorder = new Texture(new ResourceLocation(Constants.MOD_ID, "textures/gui/builder/border.png"));
	private static final Texture texSpriteSheet = new Texture(new ResourceLocation(Constants.MOD_ID, "textures/gui/builder/builder_sheet.png"));
	public static final Sprite sprArrowUp = texSpriteSheet.getSprite("arrow_up", 16, 16);
	public static final Sprite sprArrowDown = texSpriteSheet.getSprite("arrow_down", 16, 16);
	public static final Sprite sprLayers = texSpriteSheet.getSprite("layers", 16, 16);
	private static final Sprite sprScreen = texScreen.getSprite("bg", 256, 256);
	private static final Sprite sprBorder = texBorder.getSprite("bg", 276, 276);
	private static final Sprite sprTileRightSelected = texSpriteSheet.getSprite("tile_right_selected", 16, 16);
	private static final Sprite sprTileLeftSelected = texSpriteSheet.getSprite("tile_left_selected", 16, 16);
	private static final Sprite sprTileNormal = texSpriteSheet.getSprite("tile_normal", 16, 16);
	private static final Sprite sprIconDirect = texSpriteSheet.getSprite("icon_direct", 16, 16);
	private static final Sprite sprIconRegionSelection = texSpriteSheet.getSprite("icon_region_selection", 16, 16);
	private static final Sprite sprTabMode = texSpriteSheet.getSprite("tab_mode", 16, 16);
	public TileType[][][] grid = new TileType[16][16][16];
	public int selectedLayer = 0;
	public Mode selectedMode = Mode.DIRECT;

	public GuiBuilder() {
		super(sprBorder.getWidth() + LeftSidebar.leftExtended.getWidth() * 2, sprBorder.getHeight());

		for (int i = 0; i < grid.length; i++)
			for (int j = 0; j < grid.length; j++)
				for (int k = 0; k < grid.length; k++)
					grid[i][j][k] = TileType.EMPTY;

		// LEFT //
		ComponentList leftSidebar = new ComponentList(LeftSidebar.leftExtended.getWidth(), 0);

		LeftSidebar modeComp = new LeftSidebar(leftSidebar, "Selection Modes", sprTabMode, true, true);
		modeComp.listComp.setMarginLeft(5);
		modeComp.listComp.add(new ModeSelector(modeComp.listComp, this, Mode.DIRECT, "Set Tiles Directly", sprIconDirect, true).component);

		ComponentList selectRegionOptions = new ComponentList(0, 0);
		selectRegionOptions.add(new OptionFill(this, selectRegionOptions, "Fill", sprIconDirect, TileType.PLACED).component);
		selectRegionOptions.add(new OptionFill(this, selectRegionOptions, "Clear", sprIconDirect, TileType.EMPTY).component);

		ModeSelector selectRegionComp = new ModeSelector(modeComp.listComp, this, Mode.SELECT, "Select Regions", sprIconRegionSelection, false);
		selectRegionComp.listComp.add(selectRegionOptions);
		modeComp.listComp.add(selectRegionComp.component);
		leftSidebar.add(modeComp.component);

		getMainComponents().add(leftSidebar);
		// LEFT //

		// RIGHT //
		ComponentList rightSidebar = new ComponentList(sprBorder.getWidth() * 2 - 11, 0);

		RightSidebar layers = new RightSidebar(rightSidebar, "Layers", sprLayers, false, true);
		layers.listComp.setMarginLeft(-5);

		layers.component.BUS.hook(GuiComponent.ComponentTickEvent.class, componentTickEvent -> {
			layers.title = "Layers - Current: " + selectedLayer;
		});

		LayerSelector layerUp = new LayerSelector(this, rightSidebar, "Layer Up", sprArrowUp, true);
		LayerSelector layerDown = new LayerSelector(this, rightSidebar, "Layer Down", sprArrowDown, false);
		layers.listComp.add(layerUp.component, layerDown.component);

		rightSidebar.add(layers.component);
		getMainComponents().add(rightSidebar);
		// RIGHT //

		// BORDER //
		ComponentSprite compBorder = new ComponentSprite(sprBorder,
				(getGuiWidth() / 2) - (sprBorder.getWidth() / 2),
				(getGuiHeight() / 2) - (sprBorder.getHeight() / 2));
		getMainComponents().add(compBorder);
		// BORDER //

		// SCREEN //
		ComponentSprite compScreen = new ComponentSprite(sprScreen,
				(getGuiWidth() / 2) - (sprScreen.getWidth() / 2),
				(getGuiHeight() / 2) - (sprScreen.getHeight() / 2));

		new ButtonMixin<>(compScreen, () -> {
		});

		compScreen.BUS.hook(GuiComponent.MouseDragEvent.class, (event) -> {
			Vec2d pos = event.getMousePos();
			int x = pos.getXi() / 16;
			int y = pos.getYi() / 16;
			if (x < grid.length && y < grid.length && x > 0 && y > 0)
				if (selectedMode == Mode.DIRECT) {
					if (event.getButton() == EnumMouseButton.LEFT)
						grid[selectedLayer][x][y] = TileType.PLACED;
					else if (event.getButton() == EnumMouseButton.RIGHT)
						grid[selectedLayer][x][y] = TileType.EMPTY;
				}
		});

		compScreen.BUS.hook(GuiComponent.MouseDownEvent.class, (event) -> {
			Vec2d pos = event.getMousePos();
			int x = pos.getXi() / 16;
			int y = pos.getYi() / 16;
			if (x < grid.length && y < grid.length && x > 0 && y > 0)
				if (selectedMode == Mode.DIRECT)
					if (grid[selectedLayer][x][y] == TileType.EMPTY)
						grid[selectedLayer][x][y] = TileType.PLACED;
					else grid[selectedLayer][x][y] = TileType.EMPTY;
				else if (selectedMode == Mode.SELECT) {
					if (grid[selectedLayer][x][y] == TileType.EMPTY)
						if (event.getButton() == EnumMouseButton.LEFT) {
							Vec2d left = getTile(TileType.LEFT_SELECTED);
							if (left != null) grid[selectedLayer][left.getXi()][left.getYi()] = TileType.EMPTY;
							grid[selectedLayer][x][y] = TileType.LEFT_SELECTED;
						} else {
							Vec2d left = getTile(TileType.RIGHT_SELECTED);
							if (left != null) grid[selectedLayer][left.getXi()][left.getYi()] = TileType.EMPTY;
							grid[selectedLayer][x][y] = TileType.RIGHT_SELECTED;
						}
					else grid[selectedLayer][x][y] = TileType.EMPTY;
				}
		});

		compScreen.BUS.hook(GuiComponent.PostDrawEvent.class, (event) -> {
			for (int i = 0; i < grid.length; i++)
				for (int j = 0; j < grid.length; j++) {
					TileType box = grid[selectedLayer][i][j];

					GlStateManager.pushMatrix();
					GlStateManager.enableAlpha();
					GlStateManager.enableBlend();
					GlStateManager.enableTexture2D();
					GlStateManager.disableLighting();

					texSpriteSheet.bind();
					if (box == TileType.PLACED) {
						sprTileNormal.draw((int) ClientTickHandler.getPartialTicks(),
								event.getComponent().getPos().getXi() + i * 16,
								event.getComponent().getPos().getYi() + j * 16);
					} else if (box == TileType.LEFT_SELECTED) {
						sprTileLeftSelected.draw((int) ClientTickHandler.getPartialTicks(),
								event.getComponent().getPos().getXi() + i * 16,
								event.getComponent().getPos().getYi() + j * 16);
					} else if (box == TileType.RIGHT_SELECTED) {
						sprTileRightSelected.draw((int) ClientTickHandler.getPartialTicks(),
								event.getComponent().getPos().getXi() + i * 16,
								event.getComponent().getPos().getYi() + j * 16);
					}

					GlStateManager.enableLighting();
					GlStateManager.disableTexture2D();
					GlStateManager.popMatrix();
				}

			for (int i = 0; i < grid.length; i++)
				for (int j = 0; j < grid.length; j++) {
					if (selectedLayer - 1 >= 0) {
						TileType box = grid[selectedLayer - 1][i][j];

						GlStateManager.pushMatrix();
						GlStateManager.enableAlpha();
						GlStateManager.enableBlend();
						GlStateManager.enableTexture2D();
						GlStateManager.disableLighting();

						GlStateManager.color(1, 1, 1, 0.3f);

						texSpriteSheet.bind();
						if (box == TileType.PLACED) {
							sprTileNormal.draw((int) ClientTickHandler.getPartialTicks(),
									event.getComponent().getPos().getXi() + i * 16,
									event.getComponent().getPos().getYi() + j * 16);
						} else if (box == TileType.LEFT_SELECTED) {
							sprTileLeftSelected.draw((int) ClientTickHandler.getPartialTicks(),
									event.getComponent().getPos().getXi() + i * 16,
									event.getComponent().getPos().getYi() + j * 16);
						} else if (box == TileType.RIGHT_SELECTED) {
							sprTileRightSelected.draw((int) ClientTickHandler.getPartialTicks(),
									event.getComponent().getPos().getXi() + i * 16,
									event.getComponent().getPos().getYi() + j * 16);
						}

						GlStateManager.enableLighting();
						GlStateManager.disableTexture2D();
						GlStateManager.popMatrix();
					}
				}
		});
		getMainComponents().add(compScreen);
		// SCREEN //
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	public boolean hasTile(TileType type) {
		for (TileType[] x : grid[selectedLayer])
			for (TileType tileType : x)
				if (tileType == type) return true;
		return false;
	}

	@Nullable
	public Vec2d getTile(TileType type) {
		for (int i = 0; i < grid.length; i++)
			for (int j = 0; j < grid.length; j++)
				if (grid[selectedLayer][i][j] == type) return new Vec2d(i, j);
		return null;
	}

	public enum TileType {
		EMPTY, LEFT_SELECTED, RIGHT_SELECTED, PLACED
	}

	public enum Mode {
		DIRECT, SELECT
	}
}
