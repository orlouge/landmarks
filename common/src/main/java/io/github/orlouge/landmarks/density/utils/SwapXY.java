package io.github.orlouge.landmarks.density.utils;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record SwapXY(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<SwapXY> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("argument").forGetter(SwapXY::argument)
    ).apply(instance, SwapXY::new));
    public static final KeyDispatchDataCodec<SwapXY> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return argument.compute(new DensityFunction.SinglePointContext(pos.blockY(), pos.blockX(), pos.blockZ()));
    }

    @Override
    public double minValue() {
        return argument.maxValue();
    }

    @Override
    public double maxValue() {
        return argument.minValue();
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new SwapXY(visitor.apply(this.argument));
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
