package io.github.orlouge.landmarks.features;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.LandmarksMod;
import io.github.orlouge.landmarks.generation.BlockTemplate;
import io.github.orlouge.landmarks.utils.MaxDensitySquare;
import io.github.orlouge.landmarks.utils.RandomProperty;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Properties;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.*;
import java.util.stream.IntStream;

public class SurfaceNoiseFeature extends Feature<SurfaceNoiseFeature.Config> {
    public SurfaceNoiseFeature(Codec<Config> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<Config> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin(), center = origin;
        BlockState originBlock = world.getBlockState(origin.add(0, -1, 0));
        RegistryEntry<Biome> biome = context.getWorld().getBiome(origin);
        ChunkPos originChunk = new ChunkPos(origin);
        long seed = random.nextLong();
        boolean debug = false;
        //long startTime = System.currentTimeMillis();

        try {
            InstanceConfig config = context.getConfig().instances.sample(random, new VariantContext(biome, Collections.emptySet(), originBlock));
            VariantContext variantContext = new VariantContext(biome, new HashSet<>(), originBlock);
            for (RandomProperty<String, VariantContext, VariantContext.Predicate> variant : config.variant) {
                try {
                    variantContext.variant.add(variant.sample(random, variantContext));
                } catch (RandomProperty.NoRandomMatchException ignored) {}
            }

            if (variantContext.variant.contains("debug")) {
                debug = true;
                System.out.println("#################################");
                System.out.println("Variants: " + String.join(", ", variantContext.variant));
                System.out.println("Biome: " + variantContext.biome.getIdAsString());
                System.out.println("Origin: " + variantContext.origin.getBlock().getRegistryEntry().getIdAsString() + " at " + origin);
            }

            if (variantContext.variant.contains("abort")) return false;

            int maxHeight = randomBetween(random, config.minHeight.sample(random, variantContext), config.maxHeight.sample(random, variantContext));
            maxHeight = Math.min(maxHeight, world.getTopY() - origin.getY());
            int maxDepth = randomBetween(random, config.minDepth.sample(random, variantContext), config.maxDepth.sample(random, variantContext));
            maxDepth = Math.min(maxDepth, origin.getY() - world.getBottomY() + 1);
            int maxWidth = randomBetween(random, config.minWidth.sample(random, variantContext), config.maxWidth.sample(random, variantContext));
            int requiredFreeSpace = config.minSurface.sample(random, variantContext);
            RegistryEntryList<Block> freeSpaceAbove = config.usableSpaceAbove.sample(random, variantContext);
            RegistryEntryList<Block> freeSpaceBelow = config.usableSpaceBelow.sample(random, variantContext);
            Palette randomPalette = config.palette.sample(random, variantContext);
            Map<String, BlockTemplate> palette = randomPalette.sampleAll(random, variantContext);
            Optional<RegistryEntryList<Block>> allowedSurfaceCopy = randomPalette.allowedSurfaceCopy.isPresent() ?
                Optional.of(randomPalette.allowedSurfaceCopy.get().sample(random, variantContext)) : Optional.empty();
            boolean circularBorder = config.circularBorder.sample(random, variantContext);

            int scanWidth = config.searchAround ? 96 : maxWidth;
            int minZ = Math.max((originChunk.z - 1) * 16, center.getZ() - scanWidth / 2), maxZ = Math.min((originChunk.z + 1) * 16 - 1, center.getZ() + scanWidth / 2);
            int minX = Math.max((originChunk.x - 1) * 16, center.getX() - scanWidth / 2), maxX = Math.min((originChunk.x + 1) * 16 - 1, center.getX() + scanWidth / 2);
            int xExt = maxX - minX + 1, zExt = maxZ - minZ + 1, radius = Math.min(xExt, zExt) / 2, sqRadius = radius * radius;
            int middleX = minX + xExt / 2, middleZ = minZ + zExt / 2;
            boolean[][] cantPlace = new boolean[scanWidth + 3][scanWidth + 3];
            int freeSpace = 0;
            for (int x = 0; x < cantPlace.length; x++) {
                for (int z = 0; z < cantPlace[0].length; z++) {
                    cantPlace[x][z] = true;
                }
            }
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, center.getY(), z);
                    if (!config.searchAround && circularBorder && (x - middleX) * (x - middleX) + (z - middleZ) * (z - middleZ) > sqRadius) continue;
                    if ((
                           IntStream.range(0, maxHeight + 1).allMatch(off -> (world.getBlockState(pos.add(0, off, 0))).isIn(freeSpaceAbove))
                        && IntStream.range(1, maxDepth + 1).allMatch(off -> (world.getBlockState(pos.add(0, -off, 0))).isIn(freeSpaceBelow)))) {
                        cantPlace[x - minX + 1][z - minZ + 1] = false;
                        freeSpace += 1;
                    }
                }
            }

            if (config.searchAround) {
                int originalMaxWidth = maxWidth;
                MaxDensitySquare.Result bestSquare = MaxDensitySquare.findDenseSquare(
                    cantPlace, 100 * (1 + maxWidth), random,
                    random2 -> randomBetween(random2, (int) Math.sqrt(requiredFreeSpace), originalMaxWidth),
                    result -> result.count() >= requiredFreeSpace ? result.count() * Math.pow(((double) result.count()) / (result.size() * result.size()), 5) : result.count() - requiredFreeSpace
                );
                maxWidth = Math.min(bestSquare.size(), maxWidth);
                middleX = minX + (bestSquare.x() - 1) + maxWidth / 2;
                middleZ = minZ + (bestSquare.y() - 1) + maxWidth / 2;
                int originalMinX = minX, originalMinZ = minZ;
                minZ = Math.max((originChunk.z - 1) * 16, middleZ - maxWidth / 2);
                maxZ = Math.min((originChunk.z + 1) * 16 - 1, middleZ + maxWidth / 2);
                minX = Math.max((originChunk.x - 1) * 16, middleX - maxWidth / 2);
                maxX = Math.min((originChunk.x + 1) * 16 - 1, middleX + maxWidth / 2);
                xExt = maxX - minX + 1;
                zExt = maxZ - minZ + 1;
                radius = Math.min(xExt, zExt) / 2;
                sqRadius = radius * radius;
                middleX = minX + xExt / 2;
                middleZ = minZ + zExt / 2;
                if (debug) {
                    System.out.println("Free space before: " + freeSpace);
                    System.out.println("Offset: " + bestSquare.x() + ", " + bestSquare.y());
                }
                freeSpace = 0;

                boolean[][] cantPlace2 = new boolean[maxWidth + 3][maxWidth + 3];
                for (int x = 0; x < cantPlace2.length; x++) {
                    for (int z = 0; z < cantPlace2[0].length; z++) {
                        cantPlace2[x][z] = true;
                    }
                }
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        if (circularBorder && (x - middleX) * (x - middleX) + (z - middleZ) * (z - middleZ) > sqRadius) continue;
                        if (!cantPlace[x - originalMinX + 1][z - originalMinZ + 1]) {
                            cantPlace2[x - minX + 1][z - minZ + 1] = false;
                            freeSpace += 1;
                        }
                    }
                }
                cantPlace = cantPlace2;
            }

            if (debug) {
                System.out.println();
                System.out.println("Width \u2264 " + maxWidth);
                System.out.println("Height \u2264 " + maxHeight);
                System.out.println("Depth \u2264 " + maxDepth);
                System.out.println("Free space = " + freeSpace + (freeSpace < requiredFreeSpace ? " (!)" : ""));
            }

            if (freeSpace < requiredFreeSpace) return false;

            Map<String, Double> userParameters = new HashMap<>();
            for (Map.Entry<String, RandomProperty<Double, VariantContext, VariantContext.Predicate>> entry : config.userParameters.sample(random, variantContext).entrySet()) {
                userParameters.put(entry.getKey(), entry.getValue().sample(random, variantContext));
            }
            if (debug) {
                System.out.println();
                for (Map.Entry<String, Double> par : userParameters.entrySet()) {
                    System.out.println(par.getKey() + " = " + par.getValue());
                }
            }

            int surfaceY = origin.getY() - 1;
            int maxY = Math.min(world.getTopY(), surfaceY + maxHeight), minY = Math.max(world.getBottomY(), surfaceY - maxDepth + 1);
            int yExt = maxY - minY + 1;

            FeatureDensityFunctionContext densityContext = new FeatureDensityFunctionContext(seed, minX, maxX, minY, maxY, minZ, maxZ, surfaceY, cantPlace, userParameters);
            Map<String, DensityFunction> densityFunctions = new HashMap<>();
            for (Map.Entry<String, RandomProperty<DensityFunction, VariantContext, VariantContext.Predicate>> entry : config.densityFunctions.sample(random, variantContext).entrySet()) {
                densityFunctions.put(entry.getKey(), entry.getValue().sample(random, variantContext).apply(densityContext.getVisitor()));
            }

            Map<String, double[]> densityValues = new HashMap<>();
            for (Map.Entry<String, DensityFunction> densityFunction : densityFunctions.entrySet()) {
                double[] values = new double[xExt * yExt * zExt];
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            int idx = (xExt * (y - minY) + (x - minX)) * zExt + (z - minZ);
                            values[idx] = densityFunction.getValue().sample(new DensityFunction.UnblendedNoisePos(x, y, z));
                        }
                    }
                }
                densityValues.put(densityFunction.getKey(), values);
            }

            if (debug) {
                System.out.println();
                for (Map.Entry<String, double[]> entry : densityValues.entrySet()) {
                    double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY, avg = 0, std = 0;
                    for (int idx = 0; idx < entry.getValue().length; idx++) {
                        double x = entry.getValue()[idx];
                        min = Math.min(min, x);
                        max = Math.max(max, x);
                        avg += x;
                    }
                    avg /= entry.getValue().length;
                    for (int idx = 0; idx < entry.getValue().length; idx++) {
                        double x = entry.getValue()[idx];
                        std += (x - avg) * (x - avg);
                    }
                    std = Math.sqrt(std / entry.getValue().length);
                    System.out.println(entry.getKey() + " \u2208 [" + min + ", " + max + "] (Mean: " + avg + " \u00b1 " + std + ")");
                }
            }

            // if (maxDist < 3) return false;


            List<SampledRuleSet> ruleSets = new ArrayList<>();
            for (RuleSet ruleSet : config.processingSteps.sample(random, variantContext)) {
                ruleSets.add(SampledRuleSet.sampleAll(ruleSet, random, variantContext, palette, densityValues, densityFunctions, userParameters));
            }

            for (SampledRuleSet ruleSet : ruleSets) {
                IntStream ysStream;
                Optional<Integer> startY = ruleSet.startRelativeY().map(y -> Math.max(minY, Math.min(maxY, y + surfaceY)));
                Optional<Integer> endY = ruleSet.endRelativeY().map(y -> Math.max(minY, Math.min(maxY, y + surfaceY)));
                if (startY.isPresent() && endY.isPresent()) {
                    if (endY.get() >= startY.get()) {
                        ysStream = IntStream.range(startY.get(), endY.get() + 1);
                    } else {
                        ysStream = IntStream.range(endY.get(), startY.get() + 1);
                        ysStream = ysStream.map(i -> startY.get() + endY.get() - i);
                    }
                } else if (endY.isPresent()) {
                    ysStream = IntStream.range(minY, endY.get() + 1);
                } else if (startY.isPresent()) {
                    ysStream = IntStream.range(startY.get(), maxY + 1);
                } else {
                    ysStream = IntStream.range(minY, maxY + 1);
                }

                int[] ys = ysStream.toArray();

                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
                        if (
                            (allowedSurfaceCopy.isEmpty() && ground.isSolid() && !ground.isIn(BlockTags.FEATURES_CANNOT_REPLACE)) ||
                            (allowedSurfaceCopy.isPresent() && allowedSurfaceCopy.get().contains(ground.getRegistryEntry()))) {
                            palette.put("%surface", BlockTemplate.block(ground));
                            palette.put("%surface_reset_state", BlockTemplate.block(ground.getBlock().getDefaultState()));
                        } else {
                            palette.put("%surface", BlockTemplate.empty());
                            palette.put("%surface_reset_state", BlockTemplate.empty());
                        }
                        for (int y : ys) {
                            BlockPos pos = new BlockPos(x, y, z);
                            int idx = (xExt * (y - minY) + (x - minX)) * zExt + (z - minZ);
                            DensityFunction.NoisePos noisePos = new DensityFunction.UnblendedNoisePos(x, y, z);
                            RegistryEntry<Block> blockBeingReplaced = world.getBlockState(pos).getRegistryEntry();

                            /*for (Map.Entry<String, double[]> entry : densityValues.entrySet()) {
                                System.out.println((y - minY) + " | " + entry.getKey() + "=" + entry.getValue()[idx]);
                            }*/

                            ruleSequence:
                            for (SampledRule rule : ruleSet.rules) {
                                if (rule.impossible) continue ruleSequence;

                                if (rule.blockAbove.isPresent() && !rule.blockAbove.get().contains(world.getBlockState(pos.add(0, 1, 0)).getRegistryEntry()))
                                    continue ruleSequence;
                                if (rule.blockBelow.isPresent() && !rule.blockBelow.get().contains(world.getBlockState(pos.add(0, -1, 0)).getRegistryEntry()))
                                    continue ruleSequence;
                                if (rule.blockBeingReplaced.isPresent() && !rule.blockBeingReplaced.get().contains(blockBeingReplaced))
                                    continue ruleSequence;
                                if (rule.cantReplace.map(blockBeingReplaced::isIn, list -> list.contains(blockBeingReplaced)))
                                    continue ruleSequence;

                                for (Condition condition : rule.densityConditions) {
                                    if (!condition.test(idx, noisePos)) continue ruleSequence;
                                }

                                BlockState blockToPlace = rule.template == null ? null : rule.template.getBlockState(world, random, palette);
                                if (blockToPlace != null) {
                                    if (rule.autoWaterlog && blockBeingReplaced.value() == Blocks.WATER && blockToPlace.contains(Properties.WATERLOGGED)) {
                                        blockToPlace = blockToPlace.with(Properties.WATERLOGGED, true);
                                    }

                                    if (!rule.postPorcessing) LandmarksMod.DISABLE_POST_PROCESSING_ONCE = true;

                                    world.setBlockState(pos, blockToPlace, 2);
                                } else if (rule.passThroughIfNotReplaced) {
                                    continue ruleSequence;
                                }

                                break ruleSequence;
                            }
                        }
                    }
                }
            }
        } catch (RandomProperty.NoRandomMatchException e) {
            return false;
        }

        if (debug) System.out.println("#################################\n\n");

        //System.out.println("SurfaceNoiseFeature generated in " + (System.currentTimeMillis() - startTime) + " ms");
        return true;
    }

    public int randomBetween(Random random, int min, int max) {
        return max > min ? random.nextBetween(min, max) : min;
    }

    public static <T> Codec<RandomProperty<T, VariantContext, VariantContext.Predicate>> strictWrappedRandomCodec(Codec<T> entryCodec, String valueKey) {
        return RandomProperty.strictWrappedCodec(entryCodec, VariantContext.Predicate.CODEC, new VariantContext.Predicate(), valueKey);
    }

    public static <T> Codec<RandomProperty<T, VariantContext, VariantContext.Predicate>> strictWrappedRandomCodec(Codec<T> entryCodec) {
        return RandomProperty.strictWrappedCodec(entryCodec, VariantContext.Predicate.CODEC, new VariantContext.Predicate());
    }

    public static <T> Codec<RandomProperty<T, VariantContext, VariantContext.Predicate>> extendRandomCodec(MapCodec<T> entryCodec) {
        return RandomProperty.extendCodec(entryCodec, VariantContext.Predicate.CODEC, new VariantContext.Predicate());
    }

    public static <T> RandomProperty<T, VariantContext, VariantContext.Predicate> defaultRandomProperty(T value) {
        return RandomProperty.singleton(value, new VariantContext.Predicate());
    }

    public record Config(RandomProperty<InstanceConfig, VariantContext, VariantContext.Predicate> instances) implements FeatureConfig {
        public static final Codec<Config> CODEC = extendRandomCodec(InstanceConfig.CODEC).xmap(Config::new, Config::instances);
    }

    public record VariantContext(RegistryEntry<Biome> biome, Set<String> variant, BlockState origin) {
        public record Predicate(Optional<RegistryEntryList<Biome>> biomes, Optional<HashSet<String>> variantsAny, Optional<HashSet<String>> variantsAll, Optional<RegistryEntryList<Block>> originIs) implements RandomProperty.ContextPredicate<VariantContext> {
            public static final MapCodec<Predicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                RegistryCodecs.entryList(RegistryKeys.BIOME).optionalFieldOf("biome").forGetter(Predicate::biomes),
                Codec.STRING.listOf().xmap(HashSet::new, set -> set.stream().toList()).optionalFieldOf("any_variant").forGetter(Predicate::variantsAny),
                Codec.STRING.listOf().xmap(HashSet::new, set -> set.stream().toList()).optionalFieldOf("has_variants").forGetter(Predicate::variantsAll),
                RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("origin").forGetter(Predicate::originIs)
            ).apply(instance, Predicate::new));
            
            public Predicate() { this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()); }
            
            @Override
            public boolean isDefault() {
                return biomes.isEmpty() && variantsAny.isEmpty() && variantsAll.isEmpty();
            }

            @Override
            public boolean test(VariantContext context) {
                return biomes.map(list -> list.contains(context.biome)).orElse(true) &&
                       variantsAny.map(variantSet -> {
                           HashSet<String> variantSetCopy = new HashSet<>(variantSet);
                           variantSetCopy.retainAll(context.variant);
                           return !variantSetCopy.isEmpty();
                       }).orElse(true) &&
                       variantsAll.map(context.variant::containsAll).orElse(true) &&
                       originIs.map(o -> o.contains(context.origin.getRegistryEntry())).orElse(true);
            }
        }
    }

    public record InstanceConfig(
        RandomProperty<RegistryEntryList<Block>, VariantContext, VariantContext.Predicate> usableSpaceAbove,
        RandomProperty<RegistryEntryList<Block>, VariantContext, VariantContext.Predicate> usableSpaceBelow,
        RandomProperty<Map<String, RandomProperty<DensityFunction, VariantContext, VariantContext.Predicate>>, VariantContext, VariantContext.Predicate> densityFunctions,
        RandomProperty<Map<String, RandomProperty<Double, VariantContext, VariantContext.Predicate>>, VariantContext, VariantContext.Predicate> userParameters,
        RandomProperty<List<RuleSet>, VariantContext, VariantContext.Predicate> processingSteps,
        RandomProperty<Integer, VariantContext, VariantContext.Predicate> minWidth, RandomProperty<Integer, VariantContext, VariantContext.Predicate> maxWidth,
        RandomProperty<Integer, VariantContext, VariantContext.Predicate> minHeight, RandomProperty<Integer, VariantContext, VariantContext.Predicate> maxHeight,
        RandomProperty<Integer, VariantContext, VariantContext.Predicate> minDepth, RandomProperty<Integer, VariantContext, VariantContext.Predicate> maxDepth,
        RandomProperty<Integer, VariantContext, VariantContext.Predicate> minSurface,
        boolean searchAround,
        RandomProperty<Palette, VariantContext, VariantContext.Predicate> palette,
        List<RandomProperty<String, VariantContext, VariantContext.Predicate>> variant,
        RandomProperty<Boolean, VariantContext, VariantContext.Predicate> circularBorder
    ) {
        public static final MapCodec<InstanceConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks").fieldOf("usable_space_above").forGetter(InstanceConfig::usableSpaceAbove),
            strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks").fieldOf("usable_space_below").forGetter(InstanceConfig::usableSpaceBelow),
            strictWrappedRandomCodec(Codec.unboundedMap(Codec.STRING, strictWrappedRandomCodec(DensityFunction.FUNCTION_CODEC, "function"))).fieldOf("density_functions").forGetter(InstanceConfig::densityFunctions),
            wrappedRandomCodec(Codec.unboundedMap(Codec.STRING, wrappedRandomCodec(Codec.DOUBLE)), "params").fieldOf("user_parameters").forGetter(InstanceConfig::userParameters),
            wrappedRandomCodec(RuleSet.CODEC.codec().listOf(), "steps").fieldOf("processing_sequence").forGetter(InstanceConfig::processingSteps),
            wrappedRandomCodec(Codec.intRange(1, 48)).fieldOf("min_width").forGetter(InstanceConfig::minWidth),
            wrappedRandomCodec(Codec.intRange(1, 48)).fieldOf("max_width").forGetter(InstanceConfig::maxWidth),
            wrappedRandomCodec(Codecs.NONNEGATIVE_INT).fieldOf("min_height").forGetter(InstanceConfig::minHeight),
            wrappedRandomCodec(Codecs.NONNEGATIVE_INT).fieldOf("max_height").forGetter(InstanceConfig::maxHeight),
            wrappedRandomCodec(Codecs.NONNEGATIVE_INT).fieldOf("min_depth").forGetter(InstanceConfig::minDepth),
            wrappedRandomCodec(Codecs.NONNEGATIVE_INT).fieldOf("max_depth").forGetter(InstanceConfig::maxDepth),
            wrappedRandomCodec(Codecs.NONNEGATIVE_INT).fieldOf("min_surface").forGetter(InstanceConfig::minSurface),
            Codec.BOOL.optionalFieldOf("search_around", false).forGetter(InstanceConfig::searchAround),
            extendRandomCodec(Palette.CODEC).optionalFieldOf("palette", defaultRandomProperty(new Palette())).forGetter(InstanceConfig::palette),
            wrappedRandomCodec(Codec.STRING, "name").listOf().optionalFieldOf("variants", List.of()).forGetter(InstanceConfig::variant),
            wrappedRandomCodec(Codec.BOOL).optionalFieldOf("circular_border", defaultRandomProperty(false)).forGetter(InstanceConfig::circularBorder)
        ).apply(instance, InstanceConfig::new));
    }
    
    public static <T> Codec<RandomProperty<T, VariantContext, VariantContext.Predicate>> wrappedRandomCodec(Codec<T> entryCodec, String valueKey) {
        return RandomProperty.wrappedCodec(entryCodec, VariantContext.Predicate.CODEC, new VariantContext.Predicate(), valueKey);
    }

    public static <T> Codec<RandomProperty<T, VariantContext, VariantContext.Predicate>> wrappedRandomCodec(Codec<T> entryCodec) {
        return RandomProperty.wrappedCodec(entryCodec, VariantContext.Predicate.CODEC, new VariantContext.Predicate());
    }

    public record Palette(
        Map<String, RandomProperty<PalettedBlockTemplate, VariantContext, VariantContext.Predicate>> entries,
        Optional<RandomProperty<RegistryEntryList<Block>, VariantContext, VariantContext.Predicate>> allowedSurfaceCopy
    ) {
        public static final MapCodec<Palette> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, wrappedRandomCodec(PalettedBlockTemplate.CODEC, "block")).fieldOf("entries").forGetter(Palette::entries),
            wrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks").optionalFieldOf("allowed_surface_copy").forGetter(Palette::allowedSurfaceCopy)
            ).apply(instance, Palette::new)
        );

        public Palette() { this(new HashMap<>(), Optional.empty()); }

        public Map<String, BlockTemplate> sampleAll(Random random, VariantContext context) throws RandomProperty.NoRandomMatchException {
            Map<String, BlockTemplate> resolved = new HashMap<>();
            Map<String, String> unresolved = new HashMap<>();

            for (Map.Entry<String, RandomProperty<PalettedBlockTemplate, VariantContext, VariantContext.Predicate>> entry : entries.entrySet()) {
                try {
                    PalettedBlockTemplate sampled = entry.getValue().sample(random, context);
                    sampled.referenceOrTemplate.map(
                        str -> str.startsWith("%") ? resolved.put(entry.getKey(), BlockTemplate.parse("%" + str))
                            : unresolved.put(entry.getKey(), str),
                        template -> resolved.put(entry.getKey(), template));
                } catch (RandomProperty.NoRandomMatchException ignored) {
                    resolved.put(entry.getKey(), null);
                }
            }

            int lastUnresolvedCount = unresolved.size(), unchangedIterations = 0;
            while (!unresolved.isEmpty() && unchangedIterations < 10) {
                for (Iterator<Map.Entry<String, String>> it = unresolved.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, String> entry = it.next();
                    String reference = entry.getValue();
                    if (resolved.containsKey(reference)) {
                        //unresolved.remove(entry.getKey());
                        it.remove();
                        resolved.put(entry.getKey(), resolved.get(reference));
                    } else if (unresolved.containsKey(reference)) {
                        unresolved.put(entry.getKey(), unresolved.get(reference));
                    } else {
                        //unresolved.remove(entry.getKey());
                        it.remove();
                        resolved.put(entry.getKey(), BlockTemplate.empty());
                        resolved.put(reference, BlockTemplate.empty());
                    }
                }

                if (unresolved.size() == lastUnresolvedCount) {
                    unchangedIterations += 1;
                } else {
                    unchangedIterations = 0;
                    lastUnresolvedCount = unresolved.size();
                }
            }

            if (!unresolved.isEmpty()) throw new RuntimeException("Unresolved palette references: " + String.join(", ",
                unresolved.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).toList()) +
                " (variants: " + String.join(", ", context.variant) + ")");

            return resolved;
        }
    }

    public record PalettedBlockTemplate(Either<String, BlockTemplate> referenceOrTemplate) {
        public static final Codec<PalettedBlockTemplate> CODEC = Codec.either(
            Codec.STRING.comapFlatMap(
                str -> str.startsWith("%") && !str.contains(";") && !str.contains("*") ? DataResult.success(str.substring(1)) : DataResult.<String>error(() -> "Palette references must begin with %."),
                ref -> "%" + ref
            ),
            BlockTemplate.CODEC
        ).xmap(PalettedBlockTemplate::new, PalettedBlockTemplate::referenceOrTemplate);
    }

    public record RuleSet(
        List<RandomProperty<Rule, VariantContext, VariantContext.Predicate>> rules,
        Optional<Integer> startRelativeY,
        Optional<Integer> endRelativeY
        ) {
        public static final MapCodec<RuleSet> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            extendRandomCodec(Rule.CODEC).listOf().fieldOf("rules").forGetter(RuleSet::rules),
            Codec.INT.optionalFieldOf("start_relative_y").forGetter(RuleSet::startRelativeY),
            Codec.INT.optionalFieldOf("end_relative_y").forGetter(RuleSet::endRelativeY)
        ).apply(instance, RuleSet::new));
    }

    public record SampledRuleSet(
        List<SampledRule> rules,
        Optional<Integer> startRelativeY,
        Optional<Integer> endRelativeY
    ) {
        public static SampledRuleSet sampleAll(RuleSet ruleSet, Random random, VariantContext context, Map<String, BlockTemplate> palette, Map<String, double[]> densityValues, Map<String, DensityFunction> densityFunctions, Map<String, Double> userParameters) {
            List<SampledRule> rules = new ArrayList<>();
            for (RandomProperty<Rule, VariantContext, VariantContext.Predicate> rule : ruleSet.rules) {
                try {
                    rules.add(SampledRule.sampleAll(rule.sample(random, context), random, context, palette, densityValues, densityFunctions, userParameters));
                } catch (RandomProperty.NoRandomMatchException ignored) {}
            }
            return new SampledRuleSet(rules, ruleSet.startRelativeY, ruleSet.endRelativeY);
        }
    }
    
    
    public record Rule(
        RandomProperty<PalettedBlockTemplate, VariantContext, VariantContext.Predicate> template,
        boolean postProcessing,
        boolean autoWaterlog,
        boolean passThroughIfNotReplaced,
        boolean impossible,
        Optional<RandomProperty<RegistryEntryList<Block>, VariantContext, VariantContext.Predicate>> blockBelow,
        Optional<RandomProperty<RegistryEntryList<Block>, VariantContext, VariantContext.Predicate>> blockBeingReplaced,
        Optional<RandomProperty<RegistryEntryList<Block>, VariantContext, VariantContext.Predicate>> blockAbove,
        Either<TagKey<Block>, RandomProperty<RegistryEntryList<Block>, VariantContext, VariantContext.Predicate>> cantReplace,
        RandomProperty<List<RandomProperty<String, VariantContext, VariantContext.Predicate>>, VariantContext, VariantContext.Predicate> densityConditions
    ) {
        public static final MapCodec<Rule> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            wrappedRandomCodec(PalettedBlockTemplate.CODEC, "block").optionalFieldOf("place",
                defaultRandomProperty(new PalettedBlockTemplate(Either.right(BlockTemplate.empty())))).forGetter(Rule::template),
            Codec.BOOL.optionalFieldOf("post_processing", true).forGetter(Rule::postProcessing),
            Codec.BOOL.optionalFieldOf("auto_waterlog", false).forGetter(Rule::autoWaterlog),
            Codec.BOOL.optionalFieldOf("pass_through_if_not_replaced", false).forGetter(Rule::passThroughIfNotReplaced),
            Codec.BOOL.optionalFieldOf("impossible", false).forGetter(Rule::impossible),
            strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks").optionalFieldOf("block_below_is").forGetter(Rule::blockBelow),
            strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks").optionalFieldOf("block_is").forGetter(Rule::blockBeingReplaced),
            strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks").optionalFieldOf("block_above_is").forGetter(Rule::blockAbove),
            Codec.either(
                TagKey.codec(RegistryKeys.BLOCK),
                strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks")
            ).optionalFieldOf("cant_replace", Either.left(BlockTags.FEATURES_CANNOT_REPLACE)).forGetter(Rule::cantReplace),
            strictWrappedRandomCodec(strictWrappedRandomCodec(Codec.STRING).listOf(), "conditions").optionalFieldOf(
                "density_conditions", defaultRandomProperty(List.of())).forGetter(Rule::densityConditions)
        ).apply(instance, Rule::new));
    }

    public record SampledRule(
        BlockTemplate template,
        boolean postPorcessing,
        boolean autoWaterlog,
        boolean passThroughIfNotReplaced,
        boolean impossible,
        Optional<RegistryEntryList<Block>> blockBelow,
        Optional<RegistryEntryList<Block>> blockBeingReplaced,
        Optional<RegistryEntryList<Block>> blockAbove,
        Either<TagKey<Block>, RegistryEntryList<Block>> cantReplace,
        List<Condition> densityConditions
    ) {
        public static SampledRule sampleAll(Rule rule, Random random, VariantContext context, Map<String, BlockTemplate> palette, Map<String, double[]> densityValues, Map<String, DensityFunction> densityFunctions, Map<String, Double> userParameters) throws RandomProperty.NoRandomMatchException {
            Optional<RegistryEntryList<Block>> blockBelow = Optional.empty();
            Optional<RegistryEntryList<Block>> blockBeingReplaced = Optional.empty();
            Optional<RegistryEntryList<Block>> blockAbove = Optional.empty();
            Either<TagKey<Block>, RegistryEntryList<Block>> cantReplace;
            List<Condition> conditions = new ArrayList<>();

            if (rule.blockBelow.isPresent()) blockBelow = Optional.of(rule.blockBelow.get().sample(random, context));
            if (rule.blockBeingReplaced.isPresent()) blockBeingReplaced = Optional.of(rule.blockBeingReplaced.get().sample(random, context));
            if (rule.blockAbove.isPresent()) blockAbove = Optional.of(rule.blockAbove.get().sample(random, context));

            if (rule.cantReplace.left().isPresent()) {
                cantReplace = Either.left(rule.cantReplace.left().get());
            } else {
                cantReplace = Either.right(rule.cantReplace().right().get().sample(random, context));
            }

            for (RandomProperty<String, VariantContext, VariantContext.Predicate> condition : rule.densityConditions.sample(random, context)) {
                conditions.add(Condition.parse(condition.sample(random, context), userParameters, densityValues, densityFunctions));
            }

            BlockTemplate template = null;
            try {
                template = rule.template.sample(random, context).referenceOrTemplate.map(palette::get, t -> t);
            } catch (RandomProperty.NoRandomMatchException ignored) {}

            return new SampledRule(
                template == null ? null : template.copy(),
                rule.postProcessing,
                rule.autoWaterlog,
                rule.passThroughIfNotReplaced,
                rule.impossible,
                blockBelow,
                blockBeingReplaced,
                blockAbove,
                cantReplace,
                conditions
            );
        }
    }

    public record Condition(List<Operand> operands, List<Operator> operators) {
        public static Condition parse(String condition, Map<String, Double> constants, Map<String, double[]> arrays, Map<String, DensityFunction> functions) {
            String[] split = condition.replaceAll(" +", "").splitWithDelimiters("([><!=]=?|[-+])", 0);

            List<Operand> operands = new ArrayList<>();
            for (int i = 0; i < split.length; i += 2) {
                String variable = split[i];
                if (variable.isEmpty()) {
                    operands.add(new Constant(0));
                } else if (arrays.containsKey(variable)) {
                    operands.add(new ConstantArray(arrays.get(variable)));
                } else if (functions.containsKey(variable)) {
                    operands.add(new Density(functions.get(variable)));
                } else if (constants.containsKey(variable)) {
                    operands.add(new Constant(constants.get(variable)));
                } else if (Character.isDigit(variable.charAt(0))) {
                    operands.add(new Constant(Double.parseDouble(variable)));
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

        public boolean test(int idx, DensityFunction.NoisePos pos) {
            if (operands.size() > 1) {
                List<Double> conditionOperands = new ArrayList<>();
                List<Operator> conditionOperators = new ArrayList<>();
                double accValue = operands.getFirst().get(idx, pos);
                int operatorIdx = 0;
                for (int i = 1; i < operands.size(); i++) {
                    double value = operands.get(i).get(idx, pos);
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

        public interface Operand {
            double get(int idx, DensityFunction.NoisePos pos);
        }

        private static class Constant implements Operand {
            private final double value;

            private Constant(double value) {
                this.value = value;
            }

            @Override
            public double get(int idx, DensityFunction.NoisePos pos) {
                return value;
            }
        }

        private static class ConstantArray implements Operand {
            private final double[] value;

            private ConstantArray(double[] value) {
                this.value = value;
            }

            @Override
            public double get(int idx, DensityFunction.NoisePos pos) {
                return value[idx];
            }
        }

        private static class Density implements Operand {
            private final DensityFunction function;

            private Density(DensityFunction function) {
                this.function = function;
            }

            @Override
            public double get(int idx, DensityFunction.NoisePos pos) {
                return function.sample(pos);
            }
        }
    }
}
