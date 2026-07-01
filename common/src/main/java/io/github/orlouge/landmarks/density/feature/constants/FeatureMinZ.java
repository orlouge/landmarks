package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class FeatureMinZ implements DensityFunction.SimpleFunction {
    public static final MapCodec<FeatureMinZ> CODEC = MapCodec.unit(new FeatureMinZ());
    public static final KeyDispatchDataCodec<FeatureMinZ> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);
    private double value = 0;

    public FeatureMinZ create(double value) {
        FeatureMinZ copy = new FeatureMinZ();
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
