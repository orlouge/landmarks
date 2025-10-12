package io.github.orlouge.landmarks.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.utils.ChamferTransform;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class BorderChamferDistance implements DensityFunction.Base {
    private static final MapCodec<BorderChamferDistance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("normalize", true).forGetter(d -> d.normalize)
    ).apply(instance, BorderChamferDistance::new));
    public static final CodecHolder<BorderChamferDistance> CODEC_HOLDER = CodecHolder.of(CODEC);

    public final boolean normalize;
    private double[][] dist = null;
    private boolean[][] isTarget = null;
    private int minX, minZ, maxX, maxZ;
    public double maxDist = 0;

    public BorderChamferDistance(boolean normalize) {
        this.normalize = normalize;
    }

    public BorderChamferDistance create(boolean[][] isTarget, int minX, int maxX, int minZ, int maxZ) {
        BorderChamferDistance copy = new BorderChamferDistance(normalize);
        copy.isTarget = isTarget;
        copy.minX = minX;
        copy.maxX = maxX;
        copy.minZ = minZ;
        copy.maxZ = maxZ;
        return copy;
    }

    @Override
    public double sample(NoisePos pos) {
        if (dist == null) {
            if (isTarget != null) {
                dist = ChamferTransform.distanceTransform(isTarget);
                maxDist = 0;
                for (int x = 0; x < dist.length; x++) {
                    for (int z = 0; z < dist[0].length; z++) {
                        dist[x][z] /= 2.0;
                        maxDist = Math.max(dist[x][z], maxDist);
                    }
                }
            }
        }
        if (dist == null) return 0;
        if (pos.blockX() < minX || pos.blockX() > maxX || pos.blockZ() < minZ || pos.blockZ() > maxZ) return 0;
        double val = dist[pos.blockX() - minX][pos.blockZ() - minZ];
        return normalize ? val / maxDist : val;
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return normalize ? 1 : maxDist;
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
