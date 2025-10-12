package io.github.orlouge.landmarks.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record Y() implements DensityFunction.Base {
    public static final MapCodec<Y> CODEC = MapCodec.unit(new Y());
    public static final CodecHolder<Y> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return pos.blockY();
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
