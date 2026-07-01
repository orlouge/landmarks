package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.List;

public record Convolution(DensityFunction input, List<Double> kernel, Double factor) implements DensityFunction {
    public static final MapCodec<Convolution> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("input").forGetter(Convolution::input),
        Codec.DOUBLE.listOf().validate(
            k -> {
                int sk = (int) Math.cbrt(k.size());
                return sk * sk * sk == k.size() && sk % 2 != 0 ? DataResult.success(k)
                        : DataResult.error(() -> "The kernel length must be the cube of an odd number."); }
        ).fieldOf("kernel").forGetter(Convolution::kernel),
        Codec.DOUBLE.fieldOf("factor").forGetter(Convolution::factor)
    ).apply(instance, Convolution::new));
    public static final KeyDispatchDataCodec<Convolution> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        int k = ((int) Math.cbrt(kernel.size())), off = k / 2;
        double acc = 0;
        for (int y = -off; y <= off; y++) {
            for (int z = -off; z <= off; z++) {
                for (int x = -off; x <= off; x++) {
                    double f = kernel.get(k * k * (y + off) + k * (z + off) + x + off);
                    if (f == 0) continue;
                    acc += f * input.compute(new DensityFunction.SinglePointContext(
                        pos.blockX() + x,
                        pos.blockY() + y,
                        pos.blockZ() + z
                    ));
                }
            }
        }
        return acc * factor;
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Convolution(visitor.apply(this.input), kernel, factor);
    }

    @Override
    public double minValue() {
        return this.input.minValue();
    }

    @Override
    public double maxValue() {
        return this.input.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
