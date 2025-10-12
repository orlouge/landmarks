package io.github.orlouge.landmarks.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.random.Random;

import java.util.List;

public record RandomProperty<T, C, S extends RandomProperty.ContextPredicate<C>>(List<WrappedEntry<T, S>> entries) {
    public SamplerCreator<T> withReplacement(C context, boolean forced) throws NoRandomMatchException {
        List<WrappedEntry<T, S>> validEntries = entries.stream().filter(
            entry -> entry.contextPredicate.test(context)
        ).toList();
        if (validEntries.isEmpty()) {
            if (forced) {
                System.out.println("Ignoring random constraints because there's no match.");
                validEntries = entries;
            } else {
                throw new NoRandomMatchException();
            }
        }
        List<WrappedEntry<T, S>> sampledEntries = validEntries;
        if (validEntries.size() == 1) return random -> () -> sampledEntries.getFirst().value;
        return random -> {
            WeightedRandomList<T> sampler = new WeightedRandomList<>();
            for (WrappedEntry<T, ? extends ContextPredicate<C>> entry : sampledEntries) {
                sampler.add(entry.weight, entry.value);
            }
            return (Sampler<T>) () -> sampler.popSample(random);
        };
    }

    public Sampler<T> sampler(Random random, C context, boolean forced) throws NoRandomMatchException {
        WeightedRandomList<T> sampler = new WeightedRandomList<>();
        for (WrappedEntry<T, ? extends ContextPredicate<C>> entry : entries) {
            if (entry.contextPredicate.test(context)) {
                sampler.add(entry.weight, entry.value);
            }
        }
        if (sampler.size() == 0) {
            if (forced) {
                System.out.println("Ignoring random constraints because there's no match.");
                for (WrappedEntry<T, S> entry : entries) {
                    sampler.add(entry.weight, entry.value);
                }
            } else {
                throw new NoRandomMatchException();
            }
        }
        if (sampler.size() == 1) {
            T entry = sampler.iterator().next();
            return () -> entry;
        }
        return () -> sampler.sample(random);
    }

    public T sample(Random random, C context) throws NoRandomMatchException {
        return sampler(random, context, false).sample();
    }

    public static <T, C, S extends ContextPredicate<C>> RandomProperty<T, C, S> singleton(T entry, S contextPredicate) {
        return new RandomProperty<>(List.of(new WrappedEntry<>(entry, contextPredicate)));
    }

    public static <T extends Entry<S>, C, S extends ContextPredicate<C>> Codec<RandomProperty<T, C, S>> flatCodec(Codec<T> entryCodecInList, Codec<T> entryCodec, S defaultContextPredicate) {
        return Codec.either(entryCodecInList.listOf(1, Integer.MAX_VALUE), entryCodec).xmap(
            either -> either.map(
                entries -> new RandomProperty<>(entries.stream().map(
                    entry -> new WrappedEntry<>(entry, entry.getWeight(), entry.getContextPredicate())
                ).toList()),
                entry -> new RandomProperty<>(List.of(new WrappedEntry<>(entry, defaultContextPredicate)))
            ),
            prop -> prop.entries.size() == 1 ? Either.right(prop.entries.getFirst().value)
                                                                  : Either.left(prop.entries.stream().map(WrappedEntry::value).toList())
        );
    }

    public static <T, C, S extends ContextPredicate<C>> Codec<RandomProperty<T, C, S>> wrappedCodec(Codec<T> entryCodec, MapCodec<S> contextPredicateCodec, S defaultContextPredicate, String valueKey) {
        Codec<WrappedEntry<T, S>> wrappedEntryCodec = WrappedEntry.codec(entryCodec, contextPredicateCodec, defaultContextPredicate, valueKey);
        return flatCodec(wrappedEntryCodec, wrappedEntryCodec, defaultContextPredicate).xmap(
            wrapped -> new RandomProperty<>(wrapped.entries.stream().map(WrappedEntry::value).toList()),
            unwrapped -> new RandomProperty<>(unwrapped.entries.stream().map(WrappedEntry::wrap).toList())
        );
    }


    public static <T, C, S extends ContextPredicate<C>> Codec<RandomProperty<T, C, S>> wrappedCodec(Codec<T> entryCodec, MapCodec<S> contextPredicateCodec, S defaultContextPredicate) {
        return wrappedCodec(entryCodec, contextPredicateCodec, defaultContextPredicate, "input");
    }

    public static <T, C, S extends ContextPredicate<C>> Codec<RandomProperty<T, C, S>> strictWrappedCodec(Codec<T> entryCodec, MapCodec<S> contextPredicateCodec, S defaultContextPredicate, String valueKey) {
        Codec<WrappedEntry<T, S>> listEntryCodec = WrappedEntry.strictCodec(entryCodec, contextPredicateCodec, valueKey);
        Codec<WrappedEntry<T, S>> flatEntryCodec = WrappedEntry.defaultCodec(entryCodec, defaultContextPredicate);
        return flatCodec(listEntryCodec, flatEntryCodec, defaultContextPredicate).xmap(
            wrapped -> new RandomProperty<>(wrapped.entries.stream().map(WrappedEntry::value).toList()),
            unwrapped -> new RandomProperty<>(unwrapped.entries.stream().map(WrappedEntry::wrap).toList())
        );
    }


    public static <T, C, S extends ContextPredicate<C>> Codec<RandomProperty<T, C, S>> strictWrappedCodec(Codec<T> entryCodec, MapCodec<S> contextPredicateCodec, S defaultContextPredicate) {
        return strictWrappedCodec(entryCodec, contextPredicateCodec, defaultContextPredicate, "input");
    }

    public static <T, C, S extends ContextPredicate<C>> Codec<RandomProperty<T, C, S>> extendCodec(MapCodec<T> entryCodec, MapCodec<S> contextPredicateCodec, S defaultContextPredicate) {
        Codec<WrappedEntry<T, S>> wrappedEntryCodec = WrappedEntry.extendCodec(entryCodec, contextPredicateCodec, defaultContextPredicate);
        return flatCodec(wrappedEntryCodec, wrappedEntryCodec, defaultContextPredicate).xmap(
            wrapped -> new RandomProperty<>(wrapped.entries.stream().map(WrappedEntry::value).toList()),
            unwrapped -> new RandomProperty<>(unwrapped.entries.stream().map(WrappedEntry::wrap).toList())
        );
    }

    public interface Entry<S> {
        double getWeight();
        S getContextPredicate();
    }

    public record WrappedEntry<T, S>(T value, double weight, S contextPredicate) implements Entry<S> {
        private static <T, S extends ContextPredicate<?>> Codec<WrappedEntry<T, S>> strictCodec(Codec<T> entryCodec, MapCodec<S> contextPredicateCodec, String valueKey) {
            return RecordCodecBuilder.create(instance -> instance.group(
                entryCodec.fieldOf(valueKey).forGetter(WrappedEntry::value),
                Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(WrappedEntry::weight),
                contextPredicateCodec.forGetter(WrappedEntry::contextPredicate)
            ).apply(instance, WrappedEntry::new));
        }

        private static <T, S extends ContextPredicate<?>> Codec<WrappedEntry<T, S>> defaultCodec(Codec<T> entryCodec, S defaultContextPredicate) {
            return entryCodec.xmap(
                unwrapped -> new WrappedEntry<>(unwrapped, defaultContextPredicate),
                wrapped -> wrapped.value
            );
        }

        public static <T, S extends ContextPredicate<?>> Codec<WrappedEntry<T, S>> codec(Codec<T> entryCodec, MapCodec<S> contextPredicateCodec, S defaultContextPredicate, String valueKey) {
            Codec<Either<WrappedEntry<T, S>, T>> eitherCodec = Codec.either(
                strictCodec(entryCodec, contextPredicateCodec, valueKey),
                entryCodec
            );
            return eitherCodec.xmap(
                either -> either.map(e -> e, e -> new WrappedEntry<>(e, defaultContextPredicate)),
                wrapped -> wrapped.weight == 1.0 && wrapped.contextPredicate.isDefault() ? Either.right(wrapped.value) : Either.left(wrapped)
            );
        }

        public static <T, C extends ContextPredicate<?>> Codec<WrappedEntry<T, C>> extendCodec(MapCodec<T> entryCodec, MapCodec<C> contextPredicateCodec, C defaultContextPredicate) {
            Codec<Either<WrappedEntry<T, C>, T>> eitherCodec = Codec.either(
                RecordCodecBuilder.create(instance -> instance.group(
                    entryCodec.forGetter(WrappedEntry::value),
                    Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(WrappedEntry::weight),
                    contextPredicateCodec.forGetter(WrappedEntry::contextPredicate)
                ).apply(instance, WrappedEntry::new)),
                entryCodec.codec()
            );
            return eitherCodec.xmap(
                either -> either.map(e -> e, e -> new WrappedEntry<>(e, defaultContextPredicate)),
                wrapped -> wrapped.weight == 1.0 && wrapped.contextPredicate.isDefault() ? Either.right(wrapped.value) : Either.left(wrapped)
            );
        }

        public WrappedEntry(T value, S predicate) {
            this(value, 1.0, predicate);
        }

        public static <T, C> WrappedEntry<WrappedEntry<T, C>, C> wrap(WrappedEntry<T, C> entry) {
            return new WrappedEntry<>(entry, entry.weight, entry.contextPredicate);
        }

        @Override
        public double getWeight() {
            return weight;
        }

        @Override
        public S getContextPredicate() {
            return contextPredicate;
        }
    }

    public interface SamplerCreator<T> {
        Sampler<T> sampler(Random random);
    }

    public interface Sampler<T> {
        T sample();
    }

    public interface ContextPredicate<C> {
        boolean isDefault();
        boolean test(C context);
    }

    public static class NoRandomMatchException extends Exception {
        public NoRandomMatchException() {}

        public NoRandomMatchException(String message) {
            super(message);
        }
    }
}
