package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureMinY implements DensityFunction.Base {
    public static final MapCodec<FeatureMinY> CODEC = MapCodec.unit(new FeatureMinY());
    public static final CodecHolder<FeatureMinY> CODEC_HOLDER = CodecHolder.of(CODEC);
    private double value = 0;

    public FeatureMinY create(double value) {
        FeatureMinY copy = new FeatureMinY();
        copy.value = value;
        return copy;
    }

    @Override
    public double sample(NoisePos pos) {
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
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
