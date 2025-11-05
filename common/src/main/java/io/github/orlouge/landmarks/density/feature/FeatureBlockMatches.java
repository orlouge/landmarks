package io.github.orlouge.landmarks.density.feature;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;

import net.minecraft.state.property.Property;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record FeatureBlockMatches(
    List<Either<String, RegistryEntryList<Block>>> filter,
    List<Either<String, RegistryEntryList<Block>>> negativeFilter,
    StructureWorldAccess world,
    List<Either<BlockArgumentParser.BlockResult, RegistryEntryList<Block>>> filterCache,
    List<Either<BlockArgumentParser.BlockResult, RegistryEntryList<Block>>> negativeFilterCache
) implements DensityFunction.Base {
    public static final MapCodec<FeatureBlockMatches> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.either(Codec.STRING.comapFlatMap(s -> s.startsWith("#") ? DataResult.error(() -> "No tags allowed here.") : DataResult.success(s), s -> s), RegistryCodecs.entryList(RegistryKeys.BLOCK))
            .listOf().optionalFieldOf("matches", List.of()).forGetter(FeatureBlockMatches::filter),
        Codec.either(Codec.STRING.comapFlatMap(s -> s.startsWith("#") ? DataResult.error(() -> "No tags allowed here.") : DataResult.success(s), s -> s), RegistryCodecs.entryList(RegistryKeys.BLOCK))
            .listOf().optionalFieldOf("matches_not", List.of()).forGetter(FeatureBlockMatches::negativeFilter)
    ).apply(instance, FeatureBlockMatches::new));
    public static final CodecHolder<FeatureBlockMatches> CODEC_HOLDER = CodecHolder.of(CODEC);

    public FeatureBlockMatches(List<Either<String, RegistryEntryList<Block>>> filter, List<Either<String, RegistryEntryList<Block>>> negativeFilter) {
        this(filter, negativeFilter, null, null, null);
    }

    public FeatureBlockMatches create(StructureWorldAccess world) {
        RegistryWrapper<Block> commandRegistryWrapper = world.createCommandRegistryWrapper(RegistryKeys.BLOCK);
        Function<Either<String, RegistryEntryList<Block>>, Either<BlockArgumentParser.BlockResult, RegistryEntryList<Block>>> cache = e -> e.mapLeft(
            s -> {
                try {
                    return BlockArgumentParser.block(commandRegistryWrapper, s, false);
                } catch (CommandSyntaxException ex) {
                    throw new RuntimeException(ex);
                }
            }
        );
        return new FeatureBlockMatches(filter, negativeFilter, world, filter.stream().map(cache).toList(), negativeFilter.stream().map(cache).toList());
    }

    @Override
    public double sample(NoisePos pos) {
        if (this.world == null || this.filterCache == null || this.negativeFilterCache == null)
            throw new RuntimeException("Attempted to sample FeatureBlockMatches outside of the feature.");

        BlockState state = world.getBlockState(new BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()));

        boolean matched = false;
        filterloop:
        for (Either<BlockArgumentParser.BlockResult, RegistryEntryList<Block>> filter : this.filterCache) {
            if (filter.left().isPresent()) {
                BlockState requiredState = filter.left().get().blockState();
                if (!state.isOf(requiredState.getBlock())) continue;
                for (Map.Entry<Property<?>, Comparable<?>> property : filter.left().get().properties().entrySet()) {
                    if (!state.contains(property.getKey())) continue filterloop;
                    if (!state.get(property.getKey()).equals(property.getValue())) continue filterloop;
                }
            } else if (filter.right().isPresent()) {
                if (!filter.right().get().contains(state.getRegistryEntry())) continue;
            }
            matched = true;
        }
        if (!matched) return 0;

        for (Either<BlockArgumentParser.BlockResult, RegistryEntryList<Block>> filter : this.negativeFilterCache) {
            if (filter.left().isPresent()) {
                BlockState requiredState = filter.left().get().blockState();
                if (state.isOf(requiredState.getBlock())) return 0;
                boolean containsAll = true;
                for (Map.Entry<Property<?>, Comparable<?>> property : filter.left().get().properties().entrySet()) {
                    if (!state.contains(property.getKey()) || !state.get(property.getKey()).equals(property.getValue())) {
                        containsAll = false;
                        break;
                    }
                }
                if (containsAll) return 0;
            } else if (filter.right().isPresent()) {
                if (filter.right().get().contains(state.getRegistryEntry())) return 0;
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
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
