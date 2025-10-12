package io.github.orlouge.landmarks.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Optional;

public record OldBiomeRandomProperty<T>(List<WrappedEntry<T>> entries) {
    public SamplerCreator<T> withReplacement(RegistryEntry<Biome> biome, boolean forced) throws NoBiomeMatchException {
        List<WrappedEntry<T>> validEntries = entries.stream().filter(
            entry -> entry.biomes.isEmpty() || entry.biomes.get().contains(biome)
        ).toList();
        if (validEntries.isEmpty()) {
            if (forced) {
                System.out.println("Ignoring biome constraints because there's no match.");
                validEntries = entries;
            } else {
                throw new NoBiomeMatchException();
            }
        }
        List<WrappedEntry<T>> sampledEntries = validEntries;
        if (validEntries.size() == 1) return random -> () -> sampledEntries.getFirst().value;
        return random -> {
            WeightedRandomList<T> sampler = new WeightedRandomList<>();
            for (WrappedEntry<T> entry : sampledEntries) {
                sampler.add(entry.weight, entry.value);
            }
            return (Sampler<T>) () -> sampler.popSample(random);
        };
    }

    public Sampler<T> sampler(Random random, RegistryEntry<Biome> biome, boolean forced) throws NoBiomeMatchException {
        WeightedRandomList<T> sampler = new WeightedRandomList<>();
        for (WrappedEntry<T> entry : entries) {
            if (entry.biomes.isEmpty() || entry.biomes.get().contains(biome)) {
                sampler.add(entry.weight, entry.value);
            }
        }
        if (sampler.size() == 0) {
            if (forced) {
                System.out.println("Ignoring biome constraints because there's no match.");
                for (WrappedEntry<T> entry : entries) {
                    sampler.add(entry.weight, entry.value);
                }
            } else {
                throw new NoBiomeMatchException();
            }
        }
        if (sampler.size() == 1) {
            T entry = sampler.iterator().next();
            return () -> entry;
        }
        return () -> sampler.sample(random);
    }

    public static <U> OldBiomeRandomProperty<U> singleton(U entry) {
        return new OldBiomeRandomProperty<>(List.of(new WrappedEntry<>(entry)));
    }

    public T sample(Random random, RegistryEntry<Biome> biome) throws NoBiomeMatchException {
        return sampler(random, biome, false).sample();
    }

    public static <U extends Entry> Codec<OldBiomeRandomProperty<U>> flatCodec(Codec<U> entryCodec) {
        return Codec.either(entryCodec.listOf(1, Integer.MAX_VALUE), entryCodec).xmap(
            either -> either.map(
                entries -> new OldBiomeRandomProperty<>(entries.stream().map(
                    entry -> new WrappedEntry<>(entry, entry.getWeight(), entry.getBiomes())
                ).toList()),
                entry -> new OldBiomeRandomProperty<>(List.of(new WrappedEntry<>(entry, 1, Optional.empty())))
            ),
            prop -> prop.entries.size() == 1 ? Either.right(prop.entries.getFirst().value)
                                                                    : Either.left(prop.entries.stream().map(WrappedEntry::value).toList())
        );
    }

    public static <U> Codec<OldBiomeRandomProperty<U>> wrappedCodec(Codec<U> entryCodec, String valueKey) {
        return flatCodec(WrappedEntry.codec(entryCodec, valueKey)).xmap(
            wrapped -> new OldBiomeRandomProperty<>(wrapped.entries.stream().map(WrappedEntry::value).toList()),
            unwrapped -> new OldBiomeRandomProperty<>(unwrapped.entries.stream().map(WrappedEntry::wrap).toList())
        );
        /*
        return Codec.either(WrappedEntry.codec(entryCodec).listOf(1, Integer.MAX_VALUE), entryCodec).xmap(
            either -> either.map(
                BiomeRandomProperty::new,
                entry -> new BiomeRandomProperty<>(List.of(new WrappedEntry<>(entry, 1, Optional.empty())))
            ),
            prop -> prop.entries.size() == 1 ? Either.right(prop.entries.getFirst().input)
                                                                   : Either.left(prop.entries)
        );

         */
    }


    public static <U> Codec<OldBiomeRandomProperty<U>> wrappedCodec(Codec<U> entryCodec) {
        return wrappedCodec(entryCodec, "input");
    }


    public static <U> Codec<OldBiomeRandomProperty<U>> extendCodec(MapCodec<U> entryCodec) {
        return flatCodec(WrappedEntry.extendCodec(entryCodec)).xmap(
            wrapped -> new OldBiomeRandomProperty<>(wrapped.entries.stream().map(WrappedEntry::value).toList()),
            unwrapped -> new OldBiomeRandomProperty<>(unwrapped.entries.stream().map(WrappedEntry::wrap).toList())
        );
        /*
        return Codec.either(WrappedEntry.extendCodec(entryCodec).listOf(1, Integer.MAX_VALUE), entryCodec.codec()).xmap(
            either -> either.map(
                BiomeRandomProperty::new,
                entry -> new BiomeRandomProperty<>(List.of(new WrappedEntry<>(entry, 1, Optional.empty())))
            ),
            prop -> prop.entries.size() == 1 ? Either.right(prop.entries.getFirst().input)
                : Either.left(prop.entries)
        );
         */
    }

    public interface Entry {
        double getWeight();
        Optional<RegistryEntryList<Biome>> getBiomes();
    }

    public record WrappedEntry<T>(T value, double weight, Optional<RegistryEntryList<Biome>> biomes) implements Entry {
        public static <U> Codec<WrappedEntry<U>> codec(Codec<U> entryCodec, String valueKey) {
            Codec<Either<WrappedEntry<U>, U>> eitherCodec = Codec.either(
                RecordCodecBuilder.create(instance -> instance.group(
                    entryCodec.fieldOf(valueKey).forGetter(WrappedEntry::value),
                    Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(WrappedEntry::weight),
                    RegistryCodecs.entryList(RegistryKeys.BIOME).optionalFieldOf("contextPredicate").forGetter(WrappedEntry::biomes)
                ).apply(instance, WrappedEntry::new)),
                entryCodec
            );
            return eitherCodec.xmap(
                either -> either.map(e -> e, WrappedEntry::new),
                wrapped -> wrapped.weight == 1.0 && wrapped.biomes.isEmpty() ? Either.right(wrapped.value) : Either.left(wrapped)
            );
        }

        public static <U> Codec<WrappedEntry<U>> extendCodec(MapCodec<U> entryCodec) {
            Codec<Either<WrappedEntry<U>, U>> eitherCodec = Codec.either(
                RecordCodecBuilder.create(instance -> instance.group(
                    entryCodec.forGetter(WrappedEntry::value),
                    Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(WrappedEntry::weight),
                    RegistryCodecs.entryList(RegistryKeys.BIOME).optionalFieldOf("contextPredicate").forGetter(WrappedEntry::biomes)
                ).apply(instance, WrappedEntry::new)),
                entryCodec.codec()
            );
            return eitherCodec.xmap(
                either -> either.map(e -> e, WrappedEntry::new),
                wrapped -> wrapped.weight == 1.0 && wrapped.biomes.isEmpty() ? Either.right(wrapped.value) : Either.left(wrapped)
            );
        }

        public WrappedEntry(T value) {
            this(value, 1.0, Optional.empty());
        }

        public static <U> WrappedEntry<WrappedEntry<U>> wrap(WrappedEntry<U> entry) {
            return new WrappedEntry<>(entry, entry.weight, entry.biomes);
        }

        @Override
        public double getWeight() {
            return weight;
        }

        @Override
        public Optional<RegistryEntryList<Biome>> getBiomes() {
            return biomes;
        }
    }

    public interface SamplerCreator<T> {
        Sampler<T> sampler(Random random);
    }

    public interface Sampler<T> {
        T sample();
    }

    public static class NoBiomeMatchException extends Exception {
        public NoBiomeMatchException() {}

        public NoBiomeMatchException(String message) {
            super(message);
        }
    }
}
