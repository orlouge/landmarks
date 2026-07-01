package io.github.orlouge.landmarks.neoforge;

import io.github.orlouge.landmarks.density.algorithms.*;
import io.github.orlouge.landmarks.density.feature.*;
import io.github.orlouge.landmarks.density.feature.constants.*;
import io.github.orlouge.landmarks.density.operations.*;
import io.github.orlouge.landmarks.density.shape.Cuboid;
import io.github.orlouge.landmarks.density.shape.Cylinder;
import io.github.orlouge.landmarks.density.shape.Sphere;
import io.github.orlouge.landmarks.density.utils.*;
import io.github.orlouge.landmarks.features.Generator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import io.github.orlouge.landmarks.LandmarksMod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(LandmarksMod.MOD_ID)
public final class LandmarksModNeoForge {
    public LandmarksModNeoForge(IEventBus modBus) {
        LandmarksMod.init();

        modBus.addListener(this::registerAll);
        modBus.addListener(this::registerDatapackRegisters);

        NeoForge.EVENT_BUS.addListener(this::registerWorldUnload);
    }

    public void registerWorldUnload(LevelEvent.Unload event) {
        Generator.RandomizedGeneratorConfig.childVariants = null;
    }

    public void registerDatapackRegisters(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Generator.RandomizedGeneratorConfig.REGISTRY_KEY, Generator.RandomizedGeneratorConfig.RANDOM_ENTRY_CODEC, null);
    }

    public void registerAll(RegisterEvent event) {
        event.register(
            Registries.FEATURE, registry -> {
                registry.register(Identifier.fromNamespaceAndPath(LandmarksMod.MOD_ID, "noise"), LandmarksMod.SURFACE_NOISE_FEATURE.get());
            }
        );

        event.register(
            Registries.DENSITY_FUNCTION_TYPE, registry -> {
                registry.register(id("chamfer_distance_transform"), ChamferDistanceTransform.CODEC_HOLDER.codec());
                registry.register(id("gaussian_blur_2d"), GaussianBlur.CODEC_HOLDER.codec());
                registry.register(id("noise2d"), Noise2D.CODEC_HOLDER.codec());
                registry.register(id("noise3d"), Noise3D.CODEC_HOLDER.codec());
                registry.register(id("x"), X.CODEC_HOLDER.codec());
                registry.register(id("y"), Y.CODEC_HOLDER.codec());
                registry.register(id("z"), Z.CODEC_HOLDER.codec());
                registry.register(id("swap_xy"), SwapXY.CODEC_HOLDER.codec());
                registry.register(id("swap_yz"), SwapYZ.CODEC_HOLDER.codec());
                registry.register(id("shift"), Shift.CODEC_HOLDER.codec());
                registry.register(id("map"), Map.CODEC_HOLDER.codec());
                registry.register(id("convolution"), Convolution.CODEC_HOLDER.codec());
                registry.register(id("power"), Power.CODEC_HOLDER.codec());
                registry.register(id("negate"), Negate.CODEC_HOLDER.codec());
                registry.register(id("select"), Select.CODEC_HOLDER.codec());
                registry.register(id("invert"), Invert.CODEC_HOLDER.codec());
                registry.register(id("feature_cache"), FeatureCache.CODEC_HOLDER.codec());
                registry.register(id("feature_max_y"), FeatureMaxY.CODEC_HOLDER.codec());
                registry.register(id("feature_min_y"), FeatureMinY.CODEC_HOLDER.codec());
                registry.register(id("feature_min_x"), FeatureMinX.CODEC_HOLDER.codec());
                registry.register(id("feature_min_z"), FeatureMinZ.CODEC_HOLDER.codec());
                registry.register(id("feature_max_x"), FeatureMaxX.CODEC_HOLDER.codec());
                registry.register(id("feature_max_z"), FeatureMaxZ.CODEC_HOLDER.codec());
                registry.register(id("feature_origin_x"), FeatureOriginX.CODEC_HOLDER.codec());
                registry.register(id("feature_origin_y"), FeatureOriginY.CODEC_HOLDER.codec());
                registry.register(id("feature_origin_z"), FeatureOriginZ.CODEC_HOLDER.codec());
                registry.register(id("feature_user_parameter"), FeatureUserParameter.CODEC_HOLDER.codec());
                registry.register(id("feature_mass"), FeatureMass.CODEC_HOLDER.codec());
                registry.register(id("feature_random_number"), FeatureRandomNumber.CODEC_HOLDER.codec());
                registry.register(id("feature_random_grid"), FeatureRandomGrid.CODEC_HOLDER.codec());
                registry.register(id("feature_block_matches"), FeatureBlockMatches.CODEC_HOLDER.codec());
                registry.register(id("feature_biome_matches"), FeatureBiomeMatches.CODEC_HOLDER.codec());
                registry.register(id("max_density_square"), MaxDensitySquare.CODEC_HOLDER.codec());
                registry.register(id("cuboid"), Cuboid.CODEC_HOLDER.codec());
                registry.register(id("cylinder"), Cylinder.CODEC_HOLDER.codec());
                registry.register(id("sphere"), Sphere.CODEC_HOLDER.codec());
                registry.register(id("add_along_y"), AddAlongY.CODEC_HOLDER.codec());
                registry.register(id("mul_along_y"), MulAlongY.CODEC_HOLDER.codec());
                registry.register(id("max_along_y"), MaxAlongY.CODEC_HOLDER.codec());
                registry.register(id("min_along_y"), MinAlongY.CODEC_HOLDER.codec());
            }
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(LandmarksMod.MOD_ID, path);
    }
}
