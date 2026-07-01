package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record Invert(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<Invert> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("argument").forGetter(Invert::argument)
    ).apply(instance, Invert::new));
    public static final KeyDispatchDataCodec<Invert> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return 1 / argument.compute(pos);
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
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Invert(visitor.apply(this.argument));
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
