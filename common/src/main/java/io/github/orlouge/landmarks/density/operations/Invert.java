package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record Invert(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<Invert> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(Invert::argument)
    ).apply(instance, Invert::new));
    public static final CodecHolder<Invert> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return 1 / argument.sample(pos);
    }

    @Override
    public double minValue() {
        return argument.minValue() > 0 ? 1 / argument.maxValue() : argument.maxValue() < 0 ? 1 / argument.minValue() : Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue() {
        return argument.minValue() > 0 ? 1 / argument.minValue() : argument.maxValue() < 0 ? 1 / argument.maxValue() : Double.POSITIVE_INFINITY;
    }


    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Invert(this.argument.apply(visitor)));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
