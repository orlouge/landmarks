package io.github.orlouge.landmarks.features;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.density.BoundedFunction;
import io.github.orlouge.landmarks.utils.RandomProperty;
import io.github.orlouge.landmarks.utils.StringUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
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

public class SurfaceNoiseFeature extends Feature<SurfaceNoiseFeature.Config> {
    public SurfaceNoiseFeature(Codec<Config> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<Config> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();
        RegistryEntry<Biome> biome = context.getWorld().getBiome(origin);
        ChunkPos originChunk = new ChunkPos(origin);
        long seed = random.nextLong();
        random = Random.create(seed);
        long startTime = System.currentTimeMillis();

        int minZ = (originChunk.z - 1) * 16, maxZ = (originChunk.z + 2) * 16 - 1;
        int minX = (originChunk.x - 1) * 16, maxX = (originChunk.x + 2) * 16 - 1;
        int minY = world.getBottomY(), maxY = world.getTopY();

        try {
            Config config = context.getConfig();
            boolean debug = config.debug;

            if (debug) {
                System.out.println("#################################");
                context.getFeature().flatMap(world.getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE)::getKey).ifPresentOrElse(
                    k -> System.out.println("Generating configured feature " + k.getValue()),
                    () -> System.out.println("Generating surface noise feature")
                );
                System.out.println("  at " + origin + " (" + biome.getKeyOrValue().map(k -> k.getValue().toString(), k -> "?") + ") with seed " + seed);
            }

            BlockPos minPos = new BlockPos(minX, minY, minZ), maxPos = new BlockPos(maxX, maxY, maxZ);

            VariantContext variantContext = new VariantContext(world, seed, biome, new HashMap<>(), origin, minPos, maxPos, new HashMap<>(), new HashMap<>(), new Palette());

            VariantWithFragment.VariantSampler<Generator.RandomizedGeneratorConfig> variantSampler = new VariantWithFragment.VariantSampler<>();
            variantSampler.addUnconditionalSubvariants(config.baseGenerator);
            variantContext = variantSampler.sampleContext(random, variantContext);

            if (debug) printContext("(Base)", variantContext);

            Generator baseGenerator = config.baseGenerator.fragment().sample(random, variantContext);
            if (baseGenerator.abort()) {
                if (debug) printContext("(Aborted)", variantContext.withParameters(baseGenerator.parameters()));
                return false;
            }
            if (!baseGenerator.skip()) variantContext = baseGenerator.generate(world, random, variantContext);

            DensityFunction.DensityFunctionVisitor visitor = variantContext.getVisitor();
            if (config.bounds.isPresent()) {
                Parameter.Sampler sampler = config.bounds.get().map(variantContext.userParameters()::get, p -> p.createSampler(visitor));
                if (sampler.bounds().isPresent()) {
                    minZ = Math.max(minZ, sampler.bounds().get().minZ());
                    maxZ = Math.min(maxZ, sampler.bounds().get().maxZ());
                    minX = Math.max(minX, sampler.bounds().get().minX());
                    maxX = Math.min(maxX, sampler.bounds().get().maxX());
                    minY = Math.max(minY, sampler.bounds().get().minY());
                    maxY = Math.min(maxY, sampler.bounds().get().maxY());
                } else {
                    int _minX = maxX, _maxX = minX;
                    int _minY = maxY, _maxY = minY;
                    int _minZ = maxZ, _maxZ = minZ;

                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z < maxZ; z++) {
                                if (sampler.sample(new DensityFunction.UnblendedNoisePos(x, y, z)) > 0) {
                                    if (x < _minX) _minX = x;
                                    if (x > _maxX) _maxX = x;
                                    if (y < _minY) _minY = y;
                                    if (y > _maxY) _maxY = y;
                                    if (z < _minZ) _minZ = z;
                                    if (z > _maxZ) _maxZ = z;
                                }
                            }
                        }
                    }

                    minX = _minX; minY = _minY; minZ = _minZ;
                    maxX = _maxX; maxY = _maxY; maxZ = _maxZ;
                }
            }

            DensityFunction.UnblendedNoisePos zeroPos = new DensityFunction.UnblendedNoisePos(0, 0, 0);
            if (config.minX.isPresent()) minX = Math.max(minX, (int) (config.minX.get().map(variantContext.userParameters()::get, p -> p.createSampler(visitor)).sample(zeroPos)));
            if (config.minY.isPresent()) minY = Math.max(minY, (int) (config.minY.get().map(variantContext.userParameters()::get, p -> p.createSampler(visitor)).sample(zeroPos)));
            if (config.minZ.isPresent()) minZ = Math.max(minZ, (int) (config.minZ.get().map(variantContext.userParameters()::get, p -> p.createSampler(visitor)).sample(zeroPos)));
            if (config.maxX.isPresent()) maxX = Math.min(maxX, (int) (config.maxX.get().map(variantContext.userParameters()::get, p -> p.createSampler(visitor)).sample(zeroPos)));
            if (config.maxY.isPresent()) maxY = Math.min(maxY, (int) (config.maxY.get().map(variantContext.userParameters()::get, p -> p.createSampler(visitor)).sample(zeroPos)));
            if (config.maxZ.isPresent()) maxZ = Math.min(maxZ, (int) (config.maxZ.get().map(variantContext.userParameters()::get, p -> p.createSampler(visitor)).sample(zeroPos)));
            variantContext = variantContext.withBounds(minX, minY, minZ, maxX, maxY, maxZ);

            for (Either<Identifier, VariantWithFragment<Generator.RandomizedGeneratorConfig>> referenceOrGenerator : config.generators) {
                if (debug) printContext(referenceOrGenerator.map(Identifier::toString, r -> "(Anonymous)"), variantContext);
                Generator generator;
                if (referenceOrGenerator.left().isPresent()) {
                    Pair<Generator.RandomizedGeneratorConfig, VariantContext> pair = Generator.RandomizedGeneratorConfig.getAndMerge(List.of(referenceOrGenerator.left().get()), new Generator.RandomizedGeneratorConfig(), random, variantContext, world.getRegistryManager());
                    variantContext = pair.getRight();
                    generator = pair.getLeft().sample(random, variantContext);
                } else {
                    VariantWithFragment<Generator.RandomizedGeneratorConfig> randomGen = referenceOrGenerator.right().get();
                    VariantWithFragment.VariantSampler<Generator.RandomizedGeneratorConfig> sampler = new VariantWithFragment.VariantSampler<>();
                    sampler.addUnconditionalSubvariants(randomGen);
                    variantContext = sampler.sampleContext(random, variantContext);
                    generator = randomGen.fragment().sample(random, variantContext);
                }
                if (generator.abort()) {
                    if (debug) printContext("(Aborted)", variantContext.withParameters(generator.parameters()));
                    return false;
                }
                if (generator.skip()) {
                    if (debug) System.out.println("Skipped");
                    continue;
                }
                variantContext = generator.generate(world, random, variantContext);
            }
            if (debug) printContext("(Exiting)", variantContext);

            if (debug) {
                System.out.println("Generated in " + (System.currentTimeMillis() - startTime) + " ms");
                System.out.println("#################################\n\n");
            }
        } catch (RandomProperty.NoRandomMatchException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void printContext(String generator, VariantContext variantContext) {
        System.out.println("Context for " + generator + ":");
        System.out.println("  Bounds: " + variantContext.minPos() + " -> " + variantContext.maxPos());
        if (!variantContext.variant().isEmpty())
            System.out.println("  Variants: " + String.join(", ",
                variantContext.variant().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList()));
        if (!variantContext.userParameters().isEmpty())
            try {
                System.out.println("  Parameters: " + String.join(", ",
                    variantContext.userParameters().entrySet().stream().map(e -> {
                        String value;
                        try {
                            if (e.getValue() instanceof Parameter.Constant cons) {
                                value = "" + cons.value;
                                if (value.length() > 7) value = String.format("%.3f", cons.value);
                            } else {
                                double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY, mean = 0, cnt = 0, std = 0;
                                for (BlockPos pos : BlockPos.iterate(variantContext.minPos(), variantContext.maxPos())) {
                                    double val = e.getValue().sample(new DensityFunction.UnblendedNoisePos(pos.getX(), pos.getY(), pos.getZ()));
                                    min = Math.min(val, min);
                                    max = Math.max(val, max);
                                    mean += val;
                                    cnt += 1;
                                }
                                mean /= cnt;
                                if (max - min < 1e-4) {
                                    value = "" + mean;
                                    if (value.length() > 7) value = String.format("%.3f", mean);
                                } else {
                                    for (BlockPos pos : BlockPos.iterate(variantContext.minPos(), variantContext.maxPos())) {
                                        double val = e.getValue().sample(new DensityFunction.UnblendedNoisePos(pos.getX(), pos.getY(), pos.getZ()));
                                        std += (val - mean) * (val - mean);
                                    }
                                    std = Math.sqrt(std / cnt);
                                    value = String.format("%.3f\u00b1%.2f \u2208 [%.2f,%.2f]", mean, std, min, max);
                                }
                            }
                        } catch (Exception ex) {
                            value = ex.getMessage();
                        }
                        return e.getKey() + " = " + value;
                    }).toList()));
            } catch (Exception ex) {
                System.out.println("  Parameters: ERROR");
                ex.printStackTrace();
            }
        if (!(variantContext.palette().entries().isEmpty() && variantContext.palette().copiedEntries().isEmpty()))
            System.out.println("  Palette: " + String.join(", ",
                variantContext.palette().entries().entrySet().stream().map(e ->
                    e.getKey() + " = " + e.getValue().toString()
                ).toList()) + " | " + String.join(",",
                variantContext.palette().copiedEntries().keySet()));
    }

    public record Config(
        List<Either<Identifier, VariantWithFragment<Generator.RandomizedGeneratorConfig>>> generators,
        VariantWithFragment<Generator.RandomizedGeneratorConfig> baseGenerator,
        Optional<Either<String, Parameter>> minX,
        Optional<Either<String, Parameter>> minY,
        Optional<Either<String, Parameter>> minZ,
        Optional<Either<String, Parameter>> maxX,
        Optional<Either<String, Parameter>> maxY,
        Optional<Either<String, Parameter>> maxZ,
        Optional<Either<String, Parameter>> bounds,
        boolean debug
    ) implements FeatureConfig {
        public static final MapCodec<Config> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(Identifier.CODEC, Generator.RandomizedGeneratorConfig.VARIANT_MAP_CODEC.codec()).listOf()
                .optionalFieldOf("generators", Collections.emptyList()).forGetter(Config::generators),
            Generator.RandomizedGeneratorConfig.VARIANT_MAP_CODEC.forGetter(Config::baseGenerator),
            Parameter.CODEC_NAMED.optionalFieldOf("min_x").forGetter(Config::minX),
            Parameter.CODEC_NAMED.optionalFieldOf("min_y").forGetter(Config::minY),
            Parameter.CODEC_NAMED.optionalFieldOf("min_z").forGetter(Config::minZ),
            Parameter.CODEC_NAMED.optionalFieldOf("max_x").forGetter(Config::maxX),
            Parameter.CODEC_NAMED.optionalFieldOf("max_y").forGetter(Config::maxY),
            Parameter.CODEC_NAMED.optionalFieldOf("max_z").forGetter(Config::maxZ),
            Parameter.CODEC_NAMED.optionalFieldOf("bounds").forGetter(Config::bounds),
            Codec.BOOL.optionalFieldOf("debug", false).forGetter(Config::debug)
        ).apply(instance, Config::new));
    }


}
