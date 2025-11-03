package io.github.orlouge.landmarks.density.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.BoundedFunction;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record Cuboid(DensityFunction _minX, DensityFunction _maxX, DensityFunction _minY, DensityFunction _maxY, DensityFunction _minZ, DensityFunction _maxZ, DensityFunction inside) implements DensityFunction, BoundedFunction {
    public static final MapCodec<Cuboid> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("min_x").forGetter(Cuboid::_minX),
        DensityFunction.FUNCTION_CODEC.fieldOf("max_x").forGetter(Cuboid::_maxX),
        DensityFunction.FUNCTION_CODEC.fieldOf("min_y").forGetter(Cuboid::_minY),
        DensityFunction.FUNCTION_CODEC.fieldOf("max_y").forGetter(Cuboid::_maxY),
        DensityFunction.FUNCTION_CODEC.fieldOf("min_z").forGetter(Cuboid::_minZ),
        DensityFunction.FUNCTION_CODEC.fieldOf("max_z").forGetter(Cuboid::_maxZ),
        DensityFunction.FUNCTION_CODEC.fieldOf("inside").forGetter(Cuboid::inside)
    ).apply(instance, Cuboid::new));
    public static final CodecHolder<Cuboid> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return pos.blockX() >= _minX.sample(new UnblendedNoisePos(0, 0, 0)) && pos.blockX() <= _maxX.sample(new UnblendedNoisePos(0, 0, 0)) && pos.blockY() >= _minY.sample(new UnblendedNoisePos(0, 0, 0)) && pos.blockY() <= _maxY.sample(new UnblendedNoisePos(0, 0, 0)) && pos.blockZ() >= _minZ.sample(new UnblendedNoisePos(0, 0, 0)) && pos.blockZ() <= _maxZ.sample(new UnblendedNoisePos(0, 0, 0)) ? inside.sample(pos) : 0;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Cuboid(_minX.apply(visitor), _maxX.apply(visitor), _minY.apply(visitor), _maxY.apply(visitor), _minZ.apply(visitor), _maxZ.apply(visitor), inside.apply(visitor)));
    }

    @Override
    public double minValue() {
        return Math.min(0, inside.minValue());
    }

    @Override
    public double maxValue() {
        return Math.max(0, inside.maxValue());
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }

    @Override
    public int minX() {
        return (int) _minX.sample(new UnblendedNoisePos(0, 0, 0));
    }

    @Override
    public int maxX() {
        return (int) _maxX.sample(new UnblendedNoisePos(0, 0, 0));
    }

    @Override
    public int minY() {
        return (int) _minY.sample(new UnblendedNoisePos(0, 0, 0));
    }

    @Override
    public int maxY() {
        return (int) _maxY.sample(new UnblendedNoisePos(0, 0, 0));
    }

    @Override
    public int minZ() {
        return (int) _minZ.sample(new UnblendedNoisePos(0, 0, 0));
    }

    @Override
    public int maxZ() {
        return (int) _maxZ.sample(new UnblendedNoisePos(0, 0, 0));
    }
}
