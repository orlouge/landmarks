package io.github.orlouge.landmarks.features;

import io.github.orlouge.landmarks.utils.RandomProperty;
import io.github.orlouge.landmarks.utils.TopologicalSort;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;

import java.util.*;

public record VariantWithFragment<T extends VariantWithFragment.Fragment<T>>(
    Map<String, RandomProperty<String, VariantContext, VariantContext.Predicate>> subVariants,
    Optional<String> nameOverride,
    T fragment
) {
    static <T extends Fragment<T>> Map<Identifier, Map<String, List<RandomProperty.WrappedEntry<NamedVariant<T>, VariantContext.Predicate>>>> getChildVariants(
        Registry<RandomProperty.WrappedEntry<VariantWithFragment<T>, VariantContext.Predicate>> registry, T emptyFragment) {
    HashMap<Identifier, Map<String, List<RandomProperty.WrappedEntry<NamedVariant<T>, VariantContext.Predicate>>>> childVariants = new HashMap<>();
        for (Map.Entry<ResourceKey<RandomProperty.WrappedEntry<VariantWithFragment<T>, VariantContext.Predicate>>, RandomProperty.WrappedEntry<VariantWithFragment<T>, VariantContext.Predicate>> entry : registry.entrySet()) {
            Identifier id = entry.getKey().identifier();
            String[] parts = id.getPath().split("/");
            if (parts.length < 2) continue;
            StringBuilder parentPath = new StringBuilder(parts[0]);
            int i = 1;
            for (; i < parts.length - 2; i++) {
                parentPath.append("/").append(parts[i]);
            }
            Identifier parentId = Identifier.fromNamespaceAndPath(id.getNamespace(), parentPath.toString());
            if (parts.length == 2) {
                List<RandomProperty.WrappedEntry<NamedVariant<T>, VariantContext.Predicate>> entries =
                    childVariants.computeIfAbsent(parentId, k -> new HashMap<>())
                                 .computeIfAbsent(parts[i], k -> new ArrayList<>());
                entries.add(entry.getValue().map(variant -> new NamedVariant<>(variant.nameOverride.orElse("yes"), variant)));
                entries.add(new RandomProperty.WrappedEntry<>(
                    new NamedVariant<>("no", new VariantWithFragment<>(Collections.emptyMap(), Optional.empty(), emptyFragment)),
                    1e-8, true, new VariantContext.Predicate()
                ));
            } else {
                String variantType = parts[i++], variantName = parts[i];
                childVariants.computeIfAbsent(parentId, k -> new HashMap<>())
                    .computeIfAbsent(variantType, k -> new ArrayList<>())
                    .add(entry.getValue().map(variant -> new NamedVariant<>(variant.nameOverride.orElse(variantName), variant)));
            }
        }
        return childVariants;
    }

    static <T extends Fragment<T>> Pair<T, VariantContext> getAndMerge(
            List<Identifier> variants, T baseFragment, T emptyFragment, RandomSource random,
            VariantContext baseContext, RegistryAccess registryManager,
            ResourceKey<Registry<RandomProperty.WrappedEntry<VariantWithFragment<T>, VariantContext.Predicate>>> registryKey,
            Map<Identifier, Map<String, List<RandomProperty.WrappedEntry<NamedVariant<T>, VariantContext.Predicate>>>> childVariants) throws RandomProperty.NoRandomMatchException {
        Registry<RandomProperty.WrappedEntry<VariantWithFragment<T>, VariantContext.Predicate>> registry = registryManager.lookupOrThrow(registryKey);
        List<Pair<Identifier, VariantWithFragment<T>>> baseVariants = variants.stream().map(id -> new Pair<>(id, registry
            .getOptional(ResourceKey.create(registryKey, id))
            .map(RandomProperty.WrappedEntry::value)
            .orElse(new VariantWithFragment<>(Collections.emptyMap(), Optional.empty(), emptyFragment))
        )).toList();
        List<Set<String>> relevantTypes = new ArrayList<>();
        VariantSampler<T> sampler = new VariantSampler<>();

        for (Pair<Identifier, VariantWithFragment<T>> variant : baseVariants) {
            sampler.addUnconditionalSubvariants(variant.getSecond());
            Map<String, List<RandomProperty.WrappedEntry<NamedVariant<T>, VariantContext.Predicate>>> children =
                childVariants.getOrDefault(variant.getFirst(), Collections.emptyMap());
            for (Map.Entry<String, List<RandomProperty.WrappedEntry<NamedVariant<T>, VariantContext.Predicate>>> typeEntry : children.entrySet()) {
                sampler.addChoicesWithFragments(typeEntry.getKey(), new RandomProperty<>(typeEntry.getValue()));
            }
            relevantTypes.add(children.keySet());
        }

        VariantContext context = sampler.sampleContext(random, baseContext);
        T fragment = baseFragment;
        for (int i = 0; i < baseVariants.size(); i++) {
            fragment = fragment.merge(baseVariants.get(i).getSecond().fragment());
            fragment = sampler.mergeVariant(fragment, relevantTypes.get(i), context);
        }

        return new Pair<>(fragment, context);
    }

    public record NamedVariant<T extends Fragment<T>>(String name, VariantWithFragment<T> variant) {
    }

    public static class VariantSampler<T extends Fragment<T>> {
        private final Map<String, Set<String>> typeDeps = new HashMap<>();
        private final Map<String, RandomProperty<String, VariantContext, VariantContext.Predicate>> typeSamplers = new HashMap<>();
        private final Map<String, Map<String, T>> fragments = new HashMap<>();

        public void addChoicesWithFragments(String type, RandomProperty<NamedVariant<T>, VariantContext, VariantContext.Predicate> variants) {
            for (RandomProperty.WrappedEntry<NamedVariant<T>, VariantContext.Predicate> entry : variants.entries()) {
                VariantWithFragment<T> var = entry.value().variant();
                addChoiceWithFragment(type, new NamedVariant<>(entry.value().name, new VariantWithFragment<>(var.subVariants, var.nameOverride, var.fragment.addVariantCondition(type, entry.value().name))));
            }
            addRandomChoicesAndDependencies(type, variants.map(NamedVariant::name));
        }

        public void addChoiceWithFragment(String type, NamedVariant<T> variant) {
            for (Map.Entry<String, RandomProperty<String, VariantContext, VariantContext.Predicate>> entry : variant.variant.subVariants.entrySet()) {
                typeDeps.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(type);
                addRandomChoicesAndDependencies(entry.getKey(), entry.getValue().mapPredicates(p -> p.addVariantCondition(type, variant.name)));
            }
            fragments.computeIfAbsent(type, k -> new HashMap<>()).merge(variant.name, variant.variant().fragment, T::merge);
        }

        public void addUnconditionalSubvariants(VariantWithFragment<T> variant) {
            for (Map.Entry<String, RandomProperty<String, VariantContext, VariantContext.Predicate>> entry : variant.subVariants.entrySet()) {
                addRandomChoicesAndDependencies(entry.getKey(), entry.getValue());
            }
        }

        private void addRandomChoicesAndDependencies(String type, RandomProperty<String, VariantContext, VariantContext.Predicate> variant) {
            for (RandomProperty.WrappedEntry<String, VariantContext.Predicate> entry : variant.entries()) {
                for (String dep : entry.contextPredicate().variantsAny().map(Map::keySet).orElse(Collections.emptySet())) {
                    typeDeps.computeIfAbsent(type, k -> new HashSet<>()).add(dep);
                }
                for (String dep : entry.contextPredicate().variantsNone().map(Map::keySet).orElse(Collections.emptySet())) {
                    typeDeps.computeIfAbsent(type, k -> new HashSet<>()).add(dep);
                }
            }
            typeSamplers.merge(type, variant, RandomProperty::merge);
        }

    public VariantContext sampleContext(RandomSource random, VariantContext baseContext) throws RandomProperty.NoRandomMatchException {
            List<String> typeOrder = TopologicalSort.sort(typeSamplers.keySet(), v -> typeDeps.getOrDefault(v, Collections.emptySet()).stream().filter(typeSamplers::containsKey).toList());
            VariantContext context = baseContext.withVariants(new HashMap<>());
            for (String type : typeOrder) {
                RandomProperty<String, VariantContext, VariantContext.Predicate> randomVariant = typeSamplers.get(type);
                /*if (randomVariant == null) {
                }*/
                try {
                    String variant = randomVariant.sample(random, context);
                    if (context.variant().containsKey(type) && !context.variant().get(type).equals(variant)) {
                        throw new RuntimeException("Contradictory choices for variant type \"" + type + "\": \"" + variant + "\", \"" + context.variant().get(type));
                    }
                    context.variant().put(type, variant);
                } catch (RandomProperty.NoRandomMatchException e) {
                    throw new RandomProperty.NoRandomMatchException("Variant sampling error: " + type + ", order: " + String.join(",", typeOrder) + ", samplers: " + String.join(",", typeSamplers.keySet()));
                }
            }
            return context;
        }

        public T mergeVariant(T base, Set<String> types, VariantContext context) {
            T fragment = base;
            for (Map.Entry<String, Map<String, T>> entry : fragments.entrySet()) {
                if (!types.contains(entry.getKey())) continue;
                if (context.variant().containsKey(entry.getKey())) {
                    fragment = fragment.merge(entry.getValue().get(context.variant().get(entry.getKey())));
                }
            }
            return fragment;
        }
    }

    public interface Fragment<T extends Fragment<T>> {
        T merge(T fragment);

        T addVariantCondition(String type, String name);
    }
}
