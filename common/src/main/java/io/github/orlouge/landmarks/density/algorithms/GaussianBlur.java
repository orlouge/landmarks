package io.github.orlouge.landmarks.density.algorithms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.BoundedFunction;
import io.github.orlouge.landmarks.density.FunctionWithCache;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Optional;

public class GaussianBlur implements DensityFunction, FunctionWithCache.Simple {
    private static final MapCodec<GaussianBlur> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.optionalFieldOf("kernel_radius", 3).forGetter(d -> d.kernelRadius),
        Codec.DOUBLE.optionalFieldOf("sigma", 2.0).forGetter(d -> d.sigma),
        Codec.BOOL.optionalFieldOf("normalize", true).forGetter(d -> d.normalize),
        Codec.BOOL.optionalFieldOf("rescale", false).forGetter(d -> d.rescale),
        Codec.BOOL.optionalFieldOf("ignore_zeros", true).forGetter(d -> d.ignoreZeros),
        Codec.STRING.optionalFieldOf("key", "").forGetter(d -> d.key),
        DensityFunction.CODEC.fieldOf("argument").forGetter(d -> d.argument),
        DensityFunction.CODEC.fieldOf("min_x").forGetter(d -> d.minX),
        DensityFunction.CODEC.fieldOf("min_z").forGetter(d -> d.minZ),
        DensityFunction.CODEC.fieldOf("max_x").forGetter(d -> d.maxX),
        DensityFunction.CODEC.fieldOf("max_z").forGetter(d -> d.maxZ),
        DensityFunction.CODEC.optionalFieldOf("y").forGetter(d -> d.y)
    ).apply(instance, GaussianBlur::new));
    public static final KeyDispatchDataCodec<GaussianBlur> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    public final int kernelRadius;
    public final double sigma;
    public final boolean normalize, rescale, ignoreZeros;
    public final String key;
    public final DensityFunction argument, minX, minZ, maxX, maxZ;
    public final Optional<DensityFunction> y;
    private final Cache cache;

    private GaussianBlur(int kernelRadius, double sigma, boolean normalize, boolean rescale, boolean ignoreZeros, String key, DensityFunction argument, DensityFunction minX, DensityFunction minZ, DensityFunction maxX, DensityFunction maxZ, Optional<DensityFunction> y, Cache cache) {
        this.kernelRadius = kernelRadius;
        this.sigma = sigma;
        this.normalize = normalize;
        this.rescale = rescale;
        this.ignoreZeros = ignoreZeros;
        this.key = key;
        this.argument = argument;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.y = y;
        this.cache = cache;
    }

    public GaussianBlur(int kernelRadius, double sigma, boolean normalize, boolean rescale, boolean ignoreZeros, String key, DensityFunction argument, DensityFunction minX, DensityFunction minZ, DensityFunction maxX, DensityFunction maxZ, Optional<DensityFunction> y) {
        this.kernelRadius = kernelRadius;
        this.sigma = sigma;
        this.normalize = normalize;
        this.ignoreZeros = ignoreZeros;
        this.rescale = rescale;
        this.key = key.isEmpty() ? null : key;
        this.argument = argument;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.y = y;
        this.cache = null;
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        if (pos.blockX() < cache.minX || pos.blockX() > cache.maxX || pos.blockZ() < cache.minZ || pos.blockZ() > cache.maxZ) return 0;
        double val = cache.blur[pos.blockX() - cache.minX][pos.blockZ() - cache.minZ];
        return rescale ? val * cache.scaleFactor : normalize ? (val - cache.minValue) / (cache.maxValue - cache.minValue) : val;
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new GaussianBlur(kernelRadius, sigma, normalize, rescale, ignoreZeros, key, visitor.apply(argument), visitor.apply(minX), visitor.apply(minZ), visitor.apply(maxX), visitor.apply(maxZ), y, cache);
    }

    @Override
    public double minValue() {
        return (normalize && !rescale) || cache == null ? 0 : cache.minValue;
    }

    @Override
    public double maxValue() {
        return (normalize && !rescale) || cache == null ? 1 : cache.maxValue;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Object createCache(int _minX, int _maxX, int _minY, int _maxY, int _minZ, int _maxZ) {
        int y = (int) (double) this.y.map(d -> d.compute(new DensityFunction.SinglePointContext(0, 0, 0))).orElse(0.0);
        int minX = (int) this.minX.compute(new DensityFunction.SinglePointContext(0, y, 0));
        int maxX = (int) this.maxX.compute(new DensityFunction.SinglePointContext(0, y, 0));
        int minZ = (int) this.minZ.compute(new DensityFunction.SinglePointContext(0, y, 0));
        int maxZ = (int) this.maxZ.compute(new DensityFunction.SinglePointContext(0, y, 0));
        int extX = maxX - minX + 1, extZ = maxZ - minZ + 1;
        double[][] value = new double[extX][extZ];
        boolean[][] isZero = new boolean[extX][extZ];

        if (argument instanceof BoundedFunction maskBounds) {
            minZ = Math.max(minZ, maskBounds.minZ());
            maxZ = Math.min(maxZ, maskBounds.maxZ());
            minX = Math.max(minX, maskBounds.minX());
            maxX = Math.min(maxX, maskBounds.maxX());
        }

        double originalAbsMax = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                value[x - minX][z - minZ] = this.argument.compute(new DensityFunction.SinglePointContext(x, y, z));
                if (ignoreZeros) isZero[x - minX][z - minZ] = value[x - minX][z - minZ] == 0;
                originalAbsMax = Math.max(Math.abs(value[x - minX][z - minZ]), originalAbsMax);
            }
        }

        double[][] blur = io.github.orlouge.landmarks.utils.GaussianBlur.gaussianBlur(value, isZero, kernelRadius, sigma);

        double maxValue = Double.NEGATIVE_INFINITY, minValue = Double.POSITIVE_INFINITY;
        for (double[] row : blur) {
            for (double col : row) {
                maxValue = Math.max(col, maxValue);
                minValue = Math.min(col, minValue);
            }
        }

        double scaleFactor = originalAbsMax / Math.max(Math.abs(maxValue), Math.abs(minValue));
        if (rescale) {
            maxValue *= scaleFactor;
            minValue *= scaleFactor;
        }
        return new Cache(blur, minValue, maxValue, scaleFactor, minX, minZ, maxX, maxZ);
    }

    @Override
    public FunctionWithCache.Simple setCache(Object cache) {
        return new GaussianBlur(kernelRadius, sigma ,normalize, rescale, ignoreZeros, key, argument, minX, minZ, maxX, maxZ, y, (Cache) cache);
    }

    private record Cache(double[][] blur, double minValue, double maxValue, double scaleFactor, int minX, int minZ, int maxX, int maxZ) {}
}
