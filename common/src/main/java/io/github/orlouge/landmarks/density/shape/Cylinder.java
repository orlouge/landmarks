package io.github.orlouge.landmarks.density.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.BoundedFunction;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record Cylinder(DensityFunction x, DensityFunction z, DensityFunction _minY, DensityFunction _maxY, DensityFunction _radius, DensityFunction inside) implements DensityFunction, BoundedFunction {
    public static final MapCodec<Cylinder> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("x").forGetter(Cylinder::x),
        DensityFunction.CODEC.fieldOf("z").forGetter(Cylinder::z),
        DensityFunction.CODEC.fieldOf("min_y").forGetter(Cylinder::_minY),
        DensityFunction.CODEC.fieldOf("max_y").forGetter(Cylinder::_maxY),
        DensityFunction.CODEC.fieldOf("radius").forGetter(Cylinder::_radius),
        DensityFunction.CODEC.fieldOf("inside").forGetter(Cylinder::inside)
    ).apply(instance, Cylinder::new));
    public static final KeyDispatchDataCodec<Cylinder> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return Math.sqrt(Math.pow(pos.blockX() - x.compute(new DensityFunction.SinglePointContext(0, 0, 0)), 2) + Math.pow(pos.blockZ() - z.compute(new DensityFunction.SinglePointContext(0, 0, 0)), 2)) <= _radius.compute(new DensityFunction.SinglePointContext(0, 0, 0)) && pos.blockY() >= _minY.compute(new DensityFunction.SinglePointContext(0, 0, 0)) && pos.blockY() <= _maxY.compute(new DensityFunction.SinglePointContext(0, 0, 0)) ? inside.compute(pos) : 0;
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Cylinder(visitor.apply(x), visitor.apply(z), visitor.apply(_minY), visitor.apply(_maxY), visitor.apply(_radius), visitor.apply(inside));
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
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }

    @Override
    public int minX() {
        return (int) (x.compute(new DensityFunction.SinglePointContext(0, 0, 0)) - _radius.compute(new DensityFunction.SinglePointContext(0, 0, 0)));
    }

    @Override
    public int maxX() {
        return (int) (x.compute(new DensityFunction.SinglePointContext(0, 0, 0)) + _radius.compute(new DensityFunction.SinglePointContext(0, 0, 0)));
    }

    @Override
    public int minY() {
        return (int) _minY.compute(new DensityFunction.SinglePointContext(0, 0, 0));
    }

    @Override
    public int maxY() {
        return (int) _maxY.compute(new DensityFunction.SinglePointContext(0, 0, 0));
    }

    @Override
    public int minZ() {
        return (int) (z.compute(new DensityFunction.SinglePointContext(0, 0, 0)) - _radius.compute(new DensityFunction.SinglePointContext(0, 0, 0)));
    }

    @Override
    public int maxZ() {
        return (int) (z.compute(new DensityFunction.SinglePointContext(0, 0, 0)) + _radius.compute(new DensityFunction.SinglePointContext(0, 0, 0)));
    }
}
