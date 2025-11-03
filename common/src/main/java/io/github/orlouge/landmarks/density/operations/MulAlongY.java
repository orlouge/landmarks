package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record MulAlongY(DensityFunction argument, DensityFunction minY, DensityFunction maxY) implements DensityFunction {
    public static final MapCodec<MulAlongY> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(MulAlongY::argument),
        DensityFunction.FUNCTION_CODEC.fieldOf("min_y").forGetter(MulAlongY::minY),
        DensityFunction.FUNCTION_CODEC.fieldOf("max_y").forGetter(MulAlongY::maxY)
    ).apply(instance, MulAlongY::new));
    public static final CodecHolder<MulAlongY> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        double sum = 1;
        for (int y = (int) minY.sample(pos); y <= (int) maxY.sample(pos); y++) {
            sum *= argument.sample(new UnblendedNoisePos(pos.blockX(), y, pos.blockZ()));
        }
        return sum;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new MulAlongY(this.argument.apply(visitor), this.minY.apply(visitor), this.maxY.apply(visitor)));
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
