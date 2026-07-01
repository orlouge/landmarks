package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class FeatureMinY implements DensityFunction.SimpleFunction {
    public static final MapCodec<FeatureMinY> CODEC = MapCodec.unit(new FeatureMinY());
    public static final KeyDispatchDataCodec<FeatureMinY> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);
    private double value = 0;

    public FeatureMinY create(double value) {
        FeatureMinY copy = new FeatureMinY();
        copy.value = value;
        return copy;
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return value;
    }

    @Override
    public double minValue() {
        return -64;
    }

    @Override
    public double maxValue() {
        return 320;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
