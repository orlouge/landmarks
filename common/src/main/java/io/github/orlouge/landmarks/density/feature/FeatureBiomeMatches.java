package io.github.orlouge.landmarks.density.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.List;

public record FeatureBiomeMatches(
    List<HolderSet<Biome>> filter,
    List<HolderSet<Biome>> negativeFilter,
    WorldGenLevel world
) implements DensityFunction.SimpleFunction {
    public static final MapCodec<FeatureBiomeMatches> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RegistryCodecs.homogeneousList(Registries.BIOME).listOf().optionalFieldOf("matches", List.of()).forGetter(FeatureBiomeMatches::filter),
        RegistryCodecs.homogeneousList(Registries.BIOME).listOf().optionalFieldOf("matches_not", List.of()).forGetter(FeatureBiomeMatches::negativeFilter)
    ).apply(instance, FeatureBiomeMatches::new));
    public static final KeyDispatchDataCodec<FeatureBiomeMatches> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    public FeatureBiomeMatches(List<HolderSet<Biome>> filter, List<HolderSet<Biome>> negativeFilter) {
        this(filter, negativeFilter, null);
    }

    public FeatureBiomeMatches create(WorldGenLevel world) {
        return new FeatureBiomeMatches(filter, negativeFilter, world);
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        if (this.world == null)
            throw new RuntimeException("Attempted to sample FeatureBiomeMatches outside of the feature.");

        Holder<Biome> biome = world.getBiome(new BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()));
        for (HolderSet<Biome> filter : this.filter) {
            if (!filter.contains(biome)) return 0;
        }
        for (HolderSet<Biome> filter : this.negativeFilter) {
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
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
