package io.github.orlouge.landmarks.density.feature;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.FunctionWithCache;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Optional;

public class FeatureRandomNumber implements DensityFunction, FunctionWithCache {
    public static final MapCodec<FeatureRandomNumber> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("min").forGetter(d -> d.min),
        DensityFunction.CODEC.fieldOf("max").forGetter(d -> d.max),
        DensityFunction.CODEC.optionalFieldOf("mean").forGetter(d -> d.mean),
        DensityFunction.CODEC.optionalFieldOf("std").forGetter(d -> d.std),
        Codec.BOOL.optionalFieldOf("integer", true).forGetter(d -> d.integer),
        Codec.either(Codec.LONG, Codec.STRING).xmap(e -> e.map(l -> l, s -> (long) s.hashCode()), Either::left).fieldOf("seed").forGetter(d -> d.seed)
    ).apply(instance, FeatureRandomNumber::new));
    public static final KeyDispatchDataCodec<FeatureRandomNumber> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    public final DensityFunction min, max;
    public final Optional<DensityFunction> mean, std;
    public final boolean integer;
    public final long seed;
    private final Cache cache;

    private FeatureRandomNumber(DensityFunction min, DensityFunction max, Optional<DensityFunction> mean, Optional<DensityFunction> std, boolean integer, long seed, Cache cache) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.std = std;
        this.integer = integer;
        this.seed = seed;
        this.cache = cache;
    }

    public FeatureRandomNumber(DensityFunction min, DensityFunction max, Optional<DensityFunction> mean, Optional<DensityFunction> std, boolean integer, long seed) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.std = std;
        this.integer = integer;
        this.seed = seed;
        this.cache = null;
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        if (cache == null) throw new RuntimeException("FeatureRandomNumber sampled outside of the feature type.");
        return cache.value;
    }

    @Override
    public double minValue() {
        return cache == null ? Double.NEGATIVE_INFINITY : cache.value;
    }

    @Override
    public double maxValue() {
        return cache == null ? Double.POSITIVE_INFINITY : cache.value;
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new FeatureRandomNumber(visitor.apply(this.min), visitor.apply(this.max), this.mean.map(visitor::apply), this.std.map(visitor::apply), integer, seed, cache);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }

    public FeatureRandomNumber create(long featureSeed) {
        RandomSource random = RandomSource.create(seed + featureSeed);
        double minValue = min.compute(new DensityFunction.SinglePointContext(0, 0, 0)), maxValue = max.compute(new DensityFunction.SinglePointContext(0, 0, 0));
        double value;
        if (minValue > maxValue) {
            value = minValue;
        } else {
            if (std.isPresent()) {
                double stdValue = std.get().compute(new DensityFunction.SinglePointContext(0, 0, 0));
                double meanValue = mean.map(m -> m.compute(new DensityFunction.SinglePointContext(0, 0, 0))).orElse((maxValue + minValue) / 2);
                value = random.nextGaussian() * stdValue + meanValue;
                value = Math.min(maxValue, Math.max(minValue, value));
                if (integer) value = Math.round(value);
            } else {
                if (integer) {
                    value = random.nextIntBetweenInclusive((int) minValue, (int) maxValue);
                } else {
                    value = random.nextDouble() * (maxValue - minValue) + minValue;
                }
            }
        }
        return new FeatureRandomNumber(min, max, mean, std, integer, seed, new Cache(value));
    }

    @Override
    public String key() {
        return "__feature_random_number_" + seed;
    }

    @Override
    public FunctionWithCache setCache(Object cache) {
        return new FeatureRandomNumber(min, max, mean, std, integer, seed, (Cache) cache);
    }

    public Object getCache() {
        return cache;
    }

    private record Cache(double value) {}
}
