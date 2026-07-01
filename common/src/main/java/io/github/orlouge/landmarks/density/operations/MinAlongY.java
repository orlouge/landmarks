package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record MinAlongY(DensityFunction argument, DensityFunction minY, DensityFunction maxY) implements DensityFunction {
    public static final MapCodec<MinAlongY> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("argument").forGetter(MinAlongY::argument),
        DensityFunction.CODEC.fieldOf("min_y").forGetter(MinAlongY::minY),
        DensityFunction.CODEC.fieldOf("max_y").forGetter(MinAlongY::maxY)
    ).apply(instance, MinAlongY::new));
    public static final KeyDispatchDataCodec<MinAlongY> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        double min = Double.POSITIVE_INFINITY;
        for (int y = (int) minY.compute(pos); y <= (int) maxY.compute(pos); y++) {
            min = Math.min(min, argument.compute(new DensityFunction.SinglePointContext(pos.blockX(), y, pos.blockZ())));
        }
        return min;
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new MinAlongY(visitor.apply(this.argument), visitor.apply(this.minY), visitor.apply(this.maxY));
    }

    @Override
    public double minValue() {
        return argument.minValue();
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
