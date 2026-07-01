package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record MaxAlongY(DensityFunction argument, DensityFunction minY, DensityFunction maxY) implements DensityFunction {
    public static final MapCodec<MaxAlongY> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("argument").forGetter(MaxAlongY::argument),
        DensityFunction.CODEC.fieldOf("min_y").forGetter(MaxAlongY::minY),
        DensityFunction.CODEC.fieldOf("max_y").forGetter(MaxAlongY::maxY)
    ).apply(instance, MaxAlongY::new));
    public static final KeyDispatchDataCodec<MaxAlongY> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        double max = Double.NEGATIVE_INFINITY;
        for (int y = (int) minY.compute(pos); y <= (int) maxY.compute(pos); y++) {
            max = Math.max(max, argument.compute(new DensityFunction.SinglePointContext(pos.blockX(), y, pos.blockZ())));
        }
        return max;
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new MaxAlongY(visitor.apply(this.argument), visitor.apply(this.minY), visitor.apply(this.maxY));
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue() {
        return argument.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
