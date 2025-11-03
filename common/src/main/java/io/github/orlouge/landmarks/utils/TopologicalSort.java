package io.github.orlouge.landmarks.utils;

import java.util.*;
import java.util.function.Function;

public class TopologicalSort {

    public static <T> List<T> sort(Collection<T> allObjects, Function<T, Collection<T>> getDependencies) {
        Map<T, VisitState> visited = new HashMap<>();
        List<T> result = new ArrayList<>();

        for (T obj : allObjects) {
            if (visited.get(obj) == null) {
                dfs(obj, getDependencies, visited, result);
            }
        }

        return result;
    }

    private enum VisitState { VISITING, VISITED }

    private static <T> void dfs(
            T node,
            Function<T, Collection<T>> getDependencies,
            Map<T, VisitState> visited,
            List<T> result
    ) {
        VisitState state = visited.get(node);
        if (state == VisitState.VISITING) {
            throw new IllegalStateException("Cycle detected involving: " + node);
        }
        if (state == VisitState.VISITED) {
            return;
        }

        visited.put(node, VisitState.VISITING);
        for (T dep : getDependencies.apply(node)) {
            dfs(dep, getDependencies, visited, result);
        }
        visited.put(node, VisitState.VISITED);
        result.add(node);
    }
}
