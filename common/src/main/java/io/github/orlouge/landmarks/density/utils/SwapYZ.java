package io.github.orlouge.landmarks.density.utils;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record SwapYZ(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<SwapYZ> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("argument").forGetter(SwapYZ::argument)
    ).apply(instance, SwapYZ::new));
    public static final KeyDispatchDataCodec<SwapYZ> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return argument.compute(new DensityFunction.SinglePointContext(pos.blockX(), pos.blockZ(), pos.blockY()));
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
        return new SwapYZ(visitor.apply(this.argument));
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
