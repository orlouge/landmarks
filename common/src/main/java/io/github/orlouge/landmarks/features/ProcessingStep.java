package io.github.orlouge.landmarks.features;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.LandmarksMod;
import io.github.orlouge.landmarks.density.BoundedFunction;
import io.github.orlouge.landmarks.generation.BlockTemplate;
import io.github.orlouge.landmarks.utils.RandomProperty;
import io.github.orlouge.landmarks.utils.RandomWrapper;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.*;
import java.util.function.BiFunction;

public record ProcessingStep(
    Parameter.Sampler mask,
    Map<String, Parameter.Sampler> localParameters,
    boolean xAscending,
    boolean yAscending,
    boolean zAscending,
    List<Rule> rules
) {

    public void process(StructureWorldAccess world, Random random, VariantContext context) {
        if (rules.isEmpty()) return;
        VariantContext localContext = context.withParameters(localParameters);

        Optional<BoundedFunction> maskBounds = mask.bounds();
        int minX = localContext.minPos().getX(), minY = localContext.minPos().getY(), minZ = localContext.minPos().getZ();
        int maxX = localContext.maxPos().getX(), maxY = localContext.maxPos().getY(), maxZ = localContext.maxPos().getZ();
        if (maskBounds.isPresent()) {
            minZ = Math.max(minZ, maskBounds.get().minZ());
            maxZ = Math.min(maxZ, maskBounds.get().maxZ());
            minX = Math.max(minX, maskBounds.get().minX());
            maxX = Math.min(maxX, maskBounds.get().maxX());
            minY = Math.max(minY, maskBounds.get().minY());
            maxY = Math.min(maxY, maskBounds.get().maxY());
        }

        for (int _z = 0; _z <= maxZ - minZ; _z++) {
            for (int _x = 0; _x <= maxX - minX; _x++) {
                for (int _y = 0; _y <= maxY - minY; _y++) {
                    int x = xAscending ? minX + _x : maxX - _x;
                    int y = yAscending ? minY + _y : maxY - _y;
                    int z = zAscending ? minZ + _z : maxZ - _z;
                    if (mask.sample(new DensityFunction.UnblendedNoisePos(x, y, z)) <= 0) continue;

                    BlockPos pos = new BlockPos(x, y, z);
                    DensityFunction.UnblendedNoisePos noisePos = new DensityFunction.UnblendedNoisePos(x,  y, z);
                    Map<String, BlockTemplate> copiedEntries = new HashMap<>();

                    BiFunction<String, Boolean, BlockTemplate> palette = (entry, strict) -> localContext.palette().get(entry, strict).map(
                        actualTemplate -> actualTemplate,
                        resolved -> copiedEntries.computeIfAbsent(entry, entry2 -> copyBlock(world, resolved, noisePos))
                    );
                    for (Rule rule : rules) {
                        if (rule.process(pos, world.getBlockState(pos).getRegistryEntry(), palette, world, random, localContext)) break;
                    }
                }
            }
        }
    }


    private static BlockTemplate copyBlock(StructureWorldAccess world, Palette.ResolvedCopiedEntry resolved, DensityFunction.UnblendedNoisePos noisePos) {
        int x = (int) (resolved.x().sample(noisePos));
        int y = (int) (resolved.y().sample(noisePos));
        int z = (int) (resolved.z().sample(noisePos));
        BlockState state = world.getBlockState(new BlockPos(x, y, z));
        if (!resolved.canCopy().contains(state.getRegistryEntry())) return BlockTemplate.empty();
        if (resolved.resetState()) state = state.getBlock().getDefaultState();
        return BlockTemplate.block(state);
    }

    public record RandomizedProcessingStep(
        Optional<Integer> index,
        RandomProperty<Either<String, Parameter>, VariantContext, VariantContext.Predicate> mask,
        Map<String, RandomProperty<Parameter, VariantContext, VariantContext.Predicate>> localParameters,
        List<RandomProperty<Rule.RandomizedRule, VariantContext, VariantContext.Predicate>> rules,
        RandomProperty<Boolean, VariantContext, VariantContext.Predicate> xAscending,
        RandomProperty<Boolean, VariantContext, VariantContext.Predicate> yAscending,
        RandomProperty<Boolean, VariantContext, VariantContext.Predicate> zAscending
    ) implements RandomWrapper<ProcessingStep, VariantContext> {

        public static final MapCodec<RandomizedProcessingStep> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("index").forGetter(RandomizedProcessingStep::index),
            VariantContext.wrappedRandomCodec(Parameter.CODEC_NAMED, "parameter")
                .optionalFieldOf("mask", VariantContext.defaultRandomProperty(Either.right(new Parameter.Constant(1)))).forGetter(RandomizedProcessingStep::mask),
            Codec.unboundedMap(Codec.STRING, VariantContext.wrappedRandomCodec(Parameter.CODEC)).optionalFieldOf("parameters", Collections.emptyMap()).forGetter(RandomizedProcessingStep::localParameters),
            VariantContext.strictWrappedRandomCodec(Rule.RandomizedRule.CODEC.codec(), "rule").listOf().fieldOf("rules").forGetter(RandomizedProcessingStep::rules),
            VariantContext.wrappedRandomCodec(Codec.BOOL).optionalFieldOf("x_ascending", VariantContext.defaultRandomProperty(true)).forGetter(RandomizedProcessingStep::xAscending),
            VariantContext.wrappedRandomCodec(Codec.BOOL).optionalFieldOf("y_ascending", VariantContext.defaultRandomProperty(true)).forGetter(RandomizedProcessingStep::yAscending),
            VariantContext.wrappedRandomCodec(Codec.BOOL).optionalFieldOf("z_ascending", VariantContext.defaultRandomProperty(true)).forGetter(RandomizedProcessingStep::zAscending)
        ).apply(instance, RandomizedProcessingStep::new));

        @Override
        public ProcessingStep sample(Random random, VariantContext context) throws RandomProperty.NoRandomMatchException {
            Map<String, Parameter.Sampler> localParameters = new HashMap<>();
            for (Map.Entry<String, RandomProperty<Parameter, VariantContext, VariantContext.Predicate>> entry : this.localParameters.entrySet()) {
                localParameters.put(entry.getKey(), entry.getValue().sample(random, context).createSampler(context.getVisitor()));
            }
            VariantContext localContext = context.withParameters(localParameters);

            List<Rule> rules = new ArrayList<>();
            for (RandomProperty<Rule.RandomizedRule, VariantContext, VariantContext.Predicate> rule : this.rules) {
                try {
                    rules.add(Rule.sampleAll(rule.sample(random, context), random, context));
                } catch (RandomProperty.NoRandomMatchException ignored) {

                }
            }

            Either<String, Parameter> mask = this.mask.sample(random, localContext);
            return new ProcessingStep(
                mask.map(
                    parRef -> localContext.userParameters().get(parRef),
                    par -> par.createSampler(localContext.getVisitor())
                    ),
                localParameters,
                xAscending.sample(random, context),
                yAscending.sample(random, context),
                zAscending.sample(random, context),
                rules
            );
        }
    }

    public record Rule(
        Either<BlockTemplate, Palette.ResolvedCopiedEntry> template,
        boolean postProcessing,
        boolean autoWaterlog,
        boolean passThroughIfNotReplaced,
        boolean impossible,
        Optional<RegistryEntryList<Block>> blockBelow,
        Optional<RegistryEntryList<Block>> blockBeingReplaced,
        Optional<RegistryEntryList<Block>> blockAbove,
        Either<TagKey<Block>, RegistryEntryList<Block>> cantReplace,
        List<Parameter.Condition> densityConditions
    ) {
        public static Rule sampleAll(RandomizedRule rule, Random random, VariantContext context) throws RandomProperty.NoRandomMatchException {
            Optional<RegistryEntryList<Block>> blockBelow = Optional.empty();
            Optional<RegistryEntryList<Block>> blockBeingReplaced = Optional.empty();
            Optional<RegistryEntryList<Block>> blockAbove = Optional.empty();
            Either<TagKey<Block>, RegistryEntryList<Block>> cantReplace;
            List<Parameter.Condition> conditions = new ArrayList<>();

            if (rule.blockBelow.isPresent()) blockBelow = Optional.of(rule.blockBelow.get().sample(random, context));
            if (rule.blockBeingReplaced.isPresent()) blockBeingReplaced = Optional.of(rule.blockBeingReplaced.get().sample(random, context));
            if (rule.blockAbove.isPresent()) blockAbove = Optional.of(rule.blockAbove.get().sample(random, context));

            if (rule.cantReplace.left().isPresent()) {
                cantReplace = Either.left(rule.cantReplace.left().get());
            } else {
                cantReplace = Either.right(rule.cantReplace().right().get().sample(random, context));
            }

            for (RandomProperty<String, VariantContext, VariantContext.Predicate> condition : rule.densityConditions.sample(random, context)) {
                conditions.add(Parameter.Condition.parse(condition.sample(random, context), context.userParameters()));
            }

            Either<BlockTemplate, Palette.ResolvedCopiedEntry> template = null;
            try {
                template = rule.template.sample(random, context).referenceOrTemplate().map(context.palette()::get, Either::left);
            } catch (RandomProperty.NoRandomMatchException ignored) {}

            return new Rule(
                template,
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

        public boolean process(BlockPos pos, RegistryEntry<Block> currentBlock, BiFunction<String, Boolean, BlockTemplate> palette, StructureWorldAccess world, Random random, VariantContext context) {
            if (impossible) return false;

            if (blockAbove.isPresent() && !blockAbove.get().contains(world.getBlockState(pos.add(0, 1, 0)).getRegistryEntry()))
                return false;
            if (blockBelow.isPresent() && !blockBelow.get().contains(world.getBlockState(pos.add(0, -1, 0)).getRegistryEntry()))
                return false;
            if (blockBeingReplaced.isPresent() && !blockBeingReplaced.get().contains(currentBlock))
                return false;
            if (cantReplace.map(currentBlock::isIn, list -> list.contains(currentBlock)))
                return false;

            DensityFunction.UnblendedNoisePos noisePos = new DensityFunction.UnblendedNoisePos(pos.getX(), pos.getY(), pos.getZ());

            for (Parameter.Condition condition : densityConditions) {
                if (!condition.test(noisePos)) return false;
            }

            BlockState blockToPlace = template == null ? null : template.map(
                template2 -> template2.getBlockState(world, random, palette),
                resolved -> copyBlock(world, resolved, noisePos).getBlockState(world, random, palette)
            );
            if (blockToPlace != null) {
                if (autoWaterlog && currentBlock.value() == Blocks.WATER && blockToPlace.contains(net.minecraft.state.property.Properties.WATERLOGGED)) {
                    blockToPlace = blockToPlace.with(Properties.WATERLOGGED, true);
                }

                if (!postProcessing) LandmarksMod.DISABLE_POST_PROCESSING_ONCE = true;

                world.setBlockState(pos, blockToPlace, 2);
                return true;
            } else return !passThroughIfNotReplaced;
        }

        public record RandomizedRule(
            RandomProperty<Palette.PalettedBlockTemplate, VariantContext, VariantContext.Predicate> template,
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
            public static final MapCodec<RandomizedRule> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                VariantContext.wrappedRandomCodec(Palette.PalettedBlockTemplate.CODEC, "block").optionalFieldOf("place",
                    VariantContext.defaultRandomProperty(new Palette.PalettedBlockTemplate(Either.right(BlockTemplate.empty())))).forGetter(RandomizedRule::template),
                Codec.BOOL.optionalFieldOf("post_processing", true).forGetter(RandomizedRule::postProcessing),
                Codec.BOOL.optionalFieldOf("auto_waterlog", false).forGetter(RandomizedRule::autoWaterlog),
                Codec.BOOL.optionalFieldOf("pass_through_if_not_replaced", false).forGetter(RandomizedRule::passThroughIfNotReplaced),
                Codec.BOOL.optionalFieldOf("impossible", false).forGetter(RandomizedRule::impossible),
                VariantContext.strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks").optionalFieldOf("block_below_is").forGetter(RandomizedRule::blockBelow),
                VariantContext.strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks").optionalFieldOf("block_is").forGetter(RandomizedRule::blockBeingReplaced),
                VariantContext.strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks").optionalFieldOf("block_above_is").forGetter(RandomizedRule::blockAbove),
                Codec.either(
                    TagKey.codec(RegistryKeys.BLOCK),
                    VariantContext.strictWrappedRandomCodec(RegistryCodecs.entryList(RegistryKeys.BLOCK), "blocks")
                ).optionalFieldOf("cant_replace", Either.left(BlockTags.FEATURES_CANNOT_REPLACE)).forGetter(RandomizedRule::cantReplace),
                VariantContext.strictWrappedRandomCodec(VariantContext.strictWrappedRandomCodec(Codec.STRING).listOf(), "conditions").optionalFieldOf(
                    "conditions", VariantContext.defaultRandomProperty(List.of())).forGetter(RandomizedRule::densityConditions)
            ).apply(instance, RandomizedRule::new));
        }
    }

}
