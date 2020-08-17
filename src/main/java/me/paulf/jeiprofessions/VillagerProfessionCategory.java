package me.paulf.jeiprofessions;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;

public class VillagerProfessionCategory implements IRecipeCategory<ProfessionEntry> {
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
        layout.getItemStacks().init(0, true, 0, 0);
        layout.getItemStacks().set(ingredients);
    }

    @Override
    public void draw(final ProfessionEntry profession, final double mouseX, final double mouseY) {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.fontRenderer.drawString(profession.getName(), 21.0F, 5.0F, 0xFF808080);
    }
}
