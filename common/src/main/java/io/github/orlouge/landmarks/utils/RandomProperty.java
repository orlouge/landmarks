package io.github.orlouge.landmarks.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public record RandomProperty<T, C, S extends RandomProperty.ContextPredicate<C>>(List<WrappedEntry<T, S>> entries) implements RandomWrapper<T, C> {
    public SamplerCreator<T> withoutReplacement(C context, boolean forced, boolean sampleFallbackAfter) throws NoRandomMatchException {
        List<WrappedEntry<T, S>> validEntries = entries.stream().filter(
            entry -> !entry.fallback && entry.weight > 0 && entry.contextPredicate.test(context)
        ).toList();
        List<WrappedEntry<T, S>> fallbackEntries = entries.stream().filter(
            entry -> entry.fallback && entry.weight > 0 && entry.contextPredicate.test(context)
        ).toList();
        if (validEntries.isEmpty() && fallbackEntries.isEmpty()) {
            if (forced) {
                System.out.println("Ignoring random constraints because there's no match.");
                validEntries = entries.stream().filter(w -> !w.isFallback()).toList();
                fallbackEntries = entries.stream().filter(WrappedEntry::isFallback).toList();
            } else {
                throw new NoRandomMatchException();
            }
        }
        if (sampleFallbackAfter) {
            List<WrappedEntry<T, S>> sampledEntries = validEntries, sampledFallbackEntries = fallbackEntries;
            return random -> {
                WeightedRandomList<T> sampler = new WeightedRandomList<>(), fallbackSampler = new WeightedRandomList<>();
                for (WrappedEntry<T, ? extends ContextPredicate<C>> entry : sampledEntries) {
                    sampler.add(entry.weight, entry.value);
                }
                for (WrappedEntry<T, ? extends ContextPredicate<C>> entry : sampledFallbackEntries) {
                    fallbackSampler.add(entry.weight, entry.value);
                }
                return (Sampler<T>) () -> {
                    T sample = sampler.popSample(random);
                    if (sample == null) return fallbackSampler.popSample(random);
                    return sample;
                };
            };
        } else {
            List<WrappedEntry<T, S>> sampledEntries = !validEntries.isEmpty() ? validEntries : fallbackEntries;
            if (sampledEntries.size() == 1) {
                return random -> {
                    boolean[] sampled = new boolean[1];
                    return () -> {
                        if (sampled[0]) return null;
                        else {
                            sampled[0] = true;
                            return sampledEntries.getFirst().value;
                        }
                    };
                };
            }
            return random -> {
                WeightedRandomList<T> sampler = new WeightedRandomList<>();
                for (WrappedEntry<T, ? extends ContextPredicate<C>> entry : sampledEntries) {
                    sampler.add(entry.weight, entry.value);
                }
                return (Sampler<T>) () -> sampler.popSample(random);
            };
        }
    }

    public Sampler<T> sampler(Random random, C context, boolean forced) throws NoRandomMatchException {
        WeightedRandomList<T> regularSampler = new WeightedRandomList<>(), fallbackSampler = new WeightedRandomList<>();
        for (WrappedEntry<T, ? extends ContextPredicate<C>> entry : entries) {
            if (entry.weight > 0 && entry.contextPredicate.test(context)) {
                (entry.fallback ? fallbackSampler : regularSampler).add(entry.weight, entry.value);
            }
        }
        if (regularSampler.size() + fallbackSampler.size() == 0) {
            if (forced) {
                System.out.println("Ignoring random constraints because there's no match.");
                for (WrappedEntry<T, S> entry : entries) {
                    (entry.fallback ? fallbackSampler : regularSampler).add(entry.weight, entry.value);
                }
            } else {
                throw new NoRandomMatchException();
            }
        }
        WeightedRandomList<T> sampler = regularSampler.size() > 0 ? regularSampler : fallbackSampler;
        if (sampler.size() == 1) {
            T entry = sampler.iterator().next();
            return () -> entry;
        }
        return () -> sampler.sample(random);
    }

    @Override
    public T sample(Random random, C context) throws NoRandomMatchException {
        return sampler(random, context, false).sample();
    }

    public <U> RandomProperty<U, C, S> map(Function<T, U> fun) {
        return new RandomProperty<>(entries.stream().map(wrapped -> wrapped.map(fun)).toList());
    }

    public RandomProperty<T, C, S> mapPredicates(Function<S, S> fun) {
        return new RandomProperty<>(entries.stream().map(wrapped -> wrapped.mapPredicate(fun)).toList());
    }

    public RandomProperty<T, C, S> merge(RandomProperty<T, C, S> other) {
        return new RandomProperty<>(Stream.concat(entries.stream(), other.entries().stream()).toList());
    }

    public static <T, C, S extends ContextPredicate<C>> RandomProperty<T, C, S> singleton(T entry, S contextPredicate) {
        return new RandomProperty<>(List.of(new WrappedEntry<>(entry, contextPredicate)));
    }

    public static <T extends Entry<S>, C, S extends ContextPredicate<C>> Codec<RandomProperty<T, C, S>> flatCodec(Codec<T> entryCodecInList, Codec<T> entryCodec, S defaultContextPredicate) {
        return Codec.either(entryCodecInList.listOf(1, Integer.MAX_VALUE), entryCodec).xmap(
            either -> either.map(
                entries -> new RandomProperty<>(entries.stream().map(
                    entry -> new WrappedEntry<>(entry, entry.getWeight(), entry.isFallback(), entry.getContextPredicate())
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
        return wrappedCodec(entryCodec, contextPredicateCodec, defaultContextPredicate, "value");
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
        return strictWrappedCodec(entryCodec, contextPredicateCodec, defaultContextPredicate, "value");
    }

    public static <T, C, S extends ContextPredicate<C>> Codec<RandomProperty<T, C, S>> extendCodec(MapCodec<T> entryCodec, MapCodec<S> contextPredicateCodec, S defaultContextPredicate) {
        Codec<WrappedEntry<T, S>> wrappedEntryCodec = WrappedEntry.extendCodec(entryCodec, contextPredicateCodec);
        return flatCodec(wrappedEntryCodec, wrappedEntryCodec, defaultContextPredicate).xmap(
            wrapped -> new RandomProperty<>(wrapped.entries.stream().map(WrappedEntry::value).toList()),
            unwrapped -> new RandomProperty<>(unwrapped.entries.stream().map(WrappedEntry::wrap).toList())
        );
    }

    public interface Entry<S> {
        double getWeight();
        boolean isFallback();
        S getContextPredicate();
    }

    public record WrappedEntry<T, S>(T value, double weight, boolean fallback, S contextPredicate) implements Entry<S> {
        private static final Codec<Double> WEIGHT_CODEC = Codec.either(Codec.doubleRange(1e-8, 99999999.9999), Codec.doubleRange(0, 0)).xmap(
            e -> e.map(x -> x, x -> x), e -> e == 0 ? Either.right(e) : Either.left(0.0)
        );

        private static <T, S extends ContextPredicate<?>> Codec<WrappedEntry<T, S>> strictCodec(Codec<T> entryCodec, MapCodec<S> contextPredicateCodec, String valueKey) {
            return RecordCodecBuilder.create(instance -> instance.group(
                entryCodec.fieldOf(valueKey).forGetter(WrappedEntry::value),
                WEIGHT_CODEC.optionalFieldOf("weight", 1.0).forGetter(WrappedEntry::weight),
                Codec.BOOL.optionalFieldOf("fallback", false).forGetter(WrappedEntry::fallback),
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

        public static <T, C extends ContextPredicate<?>> Codec<WrappedEntry<T, C>> extendCodec(MapCodec<T> entryCodec, MapCodec<C> contextPredicateCodec) {
            Codec<WrappedEntry<T, C>> extendedCodec = RecordCodecBuilder.create(instance -> instance.group(
                entryCodec.forGetter(WrappedEntry::value),
                WEIGHT_CODEC.optionalFieldOf("weight", 1.0).forGetter(WrappedEntry::weight),
                Codec.BOOL.optionalFieldOf("fallback", false).forGetter(WrappedEntry::fallback),
                contextPredicateCodec.forGetter(WrappedEntry::contextPredicate)
            ).apply(instance, WrappedEntry::new));
            return extendedCodec;
            /*
            return Codec.either(extendedCodec, entryCodec.codec()).xmap(
                either -> either.map(e -> e, e -> new WrappedEntry<>(e, defaultContextPredicate)),
                wrapped -> wrapped.weight == 1.0 && wrapped.contextPredicate.isDefault() ? Either.right(wrapped.value) : Either.left(wrapped)
            );
             */
        }

        public WrappedEntry(T value, S predicate) {
            this(value, 1.0, false, predicate);
        }

        public static <T, C> WrappedEntry<WrappedEntry<T, C>, C> wrap(WrappedEntry<T, C> entry) {
            return new WrappedEntry<>(entry, entry.weight, entry.fallback, entry.contextPredicate);
        }

        public <U> WrappedEntry<U, S> map(Function<T, U> map) {
            return new WrappedEntry<>(map.apply(value), weight, fallback, contextPredicate);
        }

        public WrappedEntry<T, S> mapPredicate(Function<S, S> map) {
            return new WrappedEntry<>(value, weight, fallback, map.apply(contextPredicate));
        }

        @Override
        public double getWeight() {
            return weight;
        }

        @Override
        public boolean isFallback() {
            return fallback;
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
