package io.github.orlouge.landmarks.density.operations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Collection;
import java.util.List;

public record Convolution(DensityFunction input, List<Double> kernel, Double factor) implements DensityFunction {
    public static final MapCodec<Convolution> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("input").forGetter(Convolution::input),
        Codec.DOUBLE.listOf().flatXmap(Convolution::checkKernelSize, Convolution::checkKernelSize).fieldOf("kernel").forGetter(Convolution::kernel),
        Codec.DOUBLE.fieldOf("factor").forGetter(Convolution::factor)
    ).apply(instance, Convolution::new));
    public static final CodecHolder<Convolution> CODEC_HOLDER = CodecHolder.of(CODEC);

    private static <T> DataResult<List<T>> checkKernelSize(List<T> k) {
        int sk = (int) Math.cbrt(k.size());
        return sk * sk * sk == k.size() && sk % 2 != 0 ? DataResult.success(k)
            : DataResult.error(() -> "The kernel length must be the cube of an odd number.");
    }

    @Override
    public double sample(NoisePos pos) {
        int k = ((int) Math.cbrt(kernel.size())), off = k / 2;
        double acc = 0;
        for (int y = -off; y <= off; y++) {
            for (int z = -off; z <= off; z++) {
                for (int x = -off; x <= off; x++) {
                    double f = kernel.get(k * k * (y + off) + k * (z + off) + x + off);
                    if (f == 0) continue;
                    acc += f * input.sample(new UnblendedNoisePos(
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
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Convolution(this.input.apply(visitor), kernel, factor));
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
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
