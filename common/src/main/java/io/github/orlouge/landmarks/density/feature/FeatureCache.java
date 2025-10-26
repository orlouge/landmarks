package io.github.orlouge.landmarks.density.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.FunctionWithCache;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Arrays;
import java.util.Optional;

public class FeatureCache implements DensityFunction, FunctionWithCache.Simple {
    public static final MapCodec<FeatureCache> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(d -> d.argument),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("repeat_y").forGetter(d -> d.repeatY),
        Codec.STRING.optionalFieldOf("key", "").forGetter(d -> d.key),
        Codec.BOOL.optionalFieldOf("precomputed", false).forGetter(d -> d.precomputed)
    ).apply(instance, FeatureCache::new));
    public static final CodecHolder<FeatureCache> CODEC_HOLDER = CodecHolder.of(CODEC);

    public final DensityFunction argument;
    public final Optional<DensityFunction> repeatY;
    public final String key;
    public final boolean precomputed;
    private final Cache cache;

    private FeatureCache(DensityFunction argument, Optional<DensityFunction> repeatY, String key, boolean precomputed, Cache cache) {
        this.argument = argument;
        this.key = key;
        this.repeatY = repeatY;
        this.cache = cache;
        this.precomputed = precomputed;
    }

    public FeatureCache(DensityFunction argument, Optional<DensityFunction> repeatY, String key, boolean precomputed) {
        this.argument = argument;
        this.key = key.isEmpty() ? null : key;
        this.repeatY = repeatY;
        this.cache = null;
        this.precomputed = precomputed;
    }

    @Override
    public double sample(NoisePos pos) {
        boolean y2d = repeatY.isPresent();
        if (cache == null || pos.blockX() < cache.minX || pos.blockX() > cache.maxX || (!y2d && (pos.blockY() < cache.minY || pos.blockY() > cache.maxY)) || pos.blockZ() < cache.minZ || pos.blockZ() > cache.maxZ) {
            return sampleWithoutCache(pos);
        } else {
            int cacheX = pos.blockX() - cache.minX, cacheY = pos.blockY() - cache.minY, cacheZ = pos.blockZ() - cache.minZ;
            int idx = !y2d ? (cache.xExt * cacheY + cacheX) * cache.zExt + cacheZ : cache.zExt * cacheX + cacheZ;
            double cachedValue = cache.cache[idx];
            if (Double.isNaN(cachedValue)) {
                cachedValue = sampleWithoutCache(pos);
                cache.cache[idx] = cachedValue;
            }
            return cachedValue;
        }
    }

    private double sampleWithoutCache(NoisePos pos) {
        return argument.sample(pos);
    }

    @Override
    public double minValue() {
        return argument.minValue();
    }

    @Override
    public double maxValue() {
        return argument.maxValue();
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new FeatureCache(this.argument.apply(visitor), this.repeatY.map(d -> d.apply(visitor)), key, precomputed, cache));
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
        int xExt = maxX - minX + 1, yExt = maxY - minY + 1, zExt = maxZ - minZ + 1;
        boolean y2d = this.repeatY.isPresent();
        int fixedY = y2d ? (int) this.repeatY.get().sample(new UnblendedNoisePos(0, 0, 0)) : 0;
        double[] cache = y2d ? new double[(xExt + 2) * (zExt + 2)] : new double[(xExt + 2) * (yExt + 2) * (zExt + 2)];
        if (precomputed) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (y2d) {
                        cache[zExt * (x - minX) + z - minZ] = sampleWithoutCache(new UnblendedNoisePos(x, fixedY, z));
                    } else {
                        for (int y = minY; y <= maxY; y++) {
                            cache[(xExt * (y - minY) + x - minX) * zExt + z - minZ] = sampleWithoutCache(new UnblendedNoisePos(x, y, z));
                        }
                    }
                }
            }
        } else {
            Arrays.fill(cache, Double.NaN);
        }
        return new Cache(cache, minX, maxX, minY, maxY, minZ, maxZ, xExt, zExt);
    }

    @Override
    public FunctionWithCache.Simple setCache(Object cache) {
        return new FeatureCache(argument, repeatY, key, precomputed, (Cache) cache);
    }

    private record Cache(double[] cache, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, int xExt, int zExt) {}
}
