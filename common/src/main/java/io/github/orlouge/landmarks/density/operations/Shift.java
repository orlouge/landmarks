package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Optional;

public record Shift(DensityFunction value, Optional<DensityFunction> xShift, Optional<DensityFunction> yShift, Optional<DensityFunction> zShift) implements DensityFunction {
    public static final MapCodec<Shift> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("input").forGetter(Shift::value),
        DensityFunction.CODEC.optionalFieldOf("x_shift").forGetter(Shift::xShift),
        DensityFunction.CODEC.optionalFieldOf("y_shift").forGetter(Shift::yShift),
        DensityFunction.CODEC.optionalFieldOf("z_shift").forGetter(Shift::zShift)
    ).apply(instance, Shift::new));
    public static final KeyDispatchDataCodec<Shift> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return value.compute(new DensityFunction.SinglePointContext(
            pos.blockX() + xShift.map(f -> (int) Math.round(f.compute(pos))).orElse(0),
            pos.blockY() + yShift.map(f -> (int) Math.round(f.compute(pos))).orElse(0),
            pos.blockZ() + zShift.map(f -> (int) Math.round(f.compute(pos))).orElse(0)
        ));
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Shift(visitor.apply(this.value), this.xShift.map(visitor::apply), this.yShift.map(visitor::apply), this.zShift.map(visitor::apply));
    }

    @Override
    public double minValue() {
        return this.value.minValue();
    }

    @Override
    public double maxValue() {
        return this.value.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
