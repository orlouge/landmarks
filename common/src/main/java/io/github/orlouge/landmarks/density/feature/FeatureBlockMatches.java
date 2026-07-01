package io.github.orlouge.landmarks.density.feature;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record FeatureBlockMatches(
    List<Either<String, HolderSet<Block>>> filter,
    List<Either<String, HolderSet<Block>>> negativeFilter,
    WorldGenLevel world,
    List<Either<BlockStateParser.BlockResult, HolderSet<Block>>> filterCache,
    List<Either<BlockStateParser.BlockResult, HolderSet<Block>>> negativeFilterCache
) implements DensityFunction.SimpleFunction {
    public static final MapCodec<FeatureBlockMatches> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.either(Codec.STRING.comapFlatMap(s -> s.startsWith("#") ? DataResult.error(() -> "No tags allowed here.") : DataResult.success(s), s -> s), RegistryCodecs.homogeneousList(Registries.BLOCK))
            .listOf().optionalFieldOf("matches", List.of()).forGetter(FeatureBlockMatches::filter),
        Codec.either(Codec.STRING.comapFlatMap(s -> s.startsWith("#") ? DataResult.error(() -> "No tags allowed here.") : DataResult.success(s), s -> s), RegistryCodecs.homogeneousList(Registries.BLOCK))
            .listOf().optionalFieldOf("matches_not", List.of()).forGetter(FeatureBlockMatches::negativeFilter)
    ).apply(instance, FeatureBlockMatches::new));
    public static final KeyDispatchDataCodec<FeatureBlockMatches> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    public FeatureBlockMatches(List<Either<String, HolderSet<Block>>> filter, List<Either<String, HolderSet<Block>>> negativeFilter) {
        this(filter, negativeFilter, null, null, null);
    }

    public FeatureBlockMatches create(WorldGenLevel world) {
        Function<Either<String, HolderSet<Block>>, Either<BlockStateParser.BlockResult, HolderSet<Block>>> cache = e -> e.mapLeft(
            s -> {
                try {
                    return BlockStateParser.parseForBlock(world.registryAccess().lookupOrThrow(Registries.BLOCK), s, false);
                } catch (CommandSyntaxException ex) {
                    throw new RuntimeException(ex);
                }
            }
        );
        return new FeatureBlockMatches(filter, negativeFilter, world, filter.stream().map(cache).toList(), negativeFilter.stream().map(cache).toList());
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        if (this.world == null || this.filterCache == null || this.negativeFilterCache == null)
            throw new RuntimeException("Attempted to sample FeatureBlockMatches outside of the feature.");

        BlockState state = world.getBlockState(new BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()));

        boolean matched = false;
        filterloop:
        for (Either<BlockStateParser.BlockResult, HolderSet<Block>> filter : this.filterCache) {
            if (filter.left().isPresent()) {
                BlockState requiredState = filter.left().get().blockState();
                if (!state.is(requiredState.getBlock())) continue;
                for (Map.Entry<Property<?>, Comparable<?>> property : filter.left().get().properties().entrySet()) {
                    if (!state.hasProperty(property.getKey())) continue filterloop;
                    if (!state.getValue(property.getKey()).equals(property.getValue())) continue filterloop;
                }
            } else if (filter.right().isPresent()) {
                if (!filter.right().get().contains(state.typeHolder())) continue;
            }
            matched = true;
        }
        if (!matched) return 0;

        for (Either<BlockStateParser.BlockResult, HolderSet<Block>> filter : this.negativeFilterCache) {
            if (filter.left().isPresent()) {
                BlockState requiredState = filter.left().get().blockState();
                if (state.is(requiredState.getBlock())) {
                    boolean containsAll = true;
                    for (Map.Entry<Property<?>, Comparable<?>> property : filter.left().get().properties().entrySet()) {
                        if (!state.hasProperty(property.getKey()) || !state.getValue(property.getKey()).equals(property.getValue())) {
                            containsAll = false;
                            break;
                        }
                    }
                    if (containsAll) return 0;
                }
            } else if (filter.right().isPresent()) {
                if (filter.right().get().contains(state.typeHolder())) return 0;
            }
        }
        return 1;
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return 1;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
