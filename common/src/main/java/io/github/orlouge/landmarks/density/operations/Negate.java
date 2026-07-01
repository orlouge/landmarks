package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record Negate(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<Negate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("argument").forGetter(Negate::argument)
    ).apply(instance, Negate::new));
    public static final KeyDispatchDataCodec<Negate> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return - argument.compute(pos);
    }

    @Override
    public double minValue() {
        return - argument.maxValue();
    }

    @Override
    public double maxValue() {
        return - argument.minValue();
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Negate(visitor.apply(this.argument));
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
