package io.github.orlouge.landmarks.density.utils;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record SwapYZ(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<SwapYZ> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(SwapYZ::argument)
    ).apply(instance, SwapYZ::new));
    public static final CodecHolder<SwapYZ> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return argument.sample(new UnblendedNoisePos(pos.blockX(), pos.blockZ(), pos.blockY()));
    }

    @Override
    public double minValue() {
        return argument.maxValue();
    }

    @Override
    public double maxValue() {
        return argument.minValue();
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new SwapYZ(this.argument.apply(visitor)));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
