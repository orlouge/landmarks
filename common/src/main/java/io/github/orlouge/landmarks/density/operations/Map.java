package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Optional;

public record Map(DensityFunction value, Optional<DensityFunction> x, Optional<DensityFunction> y, Optional<DensityFunction> z) implements DensityFunction {
    public static final MapCodec<Map> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("input").forGetter(Map::value),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("x").forGetter(Map::x),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("y").forGetter(Map::y),
        DensityFunction.FUNCTION_CODEC.optionalFieldOf("z").forGetter(Map::z)
    ).apply(instance, Map::new));
    public static final CodecHolder<Map> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return value.sample(new UnblendedNoisePos(
             x.map(f -> (int) Math.round(f.sample(pos))).orElse(pos.blockX()),
             y.map(f -> (int) Math.round(f.sample(pos))).orElse(pos.blockY()),
             z.map(f -> (int) Math.round(f.sample(pos))).orElse(pos.blockZ())
        ));
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Map(this.value.apply(visitor), this.x.map(d -> d.apply(visitor)), this.y.map(d -> d.apply(visitor)), this.z.map(d -> d.apply(visitor))));
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
