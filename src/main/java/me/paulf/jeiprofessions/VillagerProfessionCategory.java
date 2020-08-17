package me.paulf.jeiprofessions;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class VillagerProfessionCategory implements IRecipeCategory<VillagerProfession> {
    public static final ResourceLocation UID = new ResourceLocation("villager_professions");

    private final IDrawable background;

    private final IDrawable icon;

    public VillagerProfessionCategory(final IGuiHelper helper) {
        this.background = helper.createBlankDrawable(120, 18);
        this.icon = helper.createDrawableIngredient(new ItemStack(Items.BELL));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends VillagerProfession> getRecipeClass() {
        return VillagerProfession.class;
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
    public void setIngredients(final VillagerProfession profession, final IIngredients ingredients) {
        ingredients.setInputLists(
            VanillaTypes.ITEM,
            Collections.singletonList(getBlockStates(profession.getPointOfInterest()).stream()
                .map(BlockState::getBlock)
                .distinct()
                .map(ItemStack::new)
                .collect(Collectors.toList()))
        );
    }

    private static Set<BlockState> getBlockStates(final PointOfInterestType poi) {
        return ObfuscationReflectionHelper.getPrivateValue(PointOfInterestType.class, poi, "field_221075_w");
    }

    @Override
    public void setRecipe(final IRecipeLayout layout, final VillagerProfession profession, final IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 0, 0);
        layout.getItemStacks().set(ingredients);
    }

    @Override
    public void draw(final VillagerProfession profession, final double mouseX, final double mouseY) {
        final Minecraft minecraft = Minecraft.getInstance();
        // copypaste from VillagerEntity#getProfessionName
        final ResourceLocation name = Objects.requireNonNull(profession.getRegistryName(), "profession name");
        minecraft.fontRenderer.drawString(I18n.format(EntityType.VILLAGER.getTranslationKey() + '.' + (!"minecraft".equals(name.getNamespace()) ? name.getNamespace() + '.' : "") + name.getPath()), 21.0F, 5.0F, 0xFF808080);
    }
}
