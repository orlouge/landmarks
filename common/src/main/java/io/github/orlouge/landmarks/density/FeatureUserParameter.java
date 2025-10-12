package io.github.orlouge.landmarks.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class FeatureUserParameter implements DensityFunction.Base {
    public static final MapCodec<FeatureUserParameter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("parameter").forGetter(d -> d.parameter)
    ).apply(instance, FeatureUserParameter::new));
    public static final CodecHolder<FeatureUserParameter> CODEC_HOLDER = CodecHolder.of(CODEC);
    private double value = Double.NaN, min = 0, max = 0;
    public final String parameter;

    public FeatureUserParameter(String parameter) {
        this.parameter = parameter;
    }

    public FeatureUserParameter create(double value) {
        FeatureUserParameter copy = new FeatureUserParameter(parameter);
        copy.value = value;
        copy.min = value;
        copy.max = value;
        return copy;
    }

    @Override
    public double sample(NoisePos pos) {
        if (Double.isNaN(value)) System.out.println("Attempted to read unset user parameter: " + this.parameter);
        //System.out.println(this.parameter + " = " + input);
        return value;
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
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
