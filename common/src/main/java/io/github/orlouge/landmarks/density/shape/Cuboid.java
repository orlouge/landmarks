package io.github.orlouge.landmarks.density.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.BoundedFunction;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record Cuboid(DensityFunction _minX, DensityFunction _maxX, DensityFunction _minY, DensityFunction _maxY, DensityFunction _minZ, DensityFunction _maxZ, DensityFunction inside) implements DensityFunction, BoundedFunction {
    public static final MapCodec<Cuboid> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("min_x").forGetter(Cuboid::_minX),
        DensityFunction.CODEC.fieldOf("max_x").forGetter(Cuboid::_maxX),
        DensityFunction.CODEC.fieldOf("min_y").forGetter(Cuboid::_minY),
        DensityFunction.CODEC.fieldOf("max_y").forGetter(Cuboid::_maxY),
        DensityFunction.CODEC.fieldOf("min_z").forGetter(Cuboid::_minZ),
        DensityFunction.CODEC.fieldOf("max_z").forGetter(Cuboid::_maxZ),
        DensityFunction.CODEC.fieldOf("inside").forGetter(Cuboid::inside)
    ).apply(instance, Cuboid::new));
    public static final KeyDispatchDataCodec<Cuboid> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        return pos.blockX() >= _minX.compute(new DensityFunction.SinglePointContext(0, 0, 0)) && pos.blockX() <= _maxX.compute(new DensityFunction.SinglePointContext(0, 0, 0)) && pos.blockY() >= _minY.compute(new DensityFunction.SinglePointContext(0, 0, 0)) && pos.blockY() <= _maxY.compute(new DensityFunction.SinglePointContext(0, 0, 0)) && pos.blockZ() >= _minZ.compute(new DensityFunction.SinglePointContext(0, 0, 0)) && pos.blockZ() <= _maxZ.compute(new DensityFunction.SinglePointContext(0, 0, 0)) ? inside.compute(pos) : 0;
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
        return new Cuboid(visitor.apply(_minX), visitor.apply(_maxX), visitor.apply(_minY), visitor.apply(_maxY), visitor.apply(_minZ), visitor.apply(_maxZ), visitor.apply(inside));
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
        return (int) _minX.compute(new DensityFunction.SinglePointContext(0, 0, 0));
    }

    @Override
    public int maxX() {
        return (int) _maxX.compute(new DensityFunction.SinglePointContext(0, 0, 0));
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
        return (int) _minZ.compute(new DensityFunction.SinglePointContext(0, 0, 0));
    }

    @Override
    public int maxZ() {
        return (int) _maxZ.compute(new DensityFunction.SinglePointContext(0, 0, 0));
    }
}
