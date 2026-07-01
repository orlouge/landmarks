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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class LandmarksModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LandmarksMod.init();

        DynamicRegistries.register(Generator.RandomizedGeneratorConfig.REGISTRY_KEY, Generator.RandomizedGeneratorConfig.RANDOM_ENTRY_CODEC);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { Generator.RandomizedGeneratorConfig.childVariants = null; });

        Registry.register(BuiltInRegistries.FEATURE, id("noise"), LandmarksMod.SURFACE_NOISE_FEATURE.get());

        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("chamfer_distance_transform"), ChamferDistanceTransform.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("gaussian_blur_2d"), GaussianBlur.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("noise2d"), Noise2D.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("noise3d"), Noise3D.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("x"), X.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("y"), Y.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("z"), Z.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("swap_xy"), SwapXY.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("swap_yz"), SwapYZ.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("shift"), Shift.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("map"), Map.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("convolution"), Convolution.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("power"), Power.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("negate"), Negate.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("select"), Select.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("invert"), Invert.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_cache"), FeatureCache.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_max_y"), FeatureMaxY.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_min_y"), FeatureMinY.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_min_x"), FeatureMinX.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_min_z"), FeatureMinZ.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_max_x"), FeatureMaxX.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_max_z"), FeatureMaxZ.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_origin_x"), FeatureOriginX.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_origin_y"), FeatureOriginY.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_origin_z"), FeatureOriginZ.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_user_parameter"), FeatureUserParameter.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_mass"), FeatureMass.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_random_number"), FeatureRandomNumber.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_random_grid"), FeatureRandomGrid.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_block_matches"), FeatureBlockMatches.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("feature_biome_matches"), FeatureBiomeMatches.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("max_density_square"), MaxDensitySquare.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("cuboid"), Cuboid.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("cylinder"), Cylinder.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("sphere"), Sphere.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("add_along_y"), AddAlongY.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("mul_along_y"), MulAlongY.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("max_along_y"), MaxAlongY.CODEC_HOLDER.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("min_along_y"), MinAlongY.CODEC_HOLDER.codec());

        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, id("has_ocean_rocks_deep"))), GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
            ResourceKey.create(Registries.PLACED_FEATURE, id("ocean_rocks_deep"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, id("has_ocean_rocks_shallow"))), GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
            ResourceKey.create(Registries.PLACED_FEATURE, id("ocean_rocks_shallow"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, id("has_underwater_rocks"))), GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
            ResourceKey.create(Registries.PLACED_FEATURE, id("underwater_rocks"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, id("has_ocean_rocks_deep"))), GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
            ResourceKey.create(Registries.PLACED_FEATURE, id("thermal_vent"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, id("has_forest_clearing"))), GenerationStep.Decoration.LAKES,
            ResourceKey.create(Registries.PLACED_FEATURE, id("forest_clearing"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, id("has_rocks"))), GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
            ResourceKey.create(Registries.PLACED_FEATURE, id("rocks"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, id("has_ponds"))), GenerationStep.Decoration.LAKES,
            ResourceKey.create(Registries.PLACED_FEATURE, id("pond"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, id("has_sinkholes"))), GenerationStep.Decoration.LAKES,
            ResourceKey.create(Registries.PLACED_FEATURE, id("sinkhole"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, id("has_fairy_rings"))), GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
            ResourceKey.create(Registries.PLACED_FEATURE, id("fairy_ring"))
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(LandmarksMod.MOD_ID, path);
    }
}
