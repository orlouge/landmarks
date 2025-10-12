package io.github.orlouge.landmarks.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.utils.GaussianBlur;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class BorderBlurred implements DensityFunction.Base {
    private static final MapCodec<BorderBlurred> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.optionalFieldOf("kernel_radius", 3).forGetter(d -> d.kernelRadius),
        Codec.DOUBLE.optionalFieldOf("sigma", 2.0).forGetter(d -> d.sigma),
        Codec.BOOL.optionalFieldOf("normalize", true).forGetter(d -> d.normalize)
    ).apply(instance, BorderBlurred::new));
    public static final CodecHolder<BorderBlurred> CODEC_HOLDER = CodecHolder.of(CODEC);

    public final int kernelRadius;
    public final double sigma;
    public final boolean normalize;
    private double[][] blur = null;
    private boolean[][] isZero = null;
    private int minX, minZ, maxX, maxZ;
    public double minValue = 0, maxValue = 0;

    public BorderBlurred(int kernelRadius, double sigma, boolean normalize) {
        this.kernelRadius = kernelRadius;
        this.sigma = sigma;
        this.normalize = normalize;
    }


    public BorderBlurred create(boolean[][] isZero, int minX, int maxX, int minZ, int maxZ) {
        BorderBlurred copy = new BorderBlurred(kernelRadius, sigma, normalize);
        copy.isZero = isZero;
        copy.minX = minX;
        copy.maxX = maxX;
        copy.minZ = minZ;
        copy.maxZ = maxZ;
        return copy;
    }

    @Override
    public double sample(NoisePos pos) {
        if (blur == null) {
            if (isZero != null) {
                double[][] value = new double[isZero.length][isZero[0].length];

                for (int x = 0; x < isZero.length; x++) {
                    for (int z = 0; z < isZero[0].length; z++) {
                        value[x][z] = isZero[x][z] ? 0 : 1;
                    }
                }

                blur = GaussianBlur.gaussianBlur(value, isZero, kernelRadius, sigma);

                maxValue = 0; minValue = 1;
                for (int x = 0; x < blur.length; x++) {
                    for (int z = 0; z < blur[0].length; z++) {
                        maxValue = Math.max(blur[x][z], maxValue);
                        minValue = Math.min(blur[x][z], minValue);
                    }
                }
            }
        }
        if (blur == null) return 0;
        if (pos.blockX() < minX || pos.blockX() > maxX || pos.blockZ() < minZ || pos.blockZ() > maxZ) return 0;
        double val = blur[pos.blockX() - minX][pos.blockZ() - minZ];
        return normalize ? (val - minValue) / (maxValue - minValue) : val;
    }

    @Override
    public double minValue() {
        return normalize ? 0 : minValue;
    }

    @Override
    public double maxValue() {
        return normalize ? 1 : maxValue;
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
