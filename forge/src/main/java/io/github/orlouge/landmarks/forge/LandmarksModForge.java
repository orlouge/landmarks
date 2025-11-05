package io.github.orlouge.landmarks.forge;

import io.github.orlouge.landmarks.density.algorithms.*;
import io.github.orlouge.landmarks.density.feature.*;
import io.github.orlouge.landmarks.density.feature.constants.*;
import io.github.orlouge.landmarks.density.operations.*;
import io.github.orlouge.landmarks.density.shape.Cuboid;
import io.github.orlouge.landmarks.density.shape.Cylinder;
import io.github.orlouge.landmarks.density.shape.Sphere;
import io.github.orlouge.landmarks.density.utils.*;
import io.github.orlouge.landmarks.features.Generator;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

import io.github.orlouge.landmarks.LandmarksMod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;

@Mod(LandmarksMod.MOD_ID)
public final class LandmarksModForge {
    /*
    public static final DeferredRegister<Feature<?>> FEATURE_REGISTER = DeferredRegister.create(
        Registries.FEATURE,
        LandmarksMod.MOD_ID
    );

    static {
        FEATURE_REGISTER.register("flat_ground_rocks", LandmarksMod.FLAT_GROUND_ROCKS_FEATURE);
    }
    */
    //    public static final Supplier<FlatGroundRocksFeature> FLAT_GROUND_ROCKS_FEATURE = FEATURE_REGISTER.register("flat_ground_rocks", LandmarksMod.FLAT_GROUND_ROCKS_FEATURE);

    public LandmarksModForge() {
        LandmarksMod.init();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerAll);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerDatapackRegisters);

        MinecraftForge.EVENT_BUS.addListener(this::registerWorldUnload);
    }

    public void registerWorldUnload(LevelEvent.Unload event) {
        Generator.RandomizedGeneratorConfig.childVariants = null;
    }

    public void registerDatapackRegisters(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Generator.RandomizedGeneratorConfig.REGISTRY_KEY, Generator.RandomizedGeneratorConfig.RANDOM_ENTRY_CODEC, null);
    }

    public void registerAll(RegisterEvent event) {
        event.register(
            RegistryKeys.FEATURE, registry -> {
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "noise"), LandmarksMod.SURFACE_NOISE_FEATURE.get());
            }
        );

        event.register(
            RegistryKeys.DENSITY_FUNCTION_TYPE, registry -> {
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "chamfer_distance_transform"), ChamferDistanceTransform.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "gaussian_blur_2d"), GaussianBlur.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "noise2d"), Noise2D.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "noise3d"), Noise3D.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "x"), X.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "y"), Y.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "z"), Z.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "swap_xy"), SwapXY.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "swap_yz"), SwapYZ.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "shift"), Shift.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "map"), Map.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "convolution"), Convolution.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "power"), Power.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "negate"), Negate.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "select"), Select.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "invert"), Invert.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_cache"), FeatureCache.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_max_y"), FeatureMaxY.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_min_y"), FeatureMinY.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_min_x"), FeatureMinX.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_min_z"), FeatureMinZ.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_max_x"), FeatureMaxX.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_max_z"), FeatureMaxZ.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_origin_x"), FeatureOriginX.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_origin_y"), FeatureOriginY.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_origin_z"), FeatureOriginZ.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_user_parameter"), FeatureUserParameter.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_mass"), FeatureMass.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_random_number"), FeatureRandomNumber.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_random_grid"), FeatureRandomGrid.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_block_matches"), FeatureBlockMatches.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_biome_matches"), FeatureBiomeMatches.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "max_density_square"), MaxDensitySquare.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "cuboid"), Cuboid.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "cylinder"), Cylinder.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "sphere"), Sphere.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "add_along_y"), AddAlongY.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "mul_along_y"), MulAlongY.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "max_along_y"), MaxAlongY.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "min_along_y"), MinAlongY.CODEC_HOLDER.codec());
            }
        );
    }
}
