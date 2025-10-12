package io.github.orlouge.landmarks.density;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.utils.OpenSimplex2;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class Noise3D implements DensityFunction.Base {
    public static final MapCodec<Noise3D> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.either(Codec.DOUBLE, DensityFunction.FUNCTION_CODEC).fieldOf("xz_scale").forGetter(d -> d.xzScale),
        Codec.either(Codec.DOUBLE, DensityFunction.FUNCTION_CODEC).fieldOf("y_scale").forGetter(d -> d.yScale),
        Codec.LONG.optionalFieldOf("seed", 0L).forGetter(d -> d.seed)
    ).apply(instance, Noise3D::new));
    public static final CodecHolder<Noise3D> CODEC_HOLDER = CodecHolder.of(CODEC);

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
    public double sample(NoisePos pos) {
        double xzScale = this.xzScale.map(x -> x, d -> d.sample(pos));
        double yScale = this.yScale.map(x -> x, d -> d.sample(pos));
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
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Noise3D(this.xzScale.mapRight(d -> d.apply(visitor)), this.yScale.mapRight(d -> d.apply(visitor)), seed));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
