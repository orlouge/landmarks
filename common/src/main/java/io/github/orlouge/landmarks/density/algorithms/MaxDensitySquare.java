package io.github.orlouge.landmarks.density.algorithms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.BoundedFunction;
import io.github.orlouge.landmarks.density.FunctionWithCache;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;

public class MaxDensitySquare implements DensityFunction, BoundedFunction, FunctionWithCache.Simple {
    public static final MapCodec<MaxDensitySquare> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.optionalFieldOf("key", "").forGetter(s -> s.key),
        DensityFunction.CODEC.fieldOf("min_x").forGetter(s -> s.x1),
        DensityFunction.CODEC.fieldOf("max_x").forGetter(s -> s.x2),
        DensityFunction.CODEC.fieldOf("min_y").forGetter(s -> s.y1),
        DensityFunction.CODEC.fieldOf("max_y").forGetter(s -> s.y2),
        DensityFunction.CODEC.fieldOf("min_z").forGetter(s -> s.z1),
        DensityFunction.CODEC.fieldOf("max_z").forGetter(s -> s.z2),
        DensityFunction.CODEC.fieldOf("argument").forGetter(s -> s.density),
        DensityFunction.CODEC.fieldOf("min_side").forGetter(s -> s.minSide),
        DensityFunction.CODEC.fieldOf("max_side").forGetter(s -> s.maxSide)
    ).apply(instance, MaxDensitySquare::new));
    public static final KeyDispatchDataCodec<MaxDensitySquare> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    public final String key;
    public final DensityFunction x1, x2, y1, y2, z1, z2, density, minSide, maxSide;
    private final Cache cache;

    public MaxDensitySquare(String key, DensityFunction x1, DensityFunction x2, DensityFunction y1, DensityFunction y2, DensityFunction z1, DensityFunction z2, DensityFunction density, DensityFunction minSide, DensityFunction maxSide, Cache cache) {
        this.key = key;
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.z1 = z1;
        this.z2 = z2;
        this.density = density;
        this.minSide = minSide;
        this.maxSide = maxSide;
        this.cache = cache;
    }
    public MaxDensitySquare(String key, DensityFunction x1, DensityFunction x2, DensityFunction y1, DensityFunction y2, DensityFunction z1, DensityFunction z2, DensityFunction density, DensityFunction minSide, DensityFunction maxSide) {
        this(key.isEmpty() ? null : key, x1, x2, y1, y2, z1, z2, density, minSide, maxSide, new Cache());
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        computeCache(pos);
        if (cache.invalid) return 0;
        return pos.blockX() >= cache.minX && pos.blockX() <= cache.maxX && pos.blockY() >= cache.minY && pos.blockY() <= cache.maxY && pos.blockZ() >= cache.minZ && pos.blockZ() <= cache.maxZ ? 1 : 0;
    }

    private void computeCache(DensityFunction.FunctionContext pos) {
        if (!cache.computed) {
            cache.computed = true;
            cache.minY = -30000000;
            cache.maxY = 30000000;
            int _x1 = (int) x1.compute(pos), _x2 = (int) x2.compute(pos), _y1 = (int) y1.compute(pos);
            int _y2 = (int) y2.compute(pos), _z1 = (int) z1.compute(pos), _z2 = (int) z2.compute(pos);

            if (density instanceof BoundedFunction maskBounds) {
                _z1 = Math.max(_z1, maskBounds.minZ());
                _z2 = Math.min(_z2, maskBounds.maxZ());
                _x1 = Math.max(_x1, maskBounds.minX());
                _x2 = Math.min(_x2, maskBounds.maxX());
                _y1 = Math.max(_y1, maskBounds.minY());
                _y2 = Math.min(_y2, maskBounds.maxY());
            }

            int _minSide = (int) minSide.compute(pos), _maxSide = (int) maxSide.compute(pos);

            if (_x2 < _x1 || _z2 < _z1 || _y2 < _y1 || _maxSide < _minSide) {
                cache.minX = _x2;
                cache.maxX = _x2;
                cache.minZ = _z2;
                cache.maxZ = _z2;
                cache.invalid = true;
                return;
            }

            double[][] density = new double[_x2 - _x1 + 1][_z2 - _z1 + 1];

            for (int x = _x1; x <= _x2; x++) {
                for (int z = _z1; z <= _z2; z++) {
                    for (int y = _y1; y <= _y2; y++) {
                        density[x - _x1][z - _z1] += this.density.compute(new DensityFunction.SinglePointContext(x, y, z));
                    }
                }
            }

            io.github.orlouge.landmarks.utils.MaxDensitySquare.Result square = io.github.orlouge.landmarks.utils.MaxDensitySquare.findDenseSquare(
                density, 1000, RandomSource.create(0),
                rnd -> rnd.nextIntBetweenInclusive(_minSide, _maxSide),
                r -> r.density()
                );
            cache.minX = _x1 + square.x();
            cache.maxX = cache.minX + square.size();
            cache.minZ = _z1 + square.y();
            cache.maxZ = cache.minZ + square.size();
        }
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new MaxDensitySquare(key, visitor.apply(x1), visitor.apply(x2), visitor.apply(y1), visitor.apply(y2), visitor.apply(z1), visitor.apply(z2), visitor.apply(density), visitor.apply(minSide), visitor.apply(maxSide), cache);
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

    @Override
    public int minX() {
        return cache.minX;
    }

    @Override
    public int maxX() {
        return cache.maxX;
    }

    @Override
    public int minY() {
        return cache.minY;
    }

    @Override
    public int maxY() {
        return cache.maxY;
    }

    @Override
    public int minZ() {
        return cache.minZ;
    }

    @Override
    public int maxZ() {
        return cache.maxZ;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Object createCache(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        return new Cache();
    }

    @Override
    public FunctionWithCache.Simple setCache(Object cache) {
        MaxDensitySquare fun = new MaxDensitySquare(key, x1, x2, y1, y2, z1, z2, density, minSide, maxSide, (Cache) cache);
        fun.compute(new DensityFunction.SinglePointContext(0, 0, 0));
        return fun;
    }

    private static class Cache {
        public boolean computed = false, invalid = false;
        public int minX, maxX, minY, maxY, minZ, maxZ;
    }
}
