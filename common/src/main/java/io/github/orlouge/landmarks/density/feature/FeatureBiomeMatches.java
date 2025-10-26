package io.github.orlouge.landmarks.density.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.List;

public record FeatureBiomeMatches(
    List<RegistryEntryList<Biome>> filter,
    List<RegistryEntryList<Biome>> negativeFilter,
    StructureWorldAccess world
) implements DensityFunction.Base {
    public static final MapCodec<FeatureBiomeMatches> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RegistryCodecs.entryList(RegistryKeys.BIOME).listOf().optionalFieldOf("matches", List.of()).forGetter(FeatureBiomeMatches::filter),
        RegistryCodecs.entryList(RegistryKeys.BIOME).listOf().optionalFieldOf("matches_not", List.of()).forGetter(FeatureBiomeMatches::negativeFilter)
    ).apply(instance, FeatureBiomeMatches::new));
    public static final CodecHolder<FeatureBiomeMatches> CODEC_HOLDER = CodecHolder.of(CODEC);

    public FeatureBiomeMatches(List<RegistryEntryList<Biome>> filter, List<RegistryEntryList<Biome>> negativeFilter) {
        this(filter, negativeFilter, null);
    }

    public FeatureBiomeMatches create(StructureWorldAccess world) {
        return new FeatureBiomeMatches(filter, negativeFilter, world);
    }

    @Override
    public double sample(NoisePos pos) {
        if (this.world == null)
            throw new RuntimeException("Attempted to sample FeatureBiomeMatches outside of the feature.");

        RegistryEntry<Biome> biome = world.getBiome(new BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()));
        for (RegistryEntryList<Biome> filter : this.filter) {
            if (!filter.contains(biome)) return 0;
        }
        for (RegistryEntryList<Biome> filter : this.negativeFilter) {
            if (filter.contains(biome)) return 0;
        }
        return 1;
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return 1;
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
