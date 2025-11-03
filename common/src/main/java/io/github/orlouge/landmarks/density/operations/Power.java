package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record Power(DensityFunction argument1, DensityFunction argument2) implements DensityFunction {
    public static final MapCodec<Power> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument1").forGetter(Power::argument1),
        DensityFunction.FUNCTION_CODEC.fieldOf("argument2").forGetter(Power::argument2)
    ).apply(instance, Power::new));
    public static final CodecHolder<Power> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return Math.pow(argument1.sample(pos), argument2.sample(pos));
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Power(this.argument1.apply(visitor), this.argument2.apply(visitor)));
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
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
