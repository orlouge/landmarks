package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Optional;

public record Map(DensityFunction value, Optional<DensityFunction> x, Optional<DensityFunction> y, Optional<DensityFunction> z) implements DensityFunction {
    public static final MapCodec<Map> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("input").forGetter(Map::value),
        DensityFunction.CODEC.optionalFieldOf("x").forGetter(Map::x),
        DensityFunction.CODEC.optionalFieldOf("y").forGetter(Map::y),
        DensityFunction.CODEC.optionalFieldOf("z").forGetter(Map::z)
    ).apply(instance, Map::new));
    public static final KeyDispatchDataCodec<Map> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return value.compute(new DensityFunction.SinglePointContext(
             x.map(f -> (int) Math.round(f.compute(pos))).orElse(pos.blockX()),
             y.map(f -> (int) Math.round(f.compute(pos))).orElse(pos.blockY()),
             z.map(f -> (int) Math.round(f.compute(pos))).orElse(pos.blockZ())
        ));
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Map(visitor.apply(this.value), this.x.map(visitor::apply), this.y.map(visitor::apply), this.z.map(visitor::apply));
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
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
