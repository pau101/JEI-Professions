package me.paulf.jeiprofessions;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mod(JeiProfessions.ID)
public final class JeiProfessions {
    public static final String ID = "jeiprofessions";

    @JeiPlugin
    public static class Plugin implements IModPlugin {
        @Override
        public ResourceLocation getPluginUid() {
            return new ResourceLocation(JeiProfessions.ID, "plugin");
        }

        @Override
        public void registerCategories(final IRecipeCategoryRegistration registration) {
            registration.addRecipeCategories(new VillagerProfessionCategory(registration.getJeiHelpers().getGuiHelper()));
        }

        @Override
        public void registerRecipes(final IRecipeRegistration registration) {
            registration.addRecipes(
                StreamSupport.stream(ForgeRegistries.PROFESSIONS.spliterator(), false)
                    .filter(p -> p != VillagerProfession.NONE)
                    .collect(Collectors.toList()),
                VillagerProfessionCategory.UID
            );
        }
    }
}
