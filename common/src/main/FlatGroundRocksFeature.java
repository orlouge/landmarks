package io.github.orlouge.landmarks.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.generation.BlockTemplate;
import io.github.orlouge.landmarks.density.BorderChamferDistance;
import io.github.orlouge.landmarks.utils.OldBiomeRandomProperty;
import io.github.orlouge.landmarks.utils.ChamferTransform;
import io.github.orlouge.landmarks.utils.OpenSimplex2;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class FlatGroundRocksFeature extends Feature<FlatGroundRocksFeature.Config> {
    public FlatGroundRocksFeature(Codec<Config> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<Config> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin(), center = origin;
        RegistryEntry<Biome> biome = context.getWorld().getBiome(origin);
        ChunkPos originChunk = new ChunkPos(origin);
        if (origin.getY() >= world.getTopY() - 5) return false;
        long seed = random.nextLong();

        try {
            VariantConfig config = context.getConfig().variants.sample(random, biome);
            int maxHeight = random.nextBetween(config.minHeight, config.maxHeight);
            int maxWidth = random.nextBetween(config.minWidth, config.maxWidth);
            float horizontalScale = config.noise.horizontalScale.sample(random, biome);
            PaletteConfig palette = config.palette.sample(random, biome);
            Map<TagKey<Block>, BlockTemplate> exposedGroundReplacement = palette.exposedGroundReplacement.sample(random, biome);
            Map<TagKey<Block>, BlockTemplate> buriedGroundReplacement = palette.buriedGroundReplacement.sample(random, biome);
            Map<TagKey<Block>, BlockTemplate> extraBlockGround = palette.extraBlockGround.sample(random, biome);
            Map<TagKey<Block>, BlockTemplate> extraBlockTop = palette.extraBlockTop.sample(random, biome);
            BlockTemplate innerTemplate = palette.inner.sample(random, biome);
            BlockTemplate topHalfTemplate = palette.topHalf.sample(random, biome);
            BlockTemplate topFullTemplate = palette.topFull.sample(random, biome);


            int minZ = Math.max((originChunk.z - 1) * 16, center.getZ() - maxWidth / 2), maxZ = Math.min((originChunk.z + 1) * 16 - 1, center.getZ() + maxWidth / 2);
            int minX = Math.max((originChunk.x - 1) * 16, center.getX() - maxWidth / 2), maxX = Math.min((originChunk.x + 1) * 16 - 1, center.getX() + maxWidth / 2);
            boolean[][] cantPlace = new boolean[maxWidth + 3][maxWidth + 3];
            int freeSpace = 0;
            for (int x = 0; x < cantPlace.length; x++) {
                for (int z = 0; z < cantPlace[0].length; z++) {
                    cantPlace[x][z] = true;
                }
            }
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, center.getY(), z);
                    if ((world.getBlockState(pos.add(0, -1, 0))).isIn(config.canPlaceOn) && IntStream.range(0, maxHeight).allMatch(off -> (world.getBlockState(pos.add(0, off, 0))).isIn(config.canReplace)) /* || canReplace(world.getBlockState(pos.add(0, 1, 0))) */) {
                        cantPlace[x - minX + 1][z - minZ + 1] = false;
                        freeSpace += 1;
                    }
                }
            }

            if (freeSpace < config.minSurface) return false;

            Optional<DensityFunction> test = config.test.map(d -> d.apply((d2) -> {
                if (d2 instanceof BorderChamferDistance edgeDist) {
                    return edgeDist.create(cantPlace, minX - 1, maxX + 1, minZ - 1, maxZ + 1);
                } else return d2;
            }));
            double[][] dist = ChamferTransform.distanceTransform(cantPlace);
            double maxDist = -1;

            for (int x = 0; x < dist.length; x++) {
                for (int z = 0; z < dist[0].length; z++) {
                    int borderDist = Math.min(Math.min(x, z), Math.min(dist.length - x - 1, dist[0].length - z - 2));
                    if (borderDist < maxWidth / 10)
                        dist[x][z] /= 2.0 + random.nextDouble() * 4 / (1 + borderDist);
                    else
                        dist[x][z] /= 2.0;
                    maxDist = Math.max(dist[x][z], maxDist);
                }
            }

            if (maxDist < 3) return false;

            // TODO: optimize minX/maxZ in the dist loop
            //minZ = center.getZ() - maxWidth / 2; maxZ = center.getZ() + maxWidth / 2;
            //minX = center.getX() - maxWidth / 2; maxX = center.getX() + maxWidth / 2;
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int relX = x - center.getX(), relZ = z - center.getZ();
                    // double centrality = 1. / Math.pow(Math.max(1, (double) (relX * relX + relZ * relZ - 40)), 0.3);
                    double distance = dist[x - minX + 1][z - minZ + 1];
                    //double distance = 1;
                    //if (test.isPresent()) distance = test.get().sample(new DensityFunction.UnblendedNoisePos(x, center.getY(), z));
                    //if (distance > 1) System.out.println(distance);
                    if (distance < 1) continue;
                    double centrality = Math.max(0, 5 * ((1 / (1 + Math.exp(-4 * (distance + config.noise.spread) / maxDist))) - 0.78));
                    double noise = OpenSimplex2.noise2(seed, ((float) relX) * horizontalScale, ((float) relZ) * horizontalScale);
                    double noise2 = OpenSimplex2.noise2(seed + 1, ((float) relX) * (horizontalScale + 0.05f), ((float) relZ) * (horizontalScale + 0.05f));
                    double noiseHeight = noise2 < 0 ? noise + 0.5 * noise2 : Math.max(noise, noise2 * 0.5) + noise2 * 0.1;
                    noiseHeight = Math.max(0, noiseHeight + config.noise.density);
                    noiseHeight /= (1 + config.noise.density);
                    //noiseHeight = 1;

                    double height = Math.pow(Math.min(1, noiseHeight * centrality), 1 / config.heightBoost) * (maxHeight);
                    int placedBlocks = 0;

                    BlockState ground = world.getBlockState(new BlockPos(x, center.getY() - 1, z));
                    if (height < 0.1) {
                        if (height > 0.004) {
                            for (Map.Entry<TagKey<Block>, BlockTemplate> entry : exposedGroundReplacement.entrySet()) {
                                if (ground.isIn(entry.getKey())) {
                                    BlockState block = entry.getValue().getBlockState(world, random, Collections.emptyMap());
                                    if (block != null)
                                        this.setBlockStateIf(world, new BlockPos(x, center.getY() - 1, z), block, s -> !s.isIn(BlockTags.FEATURES_CANNOT_REPLACE));
                                    break;
                                }
                            }
                            ground = world.getBlockState(new BlockPos(x, center.getY() - 1, z));
                            for (Map.Entry<TagKey<Block>, BlockTemplate> entry : extraBlockGround.entrySet()) {
                                if (ground.isIn(entry.getKey())) {
                                    BlockState block = entry.getValue().getBlockState(world, random, Collections.emptyMap());
                                    if (block != null)
                                        this.setBlockStateIf(world, new BlockPos(x, center.getY(), z), block, s -> s.isIn(config.canReplace));
                                    break;
                                }
                            }
                        }
                    } else {
                        BlockState topBlock = null;
                        BlockPos pos = center;
                        for (int y = center.getY(); y < world.getTopY(); y++) {
                            if (height < 0.1) break;
                            pos = new BlockPos(x, y, z);
                            if (placedBlocks == 0 && !world.getBlockState(pos).isIn(config.canReplace)) continue;
                            BlockTemplate template = topHalfTemplate;
                            if (height > 0.6) template = topFullTemplate;
                            if (height > 1.1) template = innerTemplate;
                            topBlock = template == null ? ground : template.getBlockState(world, random, Collections.emptyMap());
                            /*
                            if (test.isPresent()) {
                                double _test = test.get().sample(new DensityFunction.UnblendedNoisePos(x, y, z));
                                if (_test < -0.8) topBlock = Blocks.AIR.getDefaultState();
                            }
                             */
                            if (topBlock == null)
                                topBlock = palette.resetCopiedGroundBlockState ? ground.getBlock().getDefaultState() : ground;
                            BlockState blockBeingReplaced = world.getBlockState(pos);
                            if (blockBeingReplaced.isIn(config.canReplace)) {
                                if (blockBeingReplaced.getBlock() == Blocks.WATER && topBlock.contains(Properties.WATERLOGGED)) {
                                    topBlock = topBlock.with(Properties.WATERLOGGED, true);
                                }
                                world.setBlockState(pos, topBlock, 2);
                            }
                            height--;
                            placedBlocks++;
                        }
                        for (Map.Entry<TagKey<Block>, BlockTemplate> entry : buriedGroundReplacement.entrySet()) {
                            if (ground.isIn(entry.getKey())) {
                                BlockState block = entry.getValue().getBlockState(world, random, Collections.emptyMap());
                                if (block != null)
                                    this.setBlockStateIf(world, new BlockPos(x, center.getY() - 1, z), block, s -> !s.isIn(BlockTags.FEATURES_CANNOT_REPLACE));
                                break;
                            }
                        }
                        for (Map.Entry<TagKey<Block>, BlockTemplate> entry : extraBlockTop.entrySet()) {
                            if (topBlock != null && topBlock.isIn(entry.getKey())) {
                                BlockState block = entry.getValue().getBlockState(world, random, Collections.emptyMap());
                                if (block != null)
                                    this.setBlockStateIf(world, pos.add(0, 1, 0), block, s -> s.isIn(config.canReplace));
                                break;
                            }
                        }
                    }
                }
            }
        } catch (OldBiomeRandomProperty.NoBiomeMatchException e) {
            return false;
        }

        return true;
    }

    /*
    public record Config(List<VariantConfig> instances) implements FeatureConfig {
        public static final Codec<Config> CODEC = Codec.either(VariantConfig.CODEC.listOf(1, Integer.MAX_VALUE), VariantConfig.CODEC).xmap(
            either -> either.map(Config::new, variant -> new Config(List.of(variant))),
            config -> config.instances.size() == 1 ? Either.right(config.instances.getFirst()) : Either.left(config.instances)
        );
    }
     */
    public record Config(OldBiomeRandomProperty<VariantConfig> variants) implements FeatureConfig {
        public static final Codec<Config> CODEC = OldBiomeRandomProperty.extendCodec(VariantConfig.CODEC).xmap(Config::new, Config::variants);
    }

    public record VariantConfig(
        int minWidth, int maxWidth, int minSurface, int minHeight, int maxHeight, double heightBoost,
        RegistryEntryList<Block> canPlaceOn, RegistryEntryList<Block> canReplace,
        NoiseConfig noise, OldBiomeRandomProperty<PaletteConfig> palette, Optional<DensityFunction> test
    ) {
        public static final MapCodec<VariantConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 48).fieldOf("min_width").forGetter(VariantConfig::minWidth),
            Codec.intRange(1, 48).fieldOf("max_width").forGetter(VariantConfig::maxWidth),
            Codec.INT.fieldOf("min_surface").forGetter(VariantConfig::minSurface),
            Codec.INT.fieldOf("min_height").forGetter(VariantConfig::minHeight),
            Codec.INT.fieldOf("max_height").forGetter(VariantConfig::maxHeight),
            Codec.DOUBLE.fieldOf("height_boost").forGetter(VariantConfig::heightBoost),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("can_place_on").forGetter(VariantConfig::canPlaceOn),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("can_replace").forGetter(VariantConfig::canReplace),
            NoiseConfig.CODEC.fieldOf("noise").forGetter(VariantConfig::noise),
            OldBiomeRandomProperty.extendCodec(PaletteConfig.CODEC).fieldOf("processingSteps").forGetter(VariantConfig::palette),
            DensityFunction.FUNCTION_CODEC.optionalFieldOf("test").forGetter(VariantConfig::test)
        ).apply(instance, VariantConfig::new));
    }

    public record NoiseConfig(OldBiomeRandomProperty<Float> horizontalScale, float spread, double density) {
        public static final Codec<NoiseConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            /*
            Codec.either(Codec.FLOAT.listOf(1, Integer.MAX_VALUE), Codec.FLOAT).xmap(
                either -> either.map(list -> list, List::of),
                list -> list.size() == 1 ? Either.right(list.getFirst()) : Either.left(list)
            ).fieldOf("horizontal_scale").forGetter(NoiseConfig::horizontalScale),
             */
            OldBiomeRandomProperty.wrappedCodec(Codec.FLOAT, "input").fieldOf("horizontal_scale").forGetter(NoiseConfig::horizontalScale),
            Codec.FLOAT.fieldOf("spread").forGetter(NoiseConfig::spread),
            Codec.DOUBLE.fieldOf("density").forGetter(NoiseConfig::density)
        ).apply(instance, NoiseConfig::new));
    }

    public record PaletteConfig(
        OldBiomeRandomProperty<BlockTemplate> inner, OldBiomeRandomProperty<BlockTemplate> topFull, OldBiomeRandomProperty<BlockTemplate> topHalf,
        OldBiomeRandomProperty<Map<TagKey<Block>, BlockTemplate>> exposedGroundReplacement,
        OldBiomeRandomProperty<Map<TagKey<Block>, BlockTemplate>> buriedGroundReplacement,
        OldBiomeRandomProperty<Map<TagKey<Block>, BlockTemplate>> extraBlockGround,
        OldBiomeRandomProperty<Map<TagKey<Block>, BlockTemplate>> extraBlockTop,
        boolean resetCopiedGroundBlockState, boolean autoWaterlog
    ) {
        public static final MapCodec<PaletteConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            OldBiomeRandomProperty.wrappedCodec(BlockTemplate.CODEC, "template").fieldOf("inner").forGetter(PaletteConfig::inner),
            OldBiomeRandomProperty.wrappedCodec(BlockTemplate.CODEC, "template").fieldOf("top_full").forGetter(PaletteConfig::topFull),
            OldBiomeRandomProperty.wrappedCodec(BlockTemplate.CODEC, "template").fieldOf("top_half").forGetter(PaletteConfig::topHalf),
            OldBiomeRandomProperty.wrappedCodec(Codec.unboundedMap(TagKey.codec(RegistryKeys.BLOCK), BlockTemplate.CODEC), "replacements").optionalFieldOf("exposed_ground_replace", OldBiomeRandomProperty.singleton(Collections.emptyMap())).forGetter(PaletteConfig::exposedGroundReplacement),
            OldBiomeRandomProperty.wrappedCodec(Codec.unboundedMap(TagKey.codec(RegistryKeys.BLOCK), BlockTemplate.CODEC), "replacements").optionalFieldOf("buried_ground_replace", OldBiomeRandomProperty.singleton(Collections.emptyMap())).forGetter(PaletteConfig::buriedGroundReplacement),
            OldBiomeRandomProperty.wrappedCodec(Codec.unboundedMap(TagKey.codec(RegistryKeys.BLOCK), BlockTemplate.CODEC), "replacements").optionalFieldOf("extra_block_on_ground", OldBiomeRandomProperty.singleton(Collections.emptyMap())).forGetter(PaletteConfig::extraBlockGround),
            OldBiomeRandomProperty.wrappedCodec(Codec.unboundedMap(TagKey.codec(RegistryKeys.BLOCK), BlockTemplate.CODEC), "replacements").optionalFieldOf("extra_block_on_top", OldBiomeRandomProperty.singleton(Collections.emptyMap())).forGetter(PaletteConfig::extraBlockTop),
            Codec.BOOL.optionalFieldOf("reset_copied_ground_block_state", false).forGetter(PaletteConfig::resetCopiedGroundBlockState),
            Codec.BOOL.optionalFieldOf("auto_waterlog", false).forGetter(PaletteConfig::autoWaterlog)
        ).apply(instance, PaletteConfig::new));
    }
}
