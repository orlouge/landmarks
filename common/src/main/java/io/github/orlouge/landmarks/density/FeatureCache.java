package io.github.orlouge.landmarks.density;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.utils.OpenSimplex2;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Arrays;

public class FeatureCache implements DensityFunction {
    public static final MapCodec<FeatureCache> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(d -> d.argument),
        Codec.STRING.fieldOf("key").forGetter(d -> d.key),
        Codec.BOOL.optionalFieldOf("y2d", false).forGetter(d -> d.y2d)
    ).apply(instance, FeatureCache::new));
    public static final CodecHolder<FeatureCache> CODEC_HOLDER = CodecHolder.of(CODEC);

    public final DensityFunction argument;
    public final String key;
    public final boolean y2d;
    private final double[] cache;
    private final int cacheMinX, cacheMaxX, cacheMinY, cacheMaxY, cacheMinZ, cacheMaxZ, xExt, zExt;

    public FeatureCache(DensityFunction argument, String key, boolean y2d, double[] cache, int cacheMinX, int cacheMaxX, int cacheMinY, int cacheMaxY, int cacheMinZ, int cacheMaxZ) {
        this.argument = argument;
        this.key = key;
        this.y2d = y2d;
        this.cache = cache;
        this.cacheMinX = cacheMinX;
        this.cacheMaxX = cacheMaxX;
        this.cacheMinY = cacheMinY;
        this.cacheMaxY = cacheMaxY;
        this.cacheMinZ = cacheMinZ;
        this.cacheMaxZ = cacheMaxZ;
        this.xExt = cacheMaxX - cacheMinX + 1;
        this.zExt = cacheMaxZ - cacheMinZ + 1;
    }

    public FeatureCache(DensityFunction argument, String key, boolean y2d) {
        this.argument = argument;
        this.key = key;
        this.y2d = y2d;
        this.cache = null;
        this.cacheMinX = 0;
        this.cacheMaxX = 0;
        this.cacheMinY = 0;
        this.cacheMaxY = 0;
        this.cacheMinZ = 0;
        this.cacheMaxZ = 0;
        this.xExt = 0;
        this.zExt = 0;
    }

    public FeatureCache create(double[] cache, int cacheMinX, int cacheMaxX, int cacheMinY, int cacheMaxY, int cacheMinZ, int cacheMaxZ) {
        Arrays.fill(cache, Double.NaN);
        return new FeatureCache(argument, key, y2d, cache, cacheMinX, cacheMaxX, cacheMinY, cacheMaxY, cacheMinZ, cacheMaxZ);
    }

    @Override
    public double sample(NoisePos pos) {
        if (cache == null || pos.blockX() < cacheMinX || pos.blockX() > cacheMaxX || (!y2d && (pos.blockY() < cacheMinY || pos.blockY() > cacheMaxY)) || pos.blockZ() < cacheMinZ || pos.blockZ() > cacheMaxZ) {
            return sampleWithoutCache(pos);
        } else {
            int cacheX = pos.blockX() - cacheMinX, cacheY = pos.blockY() - cacheMinY, cacheZ = pos.blockZ() - cacheMinZ;
            int idx = !y2d ? (xExt * cacheY + cacheX) * zExt + cacheZ : zExt * cacheX + cacheZ;
            double cachedValue = cache[idx];
            if (Double.isNaN(cachedValue)) {
                cachedValue = sampleWithoutCache(pos);
                cache[idx] = cachedValue;
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
        return visitor.apply(new FeatureCache(this.argument.apply(visitor), key, y2d, cache, cacheMinX, cacheMaxX, cacheMinY, cacheMaxY, cacheMinZ, cacheMaxZ));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
