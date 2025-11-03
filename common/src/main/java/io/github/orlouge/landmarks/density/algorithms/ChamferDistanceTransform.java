package io.github.orlouge.landmarks.density.algorithms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.FunctionWithCache;
import io.github.orlouge.landmarks.utils.ChamferTransform;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Optional;

public class ChamferDistanceTransform implements FunctionWithCache.Simple {
    private static final MapCodec<ChamferDistanceTransform> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("normalize", true).forGetter(d -> d.normalize),
        Codec.STRING.optionalFieldOf("key", "").forGetter(d -> d.key),
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(d -> d.argument),
        DensityFunction.FUNCTION_CODEC.fieldOf("min_x").forGetter(d -> d.minX),
        DensityFunction.FUNCTION_CODEC.fieldOf("min_z").forGetter(d -> d.minZ),
        DensityFunction.FUNCTION_CODEC.fieldOf("max_x").forGetter(d -> d.maxX),
        DensityFunction.FUNCTION_CODEC.fieldOf("max_z").forGetter(d -> d.maxZ),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("y").forGetter(d -> d.y)
        ).apply(instance, ChamferDistanceTransform::new));
    public static final CodecHolder<ChamferDistanceTransform> CODEC_HOLDER = CodecHolder.of(CODEC);

    public final boolean normalize;
    public final String key;
    public final DensityFunction argument, minX, minZ, maxX, maxZ;
    public final Optional<DensityFunction> y;
    private final Cache cache;
    public ChamferDistanceTransform(boolean normalize, String key, DensityFunction argument, DensityFunction minX, DensityFunction minZ, DensityFunction maxX, DensityFunction maxZ, Optional<DensityFunction> y) {
        this.normalize = normalize;
        this.key = key.isEmpty() ? null : key;
        this.argument = argument;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.y = y;
        this.cache = null;
    }

    private ChamferDistanceTransform(boolean normalize, String key, DensityFunction argument, DensityFunction minX, DensityFunction minZ, DensityFunction maxX, DensityFunction maxZ, Optional<DensityFunction> y, Cache cache) {
        this.normalize = normalize;
        this.key = key;
        this.argument = argument;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.y = y;
        this.cache = cache;
    }

    @Override
    public double sample(NoisePos pos) {
        if (pos.blockX() < cache.minX || pos.blockX() > cache.maxX || pos.blockZ() < cache.minZ || pos.blockZ() > cache.maxZ) return 0;
        double val = cache.dist[pos.blockX() - cache.minX][pos.blockZ() - cache.minZ];
        return normalize ? val / cache.maxDist : val;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new ChamferDistanceTransform(normalize, key, argument.apply(visitor), minX.apply(visitor), minZ.apply(visitor), maxX.apply(visitor), maxZ.apply(visitor), y, cache));
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return normalize || cache == null ? 1 : cache.maxDist;
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
    public Object createCache(int _minX, int _maxX, int _minY, int _maxY, int _minZ, int _maxZ) {
        int y = (int) (double) this.y.map(d -> d.sample(new UnblendedNoisePos(0, 0, 0))).orElse(0.0);
        int minX = (int) this.minX.sample(new UnblendedNoisePos(0, y, 0));
        int maxX = (int) this.maxX.sample(new UnblendedNoisePos(0, y, 0));
        int minZ = (int) this.minZ.sample(new UnblendedNoisePos(0, y, 0));
        int maxZ = (int) this.maxZ.sample(new UnblendedNoisePos(0, y, 0));
        int extX = maxX - minX + 1, extZ = maxZ - minZ + 1;
        boolean[][] isZero = new boolean[extX][extZ];

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                isZero[x - minX][z - minZ] = this.argument.sample(new UnblendedNoisePos(x, y, z)) <= 0;
            }
        }

        double[][] dist = ChamferTransform.distanceTransform(isZero);
        double maxDist = 0;
        for (int x = 0; x < dist.length; x++) {
            for (int z = 0; z < dist[0].length; z++) {
                dist[x][z] /= 2.0;
                maxDist = Math.max(dist[x][z], maxDist);
            }
        }

        return new Cache(dist, maxDist > 0 ? maxDist : 1, minX, maxX, minZ, maxZ);
    }

    @Override
    public FunctionWithCache.Simple setCache(Object cache) {
        return new ChamferDistanceTransform(normalize, key, argument, minX, minZ, maxX, maxZ, y, (Cache) cache);
    }

    private record Cache(double[][] dist, double maxDist, int minX, int maxX, int minZ, int maxZ) {}
}
