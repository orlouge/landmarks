package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class FeatureMinX implements DensityFunction.SimpleFunction {
    public static final MapCodec<FeatureMinX> CODEC = MapCodec.unit(new FeatureMinX());
    public static final KeyDispatchDataCodec<FeatureMinX> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);
    private double value = 0;

    public FeatureMinX create(double value) {
        FeatureMinX copy = new FeatureMinX();
        copy.value = value;
        return copy;
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return value;
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
