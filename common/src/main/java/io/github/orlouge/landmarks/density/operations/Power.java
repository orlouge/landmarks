package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record Power(DensityFunction argument1, DensityFunction argument2) implements DensityFunction {
    public static final MapCodec<Power> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("argument1").forGetter(Power::argument1),
        DensityFunction.CODEC.fieldOf("argument2").forGetter(Power::argument2)
    ).apply(instance, Power::new));
    public static final KeyDispatchDataCodec<Power> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return Math.pow(argument1.compute(pos), argument2.compute(pos));
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Power(visitor.apply(this.argument1), visitor.apply(this.argument2));
    }

    @Override
    public double minValue() {
        return Math.min(
            Math.min(
                Math.pow(this.argument1.minValue(), this.argument2.minValue()),
                Math.pow(this.argument1.minValue(), this.argument2.maxValue())
            ), Math.min(
                Math.pow(this.argument1.maxValue(), this.argument2.minValue()),
                Math.pow(this.argument1.maxValue(), this.argument2.maxValue())
            )
        );
    }

    @Override
    public double maxValue() {
        return Math.max(
            Math.max(
                Math.pow(this.argument1.minValue(), this.argument2.minValue()),
                Math.pow(this.argument1.minValue(), this.argument2.maxValue())
            ), Math.max(
                Math.pow(this.argument1.maxValue(), this.argument2.minValue()),
                Math.pow(this.argument1.maxValue(), this.argument2.maxValue())
            )
        );
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
