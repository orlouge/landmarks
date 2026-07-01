package io.github.orlouge.landmarks.density.algorithms;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.utils.OpenSimplex2;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class Noise3D implements DensityFunction.SimpleFunction {
    public static final MapCodec<Noise3D> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.either(Codec.DOUBLE, DensityFunction.CODEC).fieldOf("xz_scale").forGetter(d -> d.xzScale),
        Codec.either(Codec.DOUBLE, DensityFunction.CODEC).fieldOf("y_scale").forGetter(d -> d.yScale),
        Codec.either(Codec.LONG, Codec.STRING).xmap(e -> e.map(l -> l, s -> (long) s.hashCode()), Either::left).optionalFieldOf("seed", 0L).forGetter(d -> d.seed)
    ).apply(instance, Noise3D::new));
    public static final KeyDispatchDataCodec<Noise3D> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    public final Either<Double, DensityFunction> xzScale, yScale;
    public final long seed;

    public Noise3D(Either<Double, DensityFunction> xzScale, Either<Double, DensityFunction> yScale, long seed) {
        this.xzScale = xzScale;
        this.yScale = yScale;
        this.seed = seed;
    }

    public Noise3D create(long featureSeed) {
        return new Noise3D(xzScale, yScale, seed + featureSeed);
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        double xzScale = this.xzScale.map(x -> x, d -> d.compute(pos));
        double yScale = this.yScale.map(x -> x, d -> d.compute(pos));
        double x = (pos.blockX() + (seed & 0xFFFF)) * xzScale, y = (pos.blockY() + (seed & 0xFFFF)) * yScale, z = (pos.blockZ() + (seed & 0xFFFF)) * xzScale;
        return OpenSimplex2.noise3_ImproveXZ(seed, x, y, z);
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
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Noise3D(this.xzScale.mapRight(visitor::apply), this.yScale.mapRight(visitor::apply), seed);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
