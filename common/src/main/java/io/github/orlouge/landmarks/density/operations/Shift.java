package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Optional;

public record Shift(DensityFunction value, Optional<DensityFunction> xShift, Optional<DensityFunction> yShift, Optional<DensityFunction> zShift) implements DensityFunction {
    public static final MapCodec<Shift> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("input").forGetter(Shift::value),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("x_shift").forGetter(Shift::xShift),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("y_shift").forGetter(Shift::yShift),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("z_shift").forGetter(Shift::zShift)
    ).apply(instance, Shift::new));
    public static final CodecHolder<Shift> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return value.sample(new UnblendedNoisePos(
            pos.blockX() + xShift.map(f -> (int) Math.round(f.sample(pos))).orElse(0),
            pos.blockY() + yShift.map(f -> (int) Math.round(f.sample(pos))).orElse(0),
            pos.blockZ() + zShift.map(f -> (int) Math.round(f.sample(pos))).orElse(0)
        ));
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Shift(this.value.apply(visitor), this.xShift.map(d -> d.apply(visitor)), this.yShift.map(d -> d.apply(visitor)), this.zShift.map(d -> d.apply(visitor))));
    }

    @Override
    public double minValue() {
        return this.value.minValue();
    }

    @Override
    public double maxValue() {
        return this.value.maxValue();
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
