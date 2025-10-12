package io.github.orlouge.landmarks.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureMaxY implements DensityFunction.Base {
    public static final MapCodec<FeatureMaxY> CODEC = MapCodec.unit(new FeatureMaxY());
    public static final CodecHolder<FeatureMaxY> CODEC_HOLDER = CodecHolder.of(CODEC);
    private double value = 0;

    public FeatureMaxY create(double value) {
        FeatureMaxY copy = new FeatureMaxY();
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
