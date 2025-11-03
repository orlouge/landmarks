package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureOriginY implements DensityFunction.Base {
    public static final MapCodec<FeatureOriginY> CODEC = MapCodec.unit(new FeatureOriginY());
    public static final CodecHolder<FeatureOriginY> CODEC_HOLDER = CodecHolder.of(CODEC);
    private double value = 0;

    public FeatureOriginY create(double value) {
        FeatureOriginY copy = new FeatureOriginY();
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
