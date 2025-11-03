package io.github.orlouge.landmarks.density.algorithms;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.FunctionWithCache;
import io.github.orlouge.landmarks.utils.OpenSimplex2;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Arrays;

public class Noise2D implements DensityFunction, FunctionWithCache {
    public static final MapCodec<Noise2D> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.either(Codec.DOUBLE, DensityFunction.FUNCTION_CODEC).fieldOf("xz_scale").forGetter(d -> d.xzScale),
        Codec.BOOL.optionalFieldOf("global", false).forGetter(d -> d.global),
        Codec.either(Codec.LONG, Codec.STRING).xmap(e -> e.map(l -> l, s -> (long) s.hashCode()), Either::left).optionalFieldOf("seed", 0L).forGetter(d -> d.seed)
    ).apply(instance, Noise2D::new));
    public static final CodecHolder<Noise2D> CODEC_HOLDER = CodecHolder.of(CODEC);

    public final Either<Double, DensityFunction> xzScale;
    public final boolean global;
    public final long seed;
    private final Cache cache;

    private Noise2D(Either<Double, DensityFunction> xzScale, boolean global, long seed, Cache cache) {
        this.xzScale = xzScale;
        this.seed = seed;
        this.cache = cache;
        this.global = global;
    }

    public Noise2D(Either<Double, DensityFunction> xzScale, boolean global, long seed) {
        this.xzScale = xzScale;
        this.seed = seed;
        this.cache = null;
        this.global = global;
    }

    public Noise2D create(long seed, int minX, int maxX, int minZ, int maxZ) {
        return new Noise2D(xzScale, global, this.seed, createCache(seed, minX, maxX, minZ, maxZ));
    }

    @Override
    public double sample(NoisePos pos) {
        if (cache == null || pos.blockX() < cache.minX || pos.blockX() > cache.maxX || pos.blockZ() < cache.minZ || pos.blockZ() > cache.maxZ) {
            return sampleWithoutCache(pos, seed);
        } else {
            int cacheX = pos.blockX() - cache.minX, cacheZ = pos.blockZ() - cache.minZ;
            double cachedValue = cache.cache[cacheX][cacheZ];
            if (Double.isNaN(cachedValue)) {
                cachedValue = sampleWithoutCache(pos, seed + cache.featureSeed);
                cache.cache[cacheX][cacheZ] = cachedValue;
            }
            return cachedValue;
        }
    }

    private float sampleWithoutCache(NoisePos pos, long seed) {
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
        return visitor.apply(new Noise2D(this.xzScale.mapRight(d -> d.apply(visitor)), global, seed, cache));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }

    @Override
    public String key() {
        return "__noise2d_" + (global ? "global" : "local") + "_" + seed;
    }

    /*
    @Override
    public Object createCache(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        return createCache(0, minX, maxX, minZ, maxZ);
    }
     */

    private static Cache createCache(long featureSeed, int minX, int maxX, int minZ, int maxZ) {
        double[][] cache = new double[maxX - minX + 1][maxZ - minZ + 1];
        for (double[] row : cache) {
            Arrays.fill(row, Double.NaN);
        }
        return new Cache(featureSeed, cache, minX, maxX, minZ, maxZ);
    }

    @Override
    public Noise2D setCache(Object cache) {
        return new Noise2D(xzScale, global, seed, (Cache) cache);
    }

    public Object getCache() {
        return cache;
    }
    
    private record Cache(long featureSeed, double[][] cache, int minX, int maxX, int minZ, int maxZ) {}
}
