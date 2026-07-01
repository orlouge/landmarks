package io.github.orlouge.landmarks.density.utils;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record X() implements DensityFunction.SimpleFunction {
    public static final MapCodec<X> CODEC = MapCodec.unit(new X());
    public static final KeyDispatchDataCodec<X> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return pos.blockX();
    }

    @Override
    public double minValue() {
        return -30000000;
    }

    @Override
    public double maxValue() {
        return 30000000;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
