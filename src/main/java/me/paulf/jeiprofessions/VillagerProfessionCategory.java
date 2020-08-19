package me.paulf.jeiprofessions;

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
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.Vector3f;
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

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class VillagerProfessionCategory implements IRecipeCategory<ProfessionEntry> {
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
        .removalListener((RemovalListener<ProfessionEntry, CachedVillager>) notification -> notification.getValue().entity.remove(false))
        .build(CacheLoader.from(p -> {
            if (p == null) throw new NullPointerException("profession");
            final Minecraft minecraft = Minecraft.getInstance();
            final ClientWorld world = minecraft.world;
            if (world == null) throw new NullPointerException("world");
            final VillagerEntity villager = EntityType.VILLAGER.create(world);
            if (villager == null) throw new NullPointerException("villager");
            villager.setVillagerData(villager.getVillagerData().withProfession(p.get()));
            return new CachedVillager(villager, Util.milliTime());
        }));

    @Override
    public void draw(final ProfessionEntry profession, final double mouseX, final double mouseY) {
        final Minecraft minecraft = Minecraft.getInstance();
        final int nameX = (this.background.getWidth() - minecraft.fontRenderer.getStringWidth(profession.getName())) / 2;
        minecraft.fontRenderer.drawString(profession.getName(), nameX, 0, 0xFF808080);
        final CachedVillager villager = this.cache.getUnchecked(profession);
        drawEntity(70, 48, 16, villager.entity);
        villager.tick();
    }

    public static void drawEntity(final int posX, final int posY, final int scale, final LivingEntity living) {
        RenderSystem.pushMatrix();
        RenderSystem.translatef(posX, posY, 1050.0F);
        RenderSystem.scalef(1.0F, 1.0F, -1.0F);
        final MatrixStack stack = new MatrixStack();
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
        renderer.renderEntityStatic(living, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, stack, buf, 0xF000F0);
        buf.finish();
        renderer.setRenderShadow(true);
        RenderSystem.popMatrix();
    }

    static class CachedVillager {
        final VillagerEntity entity;
        final long creationTime;
        long soundTime;

        CachedVillager(final VillagerEntity entity, final long creationTime) {
            this.entity = entity;
            this.creationTime = creationTime;
        }

        void tick() {
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
    }
}
