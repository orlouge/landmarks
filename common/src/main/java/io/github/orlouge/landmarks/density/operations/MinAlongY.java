package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record MinAlongY(DensityFunction argument, DensityFunction minY, DensityFunction maxY) implements DensityFunction {
    public static final MapCodec<MinAlongY> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(MinAlongY::argument),
        DensityFunction.FUNCTION_CODEC.fieldOf("min_y").forGetter(MinAlongY::minY),
        DensityFunction.FUNCTION_CODEC.fieldOf("max_y").forGetter(MinAlongY::maxY)
    ).apply(instance, MinAlongY::new));
    public static final CodecHolder<MinAlongY> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        double min = Double.POSITIVE_INFINITY;
        for (int y = (int) minY.sample(pos); y <= (int) maxY.sample(pos); y++) {
            min = Math.min(min, argument.sample(new UnblendedNoisePos(pos.blockX(), y, pos.blockZ())));
        }
        return min;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new MinAlongY(this.argument.apply(visitor), this.minY.apply(visitor), this.maxY.apply(visitor)));
    }

    @Override
    public double minValue() {
        return argument.minValue();
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
