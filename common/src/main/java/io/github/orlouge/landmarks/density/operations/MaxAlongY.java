package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record MaxAlongY(DensityFunction argument, DensityFunction minY, DensityFunction maxY) implements DensityFunction {
    public static final MapCodec<MaxAlongY> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(MaxAlongY::argument),
        DensityFunction.FUNCTION_CODEC.fieldOf("min_y").forGetter(MaxAlongY::minY),
        DensityFunction.FUNCTION_CODEC.fieldOf("max_y").forGetter(MaxAlongY::maxY)
    ).apply(instance, MaxAlongY::new));
    public static final CodecHolder<MaxAlongY> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        double max = Double.NEGATIVE_INFINITY;
        for (int y = (int) minY.sample(pos); y <= (int) maxY.sample(pos); y++) {
            max = Math.max(max, argument.sample(new UnblendedNoisePos(pos.blockX(), y, pos.blockZ())));
        }
        return max;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new MaxAlongY(this.argument.apply(visitor), this.minY.apply(visitor), this.maxY.apply(visitor)));
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue() {
        return argument.maxValue();
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
