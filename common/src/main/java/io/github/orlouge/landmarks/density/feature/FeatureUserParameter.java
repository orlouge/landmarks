package io.github.orlouge.landmarks.density.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.features.Parameter;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class FeatureUserParameter implements DensityFunction.SimpleFunction {
    public static final MapCodec<FeatureUserParameter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("parameter").forGetter(d -> d.parameter)
    ).apply(instance, FeatureUserParameter::new));
    public static final KeyDispatchDataCodec<FeatureUserParameter> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);
    private Parameter.Sampler sampler = null;
    private double min = 0, max = 0;
    public final String parameter;

    public FeatureUserParameter(String parameter) {
        this.parameter = parameter;
    }

    public FeatureUserParameter create(Parameter.Sampler par) {
        FeatureUserParameter copy = new FeatureUserParameter(parameter);
        copy.sampler = par;
        copy.min = par.min();
        copy.max = par.max();
        return copy;
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        if (sampler == null) throw new RuntimeException("Attempted to read unset user parameter: " + this.parameter);
        //System.out.println(this.parameter + " = " + input);
        return sampler.sample(pos);
    }

    @Override
    public double minValue() {
        return min;
    }

    @Override
    public double maxValue() {
        return max;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
