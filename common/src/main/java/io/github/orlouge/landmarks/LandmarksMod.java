package io.github.orlouge.landmarks;

import io.github.orlouge.landmarks.features.SurfaceNoiseFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public final class LandmarksMod {
    public static final String MOD_ID = "landmarks";

    public static final Supplier<SurfaceNoiseFeature> SURFACE_NOISE_FEATURE = () -> new SurfaceNoiseFeature(SurfaceNoiseFeature.Config.CODEC.codec());

    public static final TagKey<Block> ROCKS_REPLACE_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "rocks_replace"));
    public static final TagKey<Block> ROCKS_PLACE_ON_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "rocks_place_on"));
    public static void init() {
    }
}
