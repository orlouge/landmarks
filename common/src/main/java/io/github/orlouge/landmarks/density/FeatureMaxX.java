package io.github.orlouge.landmarks.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureMaxX implements DensityFunction.Base {
    public static final MapCodec<FeatureMaxX> CODEC = MapCodec.unit(new FeatureMaxX());
    public static final CodecHolder<FeatureMaxX> CODEC_HOLDER = CodecHolder.of(CODEC);
    private double value = 0;

    public FeatureMaxX create(double value) {
        FeatureMaxX copy = new FeatureMaxX();
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
