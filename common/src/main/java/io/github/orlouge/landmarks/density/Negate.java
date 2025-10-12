package io.github.orlouge.landmarks.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record Negate(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<Negate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(Negate::argument)
    ).apply(instance, Negate::new));
    public static final CodecHolder<Negate> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return - argument.sample(pos);
    }

    @Override
    public double minValue() {
        return - argument.maxValue();
    }

    @Override
    public double maxValue() {
        return - argument.minValue();
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Negate(this.argument.apply(visitor)));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
