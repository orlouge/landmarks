package io.github.orlouge.landmarks.utils;

import net.minecraft.util.math.random.Random;

public interface RandomWrapper<T, C> {
    T sample(Random random, C context) throws RandomProperty.NoRandomMatchException;
}
