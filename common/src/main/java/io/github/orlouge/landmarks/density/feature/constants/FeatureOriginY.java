package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class FeatureOriginY implements DensityFunction.SimpleFunction {
    public static final MapCodec<FeatureOriginY> CODEC = MapCodec.unit(new FeatureOriginY());
    public static final KeyDispatchDataCodec<FeatureOriginY> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);
    private double value = 0;

    public FeatureOriginY create(double value) {
        FeatureOriginY copy = new FeatureOriginY();
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
