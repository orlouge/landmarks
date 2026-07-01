package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Optional;

public record Select(DensityFunction condition, DensityFunction argument1, DensityFunction argument2, Optional<DensityFunction> target) implements DensityFunction {
    public static final MapCodec<Select> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("condition").forGetter(Select::condition),
        DensityFunction.CODEC.fieldOf("argument1").forGetter(Select::argument1),
        DensityFunction.CODEC.fieldOf("argument2").forGetter(Select::argument2),
        DensityFunction.CODEC.optionalFieldOf("target").forGetter(Select::target)
    ).apply(instance, Select::new));
    public static final KeyDispatchDataCodec<Select> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return target.map(t -> condition.compute(pos) == t.compute(pos)).orElse(condition.compute(pos) >= 0) ? argument1.compute(pos) : argument2.compute(pos);
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Select(visitor.apply(this.condition), visitor.apply(this.argument1), visitor.apply(this.argument2), this.target.map(visitor::apply));
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
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
