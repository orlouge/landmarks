package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureMinX implements DensityFunction.Base {
    public static final MapCodec<FeatureMinX> CODEC = MapCodec.unit(new FeatureMinX());
    public static final CodecHolder<FeatureMinX> CODEC_HOLDER = CodecHolder.of(CODEC);
    private double value = 0;

    public FeatureMinX create(double value) {
        FeatureMinX copy = new FeatureMinX();
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
