package io.github.orlouge.landmarks.density;

import net.minecraft.world.level.levelgen.DensityFunction;

public interface FunctionWithCache extends DensityFunction {
    String key();
    FunctionWithCache setCache(Object cache);
    interface Simple extends FunctionWithCache {
        Object createCache(int minX, int maxX, int minY, int maxY, int minZ, int maxZ);
    }
}
