package io.github.orlouge.landmarks;

import io.github.orlouge.landmarks.features.SurfaceNoiseFeature;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public final class LandmarksMod {
    public static final String MOD_ID = "landmarks";

    public static final Supplier<SurfaceNoiseFeature> SURFACE_NOISE_FEATURE = () -> new SurfaceNoiseFeature(SurfaceNoiseFeature.Config.CODEC);

    public static final TagKey<Block> ROCKS_REPLACE_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "rocks_replace"));
    public static final TagKey<Block> ROCKS_PLACE_ON_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "rocks_place_on"));
    public static boolean DISABLE_POST_PROCESSING_ONCE = false;

    public static void init() {
    }
}
