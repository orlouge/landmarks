package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record Select(DensityFunction condition, DensityFunction argument1, DensityFunction argument2) implements DensityFunction {
    public static final MapCodec<Select> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("condition").forGetter(Select::condition),
        DensityFunction.FUNCTION_CODEC.fieldOf("argument1").forGetter(Select::argument1),
        DensityFunction.FUNCTION_CODEC.fieldOf("argument2").forGetter(Select::argument2)
    ).apply(instance, Select::new));
    public static final CodecHolder<Select> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return condition.sample(pos) >= 0 ? argument1.sample(pos) : argument2.sample(pos);
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Select(this.condition.apply(visitor), this.argument1.apply(visitor), this.argument2.apply(visitor)));
    }

    @Override
    public double minValue() {
        return Math.min(this.argument1.minValue(), this.argument2.minValue());
    }

    @Override
    public double maxValue() {
        return Math.max(this.argument1.maxValue(), this.argument2.maxValue());
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
