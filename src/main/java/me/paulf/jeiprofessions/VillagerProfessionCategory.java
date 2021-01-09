package me.paulf.jeiprofessions;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class VillagerProfessionCategory implements IRecipeCategory<ProfessionEntry> {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ResourceLocation UID = new ResourceLocation("villager_professions");

    private static final ResourceLocation GUI = new ResourceLocation(JeiProfessions.ID, "textures/gui/profession.png");

    private final IDrawable background;

    private final IDrawable icon;

    public VillagerProfessionCategory(final IGuiHelper helper) {
        this.background = helper.createDrawable(GUI, 0, 0, 84, 51);
        this.icon = helper.createDrawableIngredient(new ItemStack(Items.BELL));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends ProfessionEntry> getRecipeClass() {
        return ProfessionEntry.class;
    }

    @Override
    public String getTitle() {
        return I18n.format("gui.jeiprofessions.category.villager_professions");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setIngredients(final ProfessionEntry profession, final IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, profession.getInputs());
    }

    @Override
    public void setRecipe(final IRecipeLayout layout, final ProfessionEntry profession, final IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 0, 22);
        layout.getItemStacks().set(ingredients);
    }

    private final LoadingCache<ProfessionEntry, CachedVillager> cache = CacheBuilder.newBuilder()
        .maximumSize(128L)
        .expireAfterAccess(2L, TimeUnit.MINUTES)
        .removalListener((RemovalListener<ProfessionEntry, CachedVillager>) notification -> notification.getValue().remove())
        .build(CacheLoader.from(p -> {
            try {
                if (p == null) throw new NullPointerException("profession");
                final Minecraft minecraft = Minecraft.getInstance();
                final ClientWorld world = minecraft.world;
                if (world == null) throw new NullPointerException("world");
                final VillagerEntity villager = EntityType.VILLAGER.create(world);
                if (villager == null) throw new NullPointerException("villager");
                villager.setVillagerData(villager.getVillagerData().withProfession(p.get()));
                return new CachedState(new CachedVillagerEntity(villager, Util.milliTime()));
            } catch (final Throwable t) {
                Throwables.throwIfInstanceOf(t, Error.class);
                LOGGER.warn("Error creating render villager", t);
                return new CachedVillagerError(t);
            }
        }));

    @Override
    public void draw(ProfessionEntry profession, MatrixStack stack, double mouseX, double mouseY) {
        final Minecraft minecraft = Minecraft.getInstance();
        final int nameX = (this.background.getWidth() - minecraft.fontRenderer.getStringWidth(profession.getName())) / 2;
        minecraft.fontRenderer.drawString(stack, profession.getName(), nameX, 0, 0xFF808080);
        this.cache.getUnchecked(profession).render(stack);
    }

    @Override
    public List<ITextComponent> getTooltipStrings(final ProfessionEntry profession, final double mouseX, final double mouseY) {
        if (this.isOverEntity(mouseX, mouseY)) {
            final List<ITextComponent> tooltip = new ArrayList<>();
            this.cache.getUnchecked(profession).tooltip(tooltip);
            return tooltip;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean handleClick(final ProfessionEntry profession, final double mouseX, final double mouseY, final int mouseButton) {
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_1 && this.isOverEntity(mouseX, mouseY)) {
            this.cache.getUnchecked(profession).press();
        }
        return false;
    }

    private boolean isOverEntity(final double mouseX, final double mouseY) {
        return mouseX >= 57.0D && mouseX < 83.0D && mouseY >= 12.0D && mouseY < 50.0D;
    }

    public static void drawEntity(final MatrixStack stack, final int posX, final int posY, final int scale, final LivingEntity living) {
        stack.push();
        stack.translate(posX, posY, 1050.0D);
        stack.getLast().getMatrix().mul(Matrix4f.makeScale(1.0F, 1.0F, -1.0F));
        stack.translate(0.0D, 0.0D, 1000.0D);
        stack.scale(scale, scale, scale);
        stack.rotate(Vector3f.ZP.rotationDegrees(180.0F));
        living.rotationYaw = 180.0F - Util.milliTime() / 20.0F;
        living.renderYawOffset = living.rotationYaw;
        living.rotationPitch = -5.0F;
        living.rotationYawHead = living.rotationYaw;
        living.prevRotationYawHead = living.rotationYaw;
        final EntityRendererManager renderer = Minecraft.getInstance().getRenderManager();
        renderer.setCameraOrientation(Quaternion.ONE);
        renderer.setRenderShadow(false);
        final IRenderTypeBuffer.Impl buf = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        try {
            RenderSystem.runAsFancy(() -> renderer.renderEntityStatic(living, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, stack, buf, 0xF000F0));
        } finally {
            buf.finish();
            renderer.setRenderShadow(true);
            stack.pop();
        }
    }

    interface CachedVillager {
        void render(final MatrixStack stack);

        void remove();

        void tooltip(final List<ITextComponent> lines);

        void press();
    }

    static class CachedState implements CachedVillager {
        CachedVillager state;

        CachedState(final CachedVillager state) {
            this.state = state;
        }

        @Override
        public void render(final MatrixStack stack) {
            try {
                this.state.render(stack);
            } catch (final Throwable t) {
                Throwables.throwIfInstanceOf(t, Error.class);
                LOGGER.warn("Error rendering", t);
                this.state = new CachedVillagerError(t);
            }
        }

        @Override
        public void remove() {
            this.state.remove();
        }

        @Override
        public void tooltip(final List<ITextComponent> lines) {
            this.state.tooltip(lines);
        }

        @Override
        public void press() {
            this.state.press();
        }
    }

    static class CachedVillagerEntity implements CachedVillager {
        final VillagerEntity entity;
        final long creationTime;
        long soundTime;

        CachedVillagerEntity(final VillagerEntity entity, final long creationTime) {
            this.entity = entity;
            this.creationTime = creationTime;
        }

        @Override
        public void render(final MatrixStack stack) {
            drawEntity(stack, 70, 48, 16, this.entity);
            final long now = Util.milliTime();
            if (now - this.creationTime > 600000) {
                final long t = now / 50;
                if (this.soundTime == 0) {
                    this.soundTime = t;
                }
                final Random rng = this.entity.getRNG();
                if (rng.nextInt(1000) < t - this.soundTime) {
                    this.soundTime = t + 80;
                    Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.ENTITY_VILLAGER_AMBIENT, (rng.nextFloat() - rng.nextFloat()) * 0.2F + 1.0F, 1.0F));
                }
            }
        }

        @Override
        public void remove() {
            this.entity.remove(false);
        }

        @Override
        public void tooltip(final List<ITextComponent> lines) {
            lines.add(this.entity.getDisplayName());
        }

        @Override
        public void press() {
        }
    }

    static class CachedVillagerError implements CachedVillager {
        final Throwable error;

        CachedVillagerError(final Throwable error) {
            this.error = error;
        }

        @Override
        public void render(final MatrixStack stack) {
            Minecraft.getInstance().fontRenderer.func_243246_a(stack, new StringTextComponent(":(").mergeStyle(TextFormatting.BOLD), 66, 27, 0xFFA04040);
        }

        @Override
        public void remove() {
        }

        @Override
        public void tooltip(final List<ITextComponent> lines) {
            lines.add(new TranslationTextComponent("jeiprofessions.error").mergeStyle(TextFormatting.RED, TextFormatting.BOLD));
            lines.add(new TranslationTextComponent("jeiprofessions.log").mergeStyle(TextFormatting.GRAY, TextFormatting.ITALIC));
        }

        @Override
        public void press() {
        }
    }
}
