package io.github.orlouge.landmarks.density.feature;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.FunctionWithCache;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Optional;

public class FeatureRandomGrid implements DensityFunction, FunctionWithCache {
    public static final MapCodec<FeatureRandomGrid> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("min").forGetter(d -> d.min),
        DensityFunction.FUNCTION_CODEC.fieldOf("max").forGetter(d -> d.max),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("mean").forGetter(d -> d.mean),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("std").forGetter(d -> d.std),
        Codec.BOOL.optionalFieldOf("integer", true).forGetter(d -> d.integer),
        Codec.either(Codec.LONG, Codec.STRING).xmap(e -> e.map(l -> l, s -> (long) s.hashCode()), Either::left).fieldOf("seed").forGetter(d -> d.seed)
    ).apply(instance, FeatureRandomGrid::new));
    public static final CodecHolder<FeatureRandomGrid> CODEC_HOLDER = CodecHolder.of(CODEC);

    public final DensityFunction min, max;
    public final Optional<DensityFunction> mean, std;
    public final boolean integer;
    public final long seed;
    private final Cache cache;

    private FeatureRandomGrid(DensityFunction min, DensityFunction max, Optional<DensityFunction> mean, Optional<DensityFunction> std, boolean integer, long seed, Cache cache) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.std = std;
        this.integer = integer;
        this.seed = seed;
        this.cache = cache;
    }

    public FeatureRandomGrid(DensityFunction min, DensityFunction max, Optional<DensityFunction> mean, Optional<DensityFunction> std, boolean integer, long seed) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.std = std;
        this.integer = integer;
        this.seed = seed;
        this.cache = null;
    }

    @Override
    public double sample(NoisePos pos) {
        if (cache == null) throw new RuntimeException("FeatureRandomCreed sampled outside of the feature type.");
        Random random = Random.create(seed + cache.featureSeed + new BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()).hashCode());
        double value;
        if (cache.min > cache.max) {
            value = cache.min;
        } else {
            if (std.isPresent()) {
                value = random.nextGaussian() * cache.std + (cache.max + cache.min) / 2;
                value = Math.min(cache.max, Math.max(cache.min, value));
                if (integer) value = Math.round(value);
            } else {
                if (integer) {
                    value = random.nextBetween((int) cache.min, (int) cache.max);
                } else {
                    value = random.nextDouble() * (cache.max - cache.min) + cache.min;
                }
            }
        }
        return value;
    }

    @Override
    public double minValue() {
        return cache == null ? Double.NEGATIVE_INFINITY : cache.min;
    }

    @Override
    public double maxValue() {
        return cache == null ? Double.POSITIVE_INFINITY : cache.max;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new FeatureRandomGrid(this.min.apply(visitor), this.max.apply(visitor), this.mean.map(f -> f.apply(visitor)), this.std.map(f -> f.apply(visitor)), integer, seed, cache));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }

    public FeatureRandomGrid create(long featureSeed) {
        double minValue = min.sample(new UnblendedNoisePos(0, 0, 0));
        double maxValue = max.sample(new UnblendedNoisePos(0, 0, 0));
        double stdValue = std.map(s -> s.sample(new UnblendedNoisePos(0, 0, 0))).orElse(0.0);
        double meanValue = mean.map(m -> m.sample(new UnblendedNoisePos(0, 0, 0))).orElse((maxValue + minValue) / 2);
        return new FeatureRandomGrid(min, max, mean, std, integer, seed, new Cache(featureSeed, minValue, maxValue, meanValue, stdValue));
    }

    @Override
    public String key() {
        return "__feature_random_grid_" + seed;
    }

    @Override
    public FunctionWithCache setCache(Object cache) {
        return new FeatureRandomGrid(min, max, mean, std, integer, seed, (Cache) cache);
    }

    public Object getCache() {
        return cache;
    }

    private record Cache(long featureSeed, double min, double max, double mean, double std) {}
}
