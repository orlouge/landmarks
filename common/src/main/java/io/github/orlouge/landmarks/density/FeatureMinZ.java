package io.github.orlouge.landmarks.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureMinZ implements DensityFunction.Base {
    public static final MapCodec<FeatureMinZ> CODEC = MapCodec.unit(new FeatureMinZ());
    public static final CodecHolder<FeatureMinZ> CODEC_HOLDER = CodecHolder.of(CODEC);
    private double value = 0;

    public FeatureMinZ create(double value) {
        FeatureMinZ copy = new FeatureMinZ();
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
