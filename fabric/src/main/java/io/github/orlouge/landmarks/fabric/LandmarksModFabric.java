package io.github.orlouge.landmarks.fabric;

import io.github.orlouge.landmarks.density.algorithms.*;
import io.github.orlouge.landmarks.density.feature.*;
import io.github.orlouge.landmarks.density.feature.constants.*;
import io.github.orlouge.landmarks.density.operations.*;
import io.github.orlouge.landmarks.density.shape.*;
import io.github.orlouge.landmarks.density.utils.*;
import io.github.orlouge.landmarks.features.Generator;
import net.fabricmc.api.ModInitializer;

import io.github.orlouge.landmarks.LandmarksMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
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

        DynamicRegistries.register(Generator.RandomizedGeneratorConfig.REGISTRY_KEY, Generator.RandomizedGeneratorConfig.RANDOM_ENTRY_CODEC);
        ServerWorldEvents.UNLOAD.register((s, w) -> { Generator.RandomizedGeneratorConfig.childVariants = null; });

        Registry.register(Registries.FEATURE, Identifier.of(LandmarksMod.MOD_ID, "noise"), LandmarksMod.SURFACE_NOISE_FEATURE.get());

        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "chamfer_distance_transform"), ChamferDistanceTransform.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "gaussian_blur_2d"), GaussianBlur.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "noise2d"), Noise2D.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "noise3d"), Noise3D.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "x"), X.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "y"), Y.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "z"), Z.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "swap_xy"), SwapXY.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "swap_yz"), SwapYZ.CODEC_HOLDER.codec());
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
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_origin_x"), FeatureOriginX.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_origin_y"), FeatureOriginY.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_origin_z"), FeatureOriginZ.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_user_parameter"), FeatureUserParameter.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_mass"), FeatureMass.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_random_number"), FeatureRandomNumber.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_random_grid"), FeatureRandomGrid.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_block_matches"), FeatureBlockMatches.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "feature_biome_matches"), FeatureBiomeMatches.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "max_density_square"), MaxDensitySquare.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "cuboid"), Cuboid.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "cylinder"), Cylinder.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "sphere"), Sphere.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "add_along_y"), AddAlongY.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "mul_along_y"), MulAlongY.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "max_along_y"), MaxAlongY.CODEC_HOLDER.codec());
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of(LandmarksMod.MOD_ID, "min_along_y"), MinAlongY.CODEC_HOLDER.codec());

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
