package io.github.orlouge.landmarks.density.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.BoundedFunction;
import io.github.orlouge.landmarks.density.FunctionWithCache;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureMass implements DensityFunction, FunctionWithCache.Simple {
    public static final MapCodec<FeatureMass> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(d -> d.argument),
        Codec.STRING.optionalFieldOf("key", "").forGetter(d -> d.key)
    ).apply(instance, FeatureMass::new));
    public static final CodecHolder<FeatureMass> CODEC_HOLDER = CodecHolder.of(CODEC);

    public final DensityFunction argument;
    public final String key;
    private final Cache cache;

    private FeatureMass(DensityFunction argument, String key, Cache cache) {
        this.argument = argument;
        this.key = key;
        this.cache = cache;
    }

    public FeatureMass(DensityFunction argument, String key) {
        this.argument = argument;
        this.key = key.isEmpty() ? null : key;
        this.cache = null;
    }

    @Override
    public double sample(NoisePos pos) {
        if (cache == null) throw new RuntimeException("FeatureMass sampled outside of the feature type.");
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
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new FeatureMass(this.argument.apply(visitor), key, cache));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Object createCache(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        double value = 0;
        if (argument instanceof BoundedFunction maskBounds) {
            minZ = Math.max(minZ, maskBounds.minZ());
            maxZ = Math.min(maxZ, maskBounds.maxZ());
            minX = Math.max(minX, maskBounds.minX());
            maxX = Math.min(maxX, maskBounds.maxX());
            minY = Math.max(minY, maskBounds.minY());
            maxY = Math.min(maxY, maskBounds.maxY());
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    value += argument.sample(new UnblendedNoisePos(x, y, z));
                }
            }
        }
        return new Cache(value);
    }

    @Override
    public FunctionWithCache.Simple setCache(Object cache) {
        return new FeatureMass(argument, key, (Cache) cache);
    }

    private record Cache(double value) {}
}
