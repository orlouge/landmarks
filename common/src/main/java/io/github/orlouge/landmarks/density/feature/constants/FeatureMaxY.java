package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class FeatureMaxY implements DensityFunction.SimpleFunction {
    public static final MapCodec<FeatureMaxY> CODEC = MapCodec.unit(new FeatureMaxY());
    public static final KeyDispatchDataCodec<FeatureMaxY> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);
    private double value = 0;

    public FeatureMaxY create(double value) {
        FeatureMaxY copy = new FeatureMaxY();
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
