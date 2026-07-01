package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record MulAlongY(DensityFunction argument, DensityFunction minY, DensityFunction maxY) implements DensityFunction {
    public static final MapCodec<MulAlongY> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("argument").forGetter(MulAlongY::argument),
        DensityFunction.CODEC.fieldOf("min_y").forGetter(MulAlongY::minY),
        DensityFunction.CODEC.fieldOf("max_y").forGetter(MulAlongY::maxY)
    ).apply(instance, MulAlongY::new));
    public static final KeyDispatchDataCodec<MulAlongY> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        double sum = 1;
        for (int y = (int) minY.compute(pos); y <= (int) maxY.compute(pos); y++) {
            sum *= argument.compute(new DensityFunction.SinglePointContext(pos.blockX(), y, pos.blockZ()));
        }
        return sum;
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new MulAlongY(visitor.apply(this.argument), visitor.apply(this.minY), visitor.apply(this.maxY));
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
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
