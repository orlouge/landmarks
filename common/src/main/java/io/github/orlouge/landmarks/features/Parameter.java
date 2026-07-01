package io.github.orlouge.landmarks.features;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.orlouge.landmarks.density.BoundedFunction;
import io.github.orlouge.landmarks.density.feature.FeatureUserParameter;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import java.util.*;

public abstract class Parameter {
    public abstract Sampler createSampler(DensityFunction.Visitor visitor);
    public abstract Collection<String> referencedParameters();

    public interface Sampler {
        double sample(DensityFunction.FunctionContext pos);
        Optional<BoundedFunction> bounds();

        double min();
        double max();
    }

    public static final Codec<Parameter> CODEC = Codec.either(Codec.DOUBLE, DensityFunction.CODEC).xmap(
        either -> either.map(Constant::new, Density::new),
        par -> par instanceof Constant cons ? Either.left(cons.value) : Either.right(((Density) par).function)
    );

    public static final Codec<Either<String, Parameter>> CODEC_NAMED = Codec.either(Codec.STRING.flatXmap(
        s -> s.contains(":") ? DataResult.error(() -> "Parameter names cannot contain ':'") : DataResult.success(s),
        DataResult::success
    ), CODEC);

    public static class Constant extends Parameter {
        public final double value;

        public Constant(double value) {
            this.value = value;
        }

        @Override
        public Sampler createSampler(DensityFunction.Visitor visitor) {
            return new Sampler() {
                @Override
                public double sample(DensityFunction.FunctionContext pos) {
                    return value;
                }

                @Override
                public Optional<BoundedFunction> bounds() {
                    return Optional.empty();
                }

                @Override
                public double min() {
                    return value;
                }

                @Override
                public double max() {
                    return value;
                }
            };
        }

        @Override
        public Collection<String> referencedParameters() {
            return List.of();
        }
    }

    public static class Density extends Parameter {
        public final DensityFunction function;

        public Density(DensityFunction function) {
            this.function = function;
        }

        @Override
        public Sampler createSampler(DensityFunction.Visitor visitor) {
            DensityFunction visitedFunction = this.function.mapAll(visitor);
            return new Sampler() {
                @Override
                public double sample(DensityFunction.FunctionContext pos) {
                    return visitedFunction.compute(pos);
                }

                @Override
                public Optional<BoundedFunction> bounds() {
                    return visitedFunction instanceof BoundedFunction bounded ? Optional.of(bounded) : Optional.empty();
                }

                @Override
                public double min() {
                    return visitedFunction.minValue();
                }

                @Override
                public double max() {
                    return visitedFunction.maxValue();
                }
            };
        }

        @Override
        public Collection<String> referencedParameters() {
            HashSet<String> deps = new HashSet<>();
            this.function.mapAll(fun -> {
                if (fun instanceof FeatureUserParameter par) deps.add(par.parameter);
                if (fun instanceof DensityFunctions.HolderHolder reg && reg.function().value() instanceof FeatureUserParameter par) deps.add(par.parameter);
                return fun;
            });
            return deps;
        }
    }

    public record Condition(List<Sampler> operands, List<Operator> operators) {
        public static Condition parse(String condition, Map<String, Sampler> sampler) {
            String[] split = condition.replaceAll(" +", "").splitWithDelimiters("([><!=]=?|[-+])", 0);

            List<Sampler> operands = new ArrayList<>();
            for (int i = 0; i < split.length; i += 2) {
                String variable = split[i];
                if (variable.isEmpty()) {
                    operands.add(new Constant(0).createSampler(f -> f));
                } else if (sampler.containsKey(variable)) {
                    operands.add(sampler.get(variable));
                } else if (Character.isDigit(variable.charAt(0))) {
                    operands.add(new Constant(Double.parseDouble(variable)).createSampler(f -> f));
                } else {
                    throw new RuntimeException("Invalid condition operand: " + variable);
                }
            }

            List<Operator> operators = new ArrayList<>();
            for (int i = 1; i < split.length; i += 2) {
                String operator = split[i];
                operators.add(switch (operator) {
                    case "==" -> Operator.EQ;
                    case "!=" -> Operator.NE;
                    case ">" -> Operator.GT;
                    case ">=" -> Operator.GE;
                    case "<" -> Operator.LT;
                    case "<=" -> Operator.LE;
                    case "+" -> Operator.ADD;
                    case "-" -> Operator.SUB;
                    case "*" -> Operator.MUL;
                    case "/" -> Operator.DIV;
                    default -> throw new IllegalStateException("Unexpected input: " + operator);
                });
            }

            return new Condition(operands, operators);
        }

        public boolean test(DensityFunction.FunctionContext pos) {
            if (operands.size() > 1) {
                List<Double> conditionOperands = new ArrayList<>();
                List<Operator> conditionOperators = new ArrayList<>();
                double accValue = operands.getFirst().sample(pos);
                int operatorIdx = 0;
                for (int i = 1; i < operands.size(); i++) {
                    double value = operands.get(i).sample(pos);
                    Operator operator = operators.get(operatorIdx);
                    switch (operator) {
                        case ADD -> accValue += value;
                        case SUB -> accValue -= value;
                        case MUL -> accValue *= value;
                        case DIV -> accValue /= value;
                        default -> {
                            conditionOperands.add(accValue);
                            conditionOperators.add(operator);
                            accValue = value;
                        }
                    }
                    operatorIdx += 1;
                }
                conditionOperands.add(accValue);

                double lastValue = conditionOperands.getFirst();
                operatorIdx = 0;
                for (int i = 1; i < conditionOperands.size(); i++) {
                    double value = conditionOperands.get(i);
                    Operator operator = conditionOperators.get(operatorIdx);
                    if (switch (operator) {
                        case GT -> lastValue <= value;
                        case GE -> lastValue < value;
                        case LT -> lastValue >= value;
                        case LE -> lastValue > value;
                        case EQ -> lastValue != value;
                        case NE -> lastValue == value;
                        default -> false;
                    }) return false;
                    operatorIdx += 1;
                    lastValue = value;
                }
            }

            return true;
        }

        public enum Operator {
            GT, GE, LT, LE, EQ, NE, ADD, SUB, MUL, DIV
        }
    }
}
