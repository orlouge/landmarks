package io.github.orlouge.landmarks.density;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.utils.OpenSimplex2;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class Noise2D implements DensityFunction {
    public static final MapCodec<Noise2D> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.either(Codec.DOUBLE, DensityFunction.FUNCTION_CODEC).fieldOf("xz_scale").forGetter(d -> d.xzScale),
        Codec.LONG.optionalFieldOf("seed", 0L).forGetter(d -> d.seed)
    ).apply(instance, Noise2D::new));
    public static final CodecHolder<Noise2D> CODEC_HOLDER = CodecHolder.of(CODEC);

    public final Either<Double, DensityFunction> xzScale;
    public final long seed;
    private final double[][] cache;
    private final int cacheMinX, cacheMaxX, cacheMinZ, cacheMaxZ;

    public Noise2D(Either<Double, DensityFunction> xzScale, long seed, double[][] cache, int cacheMinX, int cacheMaxX, int cacheMinZ, int cacheMaxZ) {
        this.xzScale = xzScale;
        this.seed = seed;
        this.cache = cache;
        this.cacheMinX = cacheMinX;
        this.cacheMaxX = cacheMaxX;
        this.cacheMinZ = cacheMinZ;
        this.cacheMaxZ = cacheMaxZ;
    }

    public Noise2D(Either<Double, DensityFunction> xzScale, long seed) {
        this.xzScale = xzScale;
        this.seed = seed;
        this.cache = null;
        this.cacheMinX = 0;
        this.cacheMaxX = 0;
        this.cacheMinZ = 0;
        this.cacheMaxZ = 0;
    }

    public Noise2D create(long featureSeed, double[][] cache, int cacheMinX, int cacheMaxX, int cacheMinZ, int cacheMaxZ) {
        for (int x = 0; x < cache.length; x++) {
            for (int z = 0; z < cache[0].length; z++) {
                cache[x][z] = Double.NaN;
            }
        }
        return new Noise2D(xzScale, seed + featureSeed, cache, cacheMinX, cacheMaxX, cacheMinZ, cacheMaxZ);
    }

    @Override
    public double sample(NoisePos pos) {
        if (cache == null || pos.blockX() < cacheMinX || pos.blockX() > cacheMaxX || pos.blockZ() < cacheMinZ || pos.blockZ() > cacheMaxZ) {
            return sampleWithoutCache(pos);
        } else {
            int cacheX = pos.blockX() - cacheMinX, cacheZ = pos.blockZ() - cacheMinZ;
            double cachedValue = cache[cacheX][cacheZ];
            if (Double.isNaN(cachedValue)) {
                cachedValue = sampleWithoutCache(pos);
                cache[cacheX][cacheZ] = cachedValue;
            }
            return cachedValue;
        }
    }

    private float sampleWithoutCache(NoisePos pos) {
        double xzScale = this.xzScale.map(x -> x, d -> d.sample(pos));
        double x = (pos.blockX() + (seed & 0xFFFF)) * xzScale, z = (pos.blockZ() + (seed & 0xFFFF)) * xzScale;
        return OpenSimplex2.noise2(seed, x, z);
    }

    @Override
    public double minValue() {
        return -1;
    }

    @Override
    public double maxValue() {
        return 1;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Noise2D(this.xzScale.mapRight(d -> d.apply(visitor)), seed, cache, cacheMinX, cacheMaxX, cacheMinZ, cacheMaxZ));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
