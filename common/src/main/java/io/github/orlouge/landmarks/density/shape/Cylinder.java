package io.github.orlouge.landmarks.density.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.BoundedFunction;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public record Cylinder(DensityFunction x, DensityFunction z, DensityFunction _minY, DensityFunction _maxY, DensityFunction _radius, DensityFunction inside) implements DensityFunction, BoundedFunction {
    public static final MapCodec<Cylinder> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.FUNCTION_CODEC.fieldOf("x").forGetter(Cylinder::x),
        DensityFunction.FUNCTION_CODEC.fieldOf("z").forGetter(Cylinder::z),
        DensityFunction.FUNCTION_CODEC.fieldOf("min_y").forGetter(Cylinder::_minY),
        DensityFunction.FUNCTION_CODEC.fieldOf("max_y").forGetter(Cylinder::_maxY),
        DensityFunction.FUNCTION_CODEC.fieldOf("radius").forGetter(Cylinder::_radius),
        DensityFunction.FUNCTION_CODEC.fieldOf("inside").forGetter(Cylinder::inside)
    ).apply(instance, Cylinder::new));
    public static final CodecHolder<Cylinder> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return Math.sqrt(Math.pow(pos.blockX() - x.sample(new UnblendedNoisePos(0, 0, 0)), 2) + Math.pow(pos.blockZ() - z.sample(new UnblendedNoisePos(0, 0, 0)), 2)) <= _radius.sample(new UnblendedNoisePos(0, 0, 0)) && pos.blockY() >= _minY.sample(new UnblendedNoisePos(0, 0, 0)) && pos.blockY() <= _maxY.sample(new UnblendedNoisePos(0, 0, 0)) ? inside.sample(pos) : 0;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new Cylinder(x.apply(visitor), z.apply(visitor), _minY.apply(visitor), _maxY.apply(visitor), _radius.apply(visitor), inside.apply(visitor)));
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
        return (int) (x.sample(new UnblendedNoisePos(0, 0, 0)) - _radius.sample(new UnblendedNoisePos(0, 0, 0)));
    }

    @Override
    public int maxX() {
        return (int) (x.sample(new UnblendedNoisePos(0, 0, 0)) + _radius.sample(new UnblendedNoisePos(0, 0, 0)));
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
        return (int) (z.sample(new UnblendedNoisePos(0, 0, 0)) - _radius.sample(new UnblendedNoisePos(0, 0, 0)));
    }

    @Override
    public int maxZ() {
        return (int) (z.sample(new UnblendedNoisePos(0, 0, 0)) + _radius.sample(new UnblendedNoisePos(0, 0, 0)));
    }
}
