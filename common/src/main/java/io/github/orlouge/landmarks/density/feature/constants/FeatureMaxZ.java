package io.github.orlouge.landmarks.density.feature.constants;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureMaxZ implements DensityFunction.Base {
    public static final MapCodec<FeatureMaxZ> CODEC = MapCodec.unit(new FeatureMaxZ());
    public static final CodecHolder<FeatureMaxZ> CODEC_HOLDER = CodecHolder.of(CODEC);
    private double value = 0;

    public FeatureMaxZ create(double value) {
        FeatureMaxZ copy = new FeatureMaxZ();
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
