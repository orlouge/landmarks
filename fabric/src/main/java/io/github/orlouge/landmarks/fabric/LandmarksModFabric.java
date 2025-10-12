package io.github.orlouge.landmarks.fabric;

import io.github.orlouge.landmarks.density.*;
import net.fabricmc.api.ModInitializer;

import io.github.orlouge.landmarks.LandmarksMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;

public final class LandmarksModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LandmarksMod.init();

        Registry.register(Registries.FEATURE, Identifier.of(LandmarksMod.MOD_ID, "surface_noise"), LandmarksMod.SURFACE_NOISE_FEATURE.get());

        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "border_chamfer_distance"), BorderChamferDistance.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "border_blurred"), BorderBlurred.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "noise2d"), Noise2D.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "noise3d"), Noise3D.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "x"), X.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "y"), Y.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "z"), Z.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "shift"), Shift.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "convolution"), Convolution.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "power"), Power.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "negate"), Negate.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "select"), Select.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "invert"), Invert.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_cache"), FeatureCache.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_max_y"), FeatureMaxY.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_min_y"), FeatureMinY.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_min_x"), FeatureMinX.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_min_z"), FeatureMinZ.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_max_x"), FeatureMaxX.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_max_z"), FeatureMaxZ.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_surface_y"), FeatureSurfaceY.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_user_parameter"), FeatureUserParameter.CODEC_HOLDER.codec());

        /*
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_mossy_rocks"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "mossy_rocks"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_crystal_clump"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "crystal_clump"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_beach_rocks"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "beach_rocks"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_sandstone_rocks"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "sandstone_rocks"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_red_sandstone_rocks"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "red_sandstone_rocks"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_stone_rocks"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "stone_rocks"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_dirt_mound"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "dirt_mound"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_snow_ice_clump"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "snow_ice_clump"))
        );
         */
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_ocean_rocks_deep"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "ocean_rocks_deep"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_ocean_rocks_shallow"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "ocean_rocks_shallow"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_underwater_rocks"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "underwater_rocks"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_forest_clearing"))), GenerationStep.Feature.LAKES,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "forest_clearing"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_rocks"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "rocks"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_ponds"))), GenerationStep.Feature.LAKES,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "pond"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of(LandmarksMod.MOD_ID, "has_fairy_rings"))), GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(LandmarksMod.MOD_ID, "fairy_ring"))
        );
    }
}
