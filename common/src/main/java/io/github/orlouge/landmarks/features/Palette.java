package io.github.orlouge.landmarks.features;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.orlouge.landmarks.generation.BlockTemplate;
import io.github.orlouge.landmarks.utils.RandomProperty;
import io.github.orlouge.landmarks.utils.RandomWrapper;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.*;
import java.util.stream.Collectors;

public record Palette(
    Map<String, BlockTemplate> entries,
    Map<String, ResolvedCopiedEntry> copiedEntries
) {
    public Palette() {
        this(Collections.emptyMap(), Collections.emptyMap());
    }

    public Palette merge(Palette other) {
        Map<String, BlockTemplate> entries = new HashMap<>(this.entries);
        Map<String, ResolvedCopiedEntry> copiedEntries = new HashMap<>(this.copiedEntries);
        entries.putAll(other.entries);
        copiedEntries.putAll(other.copiedEntries);
        return new Palette(entries, copiedEntries);
    }

    public boolean canCombineWithoutReplacement(Palette other) {
        return other.entries.keySet().stream().noneMatch(entries.keySet()::contains) && other.copiedEntries.keySet().stream().noneMatch(copiedEntries.keySet()::contains);
    }

    public Either<BlockTemplate, ResolvedCopiedEntry> get(String entry, boolean strict) {
        BlockTemplate template = entries.get(entry);
        if (template == null) {
            ResolvedCopiedEntry copied = copiedEntries.get(entry);
            if (copied == null) {
                if (strict) throw new RuntimeException("Missing palette entry: " + entry);
                return Either.left(BlockTemplate.empty());
            }
            return Either.right(copied);
        }
        return Either.left(template);
    }

    public Either<BlockTemplate, ResolvedCopiedEntry> get(String entry) {
        return this.get(entry, true);
    }

    public record CopiedEntry(
        Either<String, Parameter> x,
        Either<String, Parameter> y,
        Either<String, Parameter> z,
        RegistryEntryList<Block> canCopy,
        boolean resetState
    ) {
        public static final MapCodec<CopiedEntry> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Parameter.CODEC_NAMED.fieldOf("x").forGetter(CopiedEntry::x),
            Parameter.CODEC_NAMED.fieldOf("y").forGetter(CopiedEntry::y),
            Parameter.CODEC_NAMED.fieldOf("z").forGetter(CopiedEntry::z),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("can_copy").forGetter(CopiedEntry::canCopy),
            Codec.BOOL.fieldOf("reset_state").forGetter(CopiedEntry::resetState)
        ).apply(instance, CopiedEntry::new));

        public ResolvedCopiedEntry resolve(VariantContext context) {
            DensityFunction.DensityFunctionVisitor visitor = context.getVisitor();
            Parameter.Sampler x = this.x.map(context.userParameters()::get, par -> par.createSampler(visitor));
            Parameter.Sampler y = this.y.map(context.userParameters()::get, par -> par.createSampler(visitor));
            Parameter.Sampler z = this.z.map(context.userParameters()::get, par -> par.createSampler(visitor));
            return new ResolvedCopiedEntry(x, y, z, canCopy, resetState);
        }
    }

    public record ResolvedCopiedEntry(
        Parameter.Sampler x,
        Parameter.Sampler y,
        Parameter.Sampler z,
        RegistryEntryList<Block> canCopy,
        boolean resetState
    ) { }

    public record RandomizedPalette(
        Map<String, RandomProperty<PalettedBlockTemplate, VariantContext, VariantContext.Predicate>> entries,
        Map<String, RandomProperty<CopiedEntry, VariantContext, VariantContext.Predicate>> copyEntries,
        boolean overwritable
    ) implements RandomWrapper<Palette, VariantContext> {
        public static final MapCodec<RandomizedPalette> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, VariantContext.wrappedRandomCodec(PalettedBlockTemplate.CODEC, "block")).optionalFieldOf("entries", Collections.emptyMap()).forGetter(RandomizedPalette::entries),
            Codec.unboundedMap(Codec.STRING, VariantContext.extendRandomCodec(CopiedEntry.MAP_CODEC)).optionalFieldOf("copy_entries", Collections.emptyMap()).forGetter(RandomizedPalette::copyEntries),
            Codec.BOOL.optionalFieldOf("overwritable", false).forGetter(RandomizedPalette::overwritable)
            ).apply(instance, RandomizedPalette::new)
        );

        public RandomizedPalette() { this(new HashMap<>(), new HashMap<>(), true); }

        public RandomizedPalette merge(RandomizedPalette other) {
            HashMap<String, RandomProperty<PalettedBlockTemplate, VariantContext, VariantContext.Predicate>> entries = new HashMap<>(this.entries);
            HashMap<String, RandomProperty<CopiedEntry, VariantContext, VariantContext.Predicate>> copiedEntries = new HashMap<>(this.copyEntries);
            entries.putAll(overwritable ? other.entries : other.entries.entrySet().stream().filter(e -> !entries.containsKey(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            copiedEntries.putAll(overwritable ? other.copyEntries : other.copyEntries.entrySet().stream().filter(e -> !copyEntries.containsKey(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            return new RandomizedPalette(entries, copiedEntries, overwritable && other.overwritable);
        }

        public boolean canCombine(RandomizedPalette other) {
            return (overwritable || other.overwritable) || (other.entries.keySet().stream().noneMatch(entries.keySet()::contains) && other.copyEntries.keySet().stream().noneMatch(copyEntries.keySet()::contains));
        }

        @Override
        public Palette sample(Random random, VariantContext context) throws RandomProperty.NoRandomMatchException {
            Map<String, BlockTemplate> resolved = new HashMap<>();
            Map<String, String> unresolved = new HashMap<>();

            for (Map.Entry<String, RandomProperty<PalettedBlockTemplate, VariantContext, VariantContext.Predicate>> entry : entries.entrySet()) {
                try {
                    PalettedBlockTemplate sampled = entry.getValue().sample(random, context);
                    sampled.referenceOrTemplate.map(
                        str -> str.startsWith("%") ? resolved.put(entry.getKey(), BlockTemplate.parse("%" + str))
                            : unresolved.put(entry.getKey(), str),
                        template -> resolved.put(entry.getKey(), template.copy()));
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
                        it.remove();
                        resolved.put(entry.getKey(), resolved.get(reference));
                    } else if (unresolved.containsKey(reference)) {
                        unresolved.put(entry.getKey(), unresolved.get(reference));
                    } else {
                        it.remove();
                        resolved.put(entry.getKey(), (BlockTemplate.empty()));
                        resolved.put(reference, (BlockTemplate.empty()));
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
                " (generators: " + String.join(", ", context.variant().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList()) + ")");

            Map<String, ResolvedCopiedEntry> copied = new HashMap<>();
            for (Map.Entry<String, RandomProperty<CopiedEntry, VariantContext, VariantContext.Predicate>> entry : copyEntries.entrySet()) {
                copied.put("%" + entry.getKey(), (entry.getValue().sample(random, context).resolve(context)));
            }

            return new Palette(resolved, copied);
        }
    }

    public record PalettedBlockTemplate(Either<String, BlockTemplate> referenceOrTemplate) {
        public static final Codec<PalettedBlockTemplate> CODEC = Codec.either(
            Codec.STRING.comapFlatMap(
                str -> str.startsWith("%") && !str.contains(";") && !str.contains("*") && !str.contains("?") ? DataResult.success(str.substring(1)) : DataResult.<String>error(() -> "Palette references must begin with %."),
                ref -> "%" + ref
            ),
            BlockTemplate.CODEC
        ).xmap(PalettedBlockTemplate::new, PalettedBlockTemplate::referenceOrTemplate);
    }
}
