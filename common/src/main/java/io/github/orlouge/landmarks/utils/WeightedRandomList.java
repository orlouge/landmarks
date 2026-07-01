package io.github.orlouge.landmarks.utils;

import net.minecraft.util.RandomSource;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class WeightedRandomList<T> implements Iterable<T> {
    private final TreeMap<Double, T> weightMap;

    private double totalWeight = 0;

    public WeightedRandomList() {
        this.weightMap = new TreeMap<>();
    }

    public WeightedRandomList(WeightedRandomList<T> copyFrom) {
        this.weightMap = (TreeMap<Double, T>) copyFrom.weightMap.clone();
        this.totalWeight = copyFrom.totalWeight;
    }

    public static <T> WeightedRandomList<T> singleton(T entry) {
        WeightedRandomList<T> list = new WeightedRandomList<>();
        list.add(1, entry);
        return list;
    }

    public void add(double weight, T element) {
        this.totalWeight += weight;
        weightMap.put(this.totalWeight, element);
    }

    public T sample(RandomSource random) {
        Map.Entry<Double, T> entry = weightMap.higherEntry(random.nextDouble() * this.totalWeight);
        return entry != null ? entry.getValue() : null;
    }

    public T popSample(RandomSource random) {
        Map.Entry<Double, T> sample;
        if (weightMap.size() == 1) {
            sample = weightMap.firstEntry();
        } else {
            sample = weightMap.higherEntry(random.nextDouble() * this.totalWeight);
        }
        if (sample != null) {
            Map.Entry<Double, T> previous = weightMap.lowerEntry(sample.getKey());
            double diff = previous == null ? sample.getKey() : sample.getKey() - previous.getKey();
            this.totalWeight -= diff;
            weightMap.remove(sample.getKey());
            for (Double entryWeight : weightMap.tailMap(sample.getKey(), false).keySet().stream().toList()) {
                T entry = weightMap.remove(entryWeight);
                weightMap.put(entryWeight - diff, entry);
            }
            return sample.getValue();
        } else {
            return null;
        }
    }

    public int size() {
        return weightMap.size();
    }

    @Override
    public Iterator<T> iterator() {
        return this.weightMap.values().iterator();
    }

    public double getTotalWeight() {
        return totalWeight;
    }
}
