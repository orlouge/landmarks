package io.github.orlouge.landmarks.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record X() implements DensityFunction.Base {
    public static final MapCodec<X> CODEC = MapCodec.unit(new X());
    public static final CodecHolder<X> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return pos.blockX();
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
