package io.github.orlouge.landmarks.density.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.BoundedFunction;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record Sphere(DensityFunction x, DensityFunction z, DensityFunction y, DensityFunction _radius, DensityFunction inside) implements DensityFunction, BoundedFunction {
    public static final MapCodec<Sphere> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("x").forGetter(Sphere::x),
        DensityFunction.CODEC.fieldOf("z").forGetter(Sphere::z),
        DensityFunction.CODEC.fieldOf("y").forGetter(Sphere::y),
        DensityFunction.CODEC.fieldOf("radius").forGetter(Sphere::_radius),
        DensityFunction.CODEC.fieldOf("inside").forGetter(Sphere::inside)
    ).apply(instance, Sphere::new));
    public static final KeyDispatchDataCodec<Sphere> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return Math.sqrt(Math.pow(pos.blockX() - x.compute(new DensityFunction.SinglePointContext(0, 0, 0)), 2) + Math.pow(pos.blockY() - y.compute(new DensityFunction.SinglePointContext(0, 0, 0)), 2) + Math.pow(pos.blockZ() - z.compute(new DensityFunction.SinglePointContext(0, 0, 0)), 2)) <= _radius.compute(new DensityFunction.SinglePointContext(0, 0, 0)) ? inside.compute(pos) : 0;
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Sphere(visitor.apply(x), visitor.apply(z), visitor.apply(y), visitor.apply(_radius), visitor.apply(inside));
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
        return (int) (y.compute(new DensityFunction.SinglePointContext(0, 0, 0)) - _radius.compute(new DensityFunction.SinglePointContext(0, 0, 0)));
    }

    @Override
    public int maxY() {
        return (int) (y.compute(new DensityFunction.SinglePointContext(0, 0, 0)) + _radius.compute(new DensityFunction.SinglePointContext(0, 0, 0)));
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
