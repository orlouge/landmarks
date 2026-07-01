package io.github.orlouge.landmarks.utils;

import net.minecraft.util.RandomSource;

public interface RandomWrapper<T, C> {
    T sample(RandomSource random, C context) throws RandomProperty.NoRandomMatchException;
}
