package io.github.orlouge.landmarks.features;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.*;
import io.github.orlouge.landmarks.density.algorithms.*;
import io.github.orlouge.landmarks.density.feature.*;
import io.github.orlouge.landmarks.density.feature.constants.*;
import io.github.orlouge.landmarks.utils.RandomProperty;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

import java.util.*;

public record VariantContext(
    StructureWorldAccess world,
    long seed,
    RegistryEntry<Biome> biome,
    Map<String, String> variant,
    BlockPos origin,
    BlockPos minPos,
    BlockPos maxPos,
    Map<String, Parameter.Sampler> userParameters,
    Map<String, Object> functionCache,
    Palette palette
) {
    public static final Predicate DEFAULT_CONTEXT_PREDICATE = new Predicate();

    public static <T> Codec<RandomProperty<T, VariantContext, Predicate>> strictWrappedRandomCodec(Codec<T> entryCodec, String valueKey) {
        return RandomProperty.strictWrappedCodec(entryCodec, Predicate.CODEC, DEFAULT_CONTEXT_PREDICATE, valueKey);
    }

    public static <T> Codec<RandomProperty<T, VariantContext, Predicate>> strictWrappedRandomCodec(Codec<T> entryCodec) {
        return RandomProperty.strictWrappedCodec(entryCodec, Predicate.CODEC, DEFAULT_CONTEXT_PREDICATE);
    }

    public static <T> Codec<RandomProperty<T, VariantContext, Predicate>> extendRandomCodec(MapCodec<T> entryCodec) {
        return RandomProperty.extendCodec(entryCodec, Predicate.CODEC, DEFAULT_CONTEXT_PREDICATE);
    }

    public static <T> RandomProperty<T, VariantContext, Predicate> defaultRandomProperty(T value) {
        return RandomProperty.singleton(value, DEFAULT_CONTEXT_PREDICATE);
    }

    public static <T> Codec<RandomProperty<T, VariantContext, Predicate>> wrappedRandomCodec(Codec<T> entryCodec, String valueKey) {
        return RandomProperty.wrappedCodec(entryCodec, Predicate.CODEC, DEFAULT_CONTEXT_PREDICATE, valueKey);
    }

    public static <T> Codec<RandomProperty<T, VariantContext, Predicate>> wrappedRandomCodec(Codec<T> entryCodec) {
        return RandomProperty.wrappedCodec(entryCodec, Predicate.CODEC, DEFAULT_CONTEXT_PREDICATE);
    }

    public VariantContext withParameters(Map<String, Parameter.Sampler> localParameters) {
        Map<String, Parameter.Sampler> parameters = new HashMap<>(userParameters);
        parameters.putAll(localParameters);
        return new VariantContext(world, seed, biome, variant, origin, minPos, maxPos, parameters, functionCache, palette);
    }

    public VariantContext withVariants(Map<String, String> newVariants) {
        Map<String, String> variants = new HashMap<>(this.variant);
        variants.putAll(newVariants);
        return new VariantContext(world, seed, biome, variants, origin, minPos, maxPos, userParameters, functionCache, palette);
    }

    public VariantContext withPalette(Palette palette) {
        return new VariantContext(world, seed, biome, variant, origin, minPos, maxPos, userParameters, functionCache, this.palette.merge(palette));
    }

    public VariantContext withBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new VariantContext(world, seed, biome, variant, origin, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), userParameters, functionCache, palette);
    }

    public DensityFunction.DensityFunctionVisitor getVisitor() {
        int minX = minPos.getX(), maxX = maxPos.getX(), minY = minPos.getY(), maxY = maxPos.getY(), minZ = minPos.getZ(), maxZ = maxPos.getZ();
        DensityFunction.DensityFunctionVisitor visitor = function -> {
            {
                if (function instanceof Noise2D noise && noise.key() != null) {
                    if (functionCache.containsKey(noise.key())) {
                        return noise.setCache(functionCache.get(noise.key()));
                    } else {
                        Noise2D noiseCached = noise.create(noise.global ? world.getSeed() : seed, minX, maxX, minZ, maxZ);
                        functionCache.put(noiseCached.key(), noiseCached.getCache());
                        return noiseCached;
                    }
                } else if (function instanceof FeatureRandomNumber random) {
                    if (functionCache.containsKey(random.key())) {
                        return random.setCache(functionCache.get(random.key()));
                    } else {
                        FeatureRandomNumber noiseCached = random.create(seed);
                        functionCache.put(noiseCached.key(), noiseCached.getCache());
                        return noiseCached;
                    }
                } else if (function instanceof FeatureRandomGrid random) {
                    if (functionCache.containsKey(random.key())) {
                        return random.setCache(functionCache.get(random.key()));
                    } else {
                        FeatureRandomGrid noiseCached = random.create(seed);
                        functionCache.put(noiseCached.key(), noiseCached.getCache());
                        return noiseCached;
                    }
                } else if (function instanceof FunctionWithCache.Simple cached && cached.key() != null) {
                    Object cache = functionCache.computeIfAbsent(cached.key(), k -> cached.createCache(minX, maxX, minY, maxY, minZ, maxZ));
                    return cached.setCache(cache);
                } else if (function instanceof FunctionWithCache.Simple cached && cached.key() == null) {
                    return cached.setCache(cached.createCache(minX, maxX, minY, maxY, minZ, maxZ));
                } else if (function instanceof  Noise3D noise) {
                    return noise.create(seed);
                } else if (function instanceof  FeatureBlockMatches block) {
                    return block.create(world);
                } else if (function instanceof  FeatureBiomeMatches block) {
                    return block.create(world);
                } else if (function instanceof  FeatureUserParameter params && userParameters.containsKey(params.parameter)) {
                    return params.create(userParameters.get(params.parameter));
                } else if (function instanceof FeatureMinY param) {
                    return param.create(minY);
                } else if (function instanceof  FeatureMaxY param) {
                    return param.create(maxY);
                } else if (function instanceof FeatureOriginX param) {
                    return param.create(origin.getX());
                } else if (function instanceof FeatureOriginY param) {
                    return param.create(origin.getY());
                } else if (function instanceof FeatureOriginZ param) {
                    return param.create(origin.getZ());
                } else if (function instanceof FeatureMinX param) {
                    return param.create(minX);
                } else if (function instanceof FeatureMinZ param) {
                    return param.create(minZ);
                } else if (function instanceof FeatureMaxX param) {
                    return param.create(maxX);
                } else if (function instanceof FeatureMaxZ param) {
                    return param.create(maxZ);
                } else {
                    return function;
                }
            }
        };
        return function -> {
            if (function instanceof DensityFunctionTypes.RegistryEntryHolder holder) {
                return visitor.apply(holder.function().value());
            } else {
                return visitor.apply(function);
            }
        };
    }

    public record Predicate(Optional<RegistryEntryList<Biome>> biomes,
                            Optional<Map<String, HashSet<String>>> variantsAny,
                            Optional<Map<String, HashSet<String>>> variantsNone,
                            Optional<RegistryEntryList<Block>> originIs,
                            Optional<List<String>> conditions) implements RandomProperty.ContextPredicate<VariantContext> {
        public static final MapCodec<Predicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.entryList(RegistryKeys.BIOME).optionalFieldOf("biome").forGetter(Predicate::biomes),
            Codec.unboundedMap(Codec.STRING, Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(e -> new HashSet<>(e.map(List::of, s -> s)), set -> Either.right(set.stream().toList()))).optionalFieldOf("has_variants").forGetter(Predicate::variantsAny),
            Codec.unboundedMap(Codec.STRING, Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(e -> new HashSet<>(e.map(List::of, s -> s)), set -> Either.right(set.stream().toList()))).optionalFieldOf("hasnt_variants").forGetter(Predicate::variantsAny),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("origin").forGetter(Predicate::originIs),
            Codec.STRING.listOf().optionalFieldOf("conditions").forGetter(Predicate::conditions)
        ).apply(instance, Predicate::new));

        public Predicate() {
            this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        public Predicate addVariantCondition(String variantType, String variantName) {
            Map<String, HashSet<String>> variantsAny = this.variantsAny.map(HashMap::new).orElse(new HashMap<>());
            variantsAny.put(variantType, new HashSet<>(List.of(variantName)));
            return new Predicate(biomes, Optional.of(variantsAny), variantsNone, originIs, conditions);
        }

        @Override
        public boolean isDefault() {
            return biomes.isEmpty() && variantsAny.isEmpty() && variantsNone.isEmpty();
        }

        @Override
        public boolean test(VariantContext context) {
            return biomes.map(list -> list.contains(context.biome)).orElse(true) &&
                variantsAny.map(variantSets -> {
                    for (Map.Entry<String, HashSet<String>> entry : variantSets.entrySet()) {
                        HashSet<String> variantSetCopy = new HashSet<>(entry.getValue());
                        if (!variantSetCopy.contains(context.variant.getOrDefault(entry.getKey(), ""))) return false;
                    }
                    return true;
                }).orElse(true) &&
                variantsNone.map(variantSets -> {
                    for (Map.Entry<String, HashSet<String>> entry : variantSets.entrySet()) {
                        HashSet<String> variantSetCopy = new HashSet<>(entry.getValue());
                        if (variantSetCopy.contains(context.variant.getOrDefault(entry.getKey(), ""))) return false;
                    }
                    return true;
                }).orElse(true) &&
                originIs.map(o ->
                    o.contains(context.world.getBlockState(context.origin.add(0, -1, 0)).getRegistryEntry())
                ).orElse(true) &&
                conditions.map(c -> c.stream().allMatch(s ->
                        Parameter.Condition.parse(s, context.userParameters).test(new DensityFunction.UnblendedNoisePos(0, 0, 0)))
                    ).orElse(true);
        }
    }
}
