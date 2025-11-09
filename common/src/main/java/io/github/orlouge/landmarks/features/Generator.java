package io.github.orlouge.landmarks.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.LandmarksMod;
import io.github.orlouge.landmarks.utils.RandomProperty;
import io.github.orlouge.landmarks.utils.RandomWrapper;
import io.github.orlouge.landmarks.utils.TopologicalSort;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.*;
import java.util.stream.Collectors;

public record Generator(
    boolean skip,
    boolean abort,
    Map<String, Parameter.Sampler> parameters,
    Palette palette,
    List<ProcessingStep> processingSteps
) {
    public VariantContext generate(StructureWorldAccess world, Random random, VariantContext context) {
        if (skip || abort) return context;
        context = context.withParameters(parameters);
        context = context.withPalette(palette);
        for (ProcessingStep step : processingSteps) {
            step.process(world, random, context);
        }
        return context;
    }

    public record RandomizedGeneratorConfig(
        Optional<RandomProperty<Boolean, VariantContext, VariantContext.Predicate>> skip,
        Optional<RandomProperty<Boolean, VariantContext, VariantContext.Predicate>> abort,
        Optional<RandomProperty<Palette.RandomizedPalette, VariantContext, VariantContext.Predicate>> palette,
        Optional<RandomProperty<Map<String, RandomProperty<Parameter, VariantContext, VariantContext.Predicate>>, VariantContext, VariantContext.Predicate>> userParameters,
        Optional<RandomProperty<List<ProcessingStep.RandomizedProcessingStep>, VariantContext, VariantContext.Predicate>> processingSteps
    ) implements RandomWrapper<Generator, VariantContext>, VariantWithFragment.Fragment<RandomizedGeneratorConfig> {
        public static final MapCodec<RandomizedGeneratorConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            VariantContext.wrappedRandomCodec(Codec.BOOL).optionalFieldOf("skip").forGetter(RandomizedGeneratorConfig::skip),
            VariantContext.wrappedRandomCodec(Codec.BOOL).optionalFieldOf("abort").forGetter(RandomizedGeneratorConfig::abort),
            VariantContext.extendRandomCodec(Palette.RandomizedPalette.CODEC).optionalFieldOf("palette").forGetter(RandomizedGeneratorConfig::palette),
            VariantContext.wrappedRandomCodec(Codec.unboundedMap(Codec.STRING, VariantContext.wrappedRandomCodec(Parameter.CODEC, "value"))).optionalFieldOf("parameters").forGetter(RandomizedGeneratorConfig::userParameters),
            VariantContext.strictWrappedRandomCodec(ProcessingStep.RandomizedProcessingStep.CODEC.codec().listOf(), "steps").optionalFieldOf("processing_sequence").forGetter(RandomizedGeneratorConfig::processingSteps)
        ).apply(i, RandomizedGeneratorConfig::new));
        public static final MapCodec<VariantWithFragment<RandomizedGeneratorConfig>> VARIANT_MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, VariantContext.wrappedRandomCodec(Codec.STRING, "variant_name"))
                .optionalFieldOf("variants", Collections.emptyMap()).forGetter(VariantWithFragment::subVariants),
            Codec.STRING.optionalFieldOf("variant_name").forGetter(VariantWithFragment::nameOverride),
            // TODO move to VariantWithFragment<T> as a function of T
            MAP_CODEC.forGetter(VariantWithFragment::fragment)
        ).apply(i, VariantWithFragment::new));
        /*
        public static final MapCodec<VariantWithFragment.NamedVariant<RandomizedGeneratorConfig>> NAMED_VARIANT_MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("variant_name").forGetter(VariantWithFragment.NamedVariant::name),
            VARIANT_MAP_CODEC.forGetter(VariantWithFragment.NamedVariant::variant)
        ).apply(instance, VariantWithFragment.NamedVariant::new));
        public static final Codec<VariantWithFragment.NamedVariant<RandomizedGeneratorConfig>> NAMED_VARIANT_CODEC = NAMED_VARIANT_MAP_CODEC.codec();
         */
        public static final Codec<RandomProperty.WrappedEntry<VariantWithFragment<RandomizedGeneratorConfig>, VariantContext.Predicate>> RANDOM_ENTRY_CODEC =
            RandomProperty.WrappedEntry.extendCodec(VARIANT_MAP_CODEC, VariantContext.Predicate.CODEC);
        public static final RegistryKey<Registry<RandomProperty.WrappedEntry<VariantWithFragment<RandomizedGeneratorConfig>, VariantContext.Predicate>>> REGISTRY_KEY = RegistryKey.ofRegistry(
            Identifier.of(LandmarksMod.MOD_ID + "_worldgen", "generators"));
        public static Map<Identifier, Map<String, List<RandomProperty.WrappedEntry<VariantWithFragment.NamedVariant<RandomizedGeneratorConfig>, VariantContext.Predicate>>>> childVariants = null;

        public RandomizedGeneratorConfig() {
            this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        public static Map<Identifier, Map<String, List<RandomProperty.WrappedEntry<VariantWithFragment.NamedVariant<RandomizedGeneratorConfig>, VariantContext.Predicate>>>> getChildVariants(DynamicRegistryManager registryManager) {
            if (childVariants == null) {
                childVariants = VariantWithFragment.getChildVariants(registryManager.get(REGISTRY_KEY), new RandomizedGeneratorConfig());
            }
            return childVariants;
        }

        public static Pair<RandomizedGeneratorConfig, VariantContext> getAndMerge(
            List<Identifier> variants, RandomizedGeneratorConfig baseConfig,
            Random random, VariantContext baseContext, DynamicRegistryManager registryManager
        ) throws RandomProperty.NoRandomMatchException {
            return VariantWithFragment.getAndMerge(
                variants, baseConfig, new RandomizedGeneratorConfig(),
                random, baseContext, registryManager, REGISTRY_KEY,
                getChildVariants(registryManager));
        }

        public RandomizedGeneratorConfig merge(RandomizedGeneratorConfig config) {
            return new RandomizedGeneratorConfig(
                this.skip.isPresent() && config.skip.isPresent() ? Optional.of(this.skip.get().merge(config.skip.get())) : this.skip.or(() -> config.skip),
                this.abort.isPresent() && config.abort.isPresent() ? Optional.of(this.abort.get().merge(config.abort.get())) : this.abort.or(() -> config.abort),
                this.palette.isPresent() && config.palette.isPresent()
                    ? Optional.of(this.palette.get().merge(config.palette.get()))
                    : this.palette.or(() -> config.palette),
                this.userParameters.isPresent() && config.userParameters.isPresent()
                    ? Optional.of(this.userParameters.get().merge(config.userParameters.get()))
                    : this.userParameters.or(() -> config.userParameters),
                this.processingSteps.isPresent() && config.processingSteps.isPresent()
                    ? Optional.of(this.processingSteps.get().merge(config.processingSteps.get()))
                    : this.processingSteps.or(() -> config.processingSteps)
            );
        }

        @Override
        public Generator sample(Random random, VariantContext context) throws RandomProperty.NoRandomMatchException {
            DensityFunction.DensityFunctionVisitor visitor = context.getVisitor();
            Map<String, Parameter.Sampler> parameters = new HashMap<>();
            if (this.userParameters.isPresent()) {
                RandomProperty.Sampler<Map<String, RandomProperty<Parameter, VariantContext, VariantContext.Predicate>>> sampler =
                    this.userParameters.get().withoutReplacement(context, false, true).sampler(random);
                Map<String, RandomProperty<Parameter, VariantContext, VariantContext.Predicate>> mergedParameters = new HashMap<>();
                for (;;) {
                    Map<String, RandomProperty<Parameter, VariantContext, VariantContext.Predicate>> parameters2 = sampler.sample();
                    if (parameters2 == null) break;
                    if (parameters2.keySet().stream().anyMatch(parameters.keySet()::contains)) continue;
                    mergedParameters.putAll(parameters2);
                }
                HashMap<String, Set<String>> deps = new HashMap<>();
                for (Map.Entry<String, RandomProperty<Parameter, VariantContext, VariantContext.Predicate>> entry : mergedParameters.entrySet()) {
                    deps.put(entry.getKey(), entry.getValue().entries().stream().flatMap(w ->
                        w.value().referencedParameters().stream()).filter(mergedParameters.keySet()::contains
                    ).collect(Collectors.toSet()));
                }
                List<String> topoParOrder = TopologicalSort.sort(deps.keySet(), deps::get);
                for (String par : topoParOrder) {
                    Parameter.Sampler value = mergedParameters.get(par).sample(random, context).createSampler(visitor);
                    parameters.put(par, value);
                    context = context.withParameters(Map.of(par, value));
                    visitor = context.getVisitor();
                }
            }

            boolean skip = this.skip.isPresent() ? this.skip.get().sample(random, context) : false;
            boolean abort = this.abort.isPresent() ? this.abort.get().sample(random, context): false;
            if (skip || abort) return new Generator(skip, abort, parameters, new Palette(), List.of());

            Palette palette = new Palette();
            if (this.palette.isPresent()) {
                RandomProperty.Sampler<Palette.RandomizedPalette> sampler = this.palette.get().withoutReplacement(context, false, true).sampler(random);
                Palette.RandomizedPalette randomPalette = new Palette.RandomizedPalette(), fallbackPalette = new Palette.RandomizedPalette();
                for (;;) {
                    Palette.RandomizedPalette palette2 = sampler.sample();
                    if (palette2 == null) break;
                    if (palette2.overwritable()) {
                        fallbackPalette = fallbackPalette.merge(palette2);
                    }
                    else if (randomPalette.canCombine(palette2)) {
                        randomPalette = randomPalette.merge(palette2);
                    }
                }
                palette = fallbackPalette.merge(randomPalette).sample(random, context);
            }
            context = context.withPalette(palette);

            LinkedHashMap<Integer, ProcessingStep> steps = new LinkedHashMap<>();
            if (this.processingSteps.isPresent()) {
                RandomProperty.Sampler<List<ProcessingStep.RandomizedProcessingStep>> sampler = this.processingSteps.get().withoutReplacement(context, false, false).sampler(random);
                for (;;) {
                    List<ProcessingStep.RandomizedProcessingStep> steps2 = sampler.sample();
                    if (steps2 == null) break;
                    LinkedHashMap<Integer, ProcessingStep.RandomizedProcessingStep> steps2Map = new LinkedHashMap<>();
                    int lastIndex = -1;
                    for (ProcessingStep.RandomizedProcessingStep step : steps2) {
                        int index = step.index().orElse(lastIndex + 1);
                        lastIndex = index;
                        steps2Map.put(index, step);
                    }
                    if (steps2Map.keySet().stream().anyMatch(steps.keySet()::contains)) continue;
                    for (Map.Entry<Integer, ProcessingStep.RandomizedProcessingStep> entry : steps2Map.entrySet()) {
                        steps.put(entry.getKey(), entry.getValue().sample(random, context));
                    }
                }
            }

            return new Generator(false, false, parameters, palette, steps.values().stream().toList());
        }

        @Override
        public RandomizedGeneratorConfig addVariantCondition(String variantType, String variantName) {
            return new RandomizedGeneratorConfig(
                skip.map(skip -> skip.mapPredicates(p -> p.addVariantCondition(variantType, variantName))),
                abort.map(abort -> abort.mapPredicates(p -> p.addVariantCondition(variantType, variantName))),
                palette.map(palette -> palette.mapPredicates(p -> p.addVariantCondition(variantType, variantName))),
                userParameters.map(pars -> pars.mapPredicates(p -> p.addVariantCondition(variantType, variantName))),
                processingSteps.map(steps -> steps.mapPredicates(p -> p.addVariantCondition(variantType, variantName)))
            );
        }
    }
}
