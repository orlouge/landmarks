package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureOriginZ implements DensityFunction.Base {
    public static final MapCodec<FeatureOriginZ> CODEC = MapCodec.unit(new FeatureOriginZ());
    public static final CodecHolder<FeatureOriginZ> CODEC_HOLDER = CodecHolder.of(CODEC);
    private double value = 0;

    public FeatureOriginZ create(double value) {
        FeatureOriginZ copy = new FeatureOriginZ();
        copy.value = value;
        return copy;
    }

    @Override
    public double sample(NoisePos pos) {
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
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
