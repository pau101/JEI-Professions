package me.paulf.jeiprofessions;

import net.minecraft.block.BlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProfessionEntry {
    final VillagerProfession profession;

    final String nameKey;

    final List<List<ItemStack>> inputs;

    private ProfessionEntry(final VillagerProfession profession, final String nameKey, final List<List<ItemStack>> inputs) {
        this.profession = profession;
        this.nameKey = nameKey;
        this.inputs = inputs;
    }

    public String getName() {
        return I18n.format(this.nameKey);
    }

    public List<List<ItemStack>> getInputs() {
        return this.inputs;
    }

    public boolean hasJobSites() {
        return !this.inputs.get(0).isEmpty();
    }

    public static ProfessionEntry of(final VillagerProfession profession) {
        final ResourceLocation name = Objects.requireNonNull(profession.getRegistryName(), "profession name");
        // copypaste from VillagerEntity#getProfessionName
        final String nameKey = I18n.format(EntityType.VILLAGER.getTranslationKey() + '.' + ("minecraft".equals(name.getNamespace()) ? "" : name.getNamespace() + '.') + name.getPath());
        final Set<BlockState> jobSites = Objects.requireNonNull(ObfuscationReflectionHelper.getPrivateValue(PointOfInterestType.class, profession.getPointOfInterest(), "field_221075_w"), "job sites");
        final List<List<ItemStack>> inputs = Collections.singletonList(jobSites.stream().map(BlockState::getBlock).distinct().map(ItemStack::new).collect(Collectors.toList()));
        return new ProfessionEntry(profession, nameKey, inputs);
    }
}
