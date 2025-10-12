package io.github.orlouge.landmarks.neoforge;

import io.github.orlouge.landmarks.density.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import io.github.orlouge.landmarks.LandmarksMod;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(LandmarksMod.MOD_ID)
public final class LandmarksModNeoForge {
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

    public LandmarksModNeoForge(IEventBus modBus) {
        LandmarksMod.init();

        modBus.addListener(this::registerAll);
    }

    public void registerAll(RegisterEvent event) {
        event.register(
            RegistryKeys.FEATURE, registry -> {
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "surface_noise"), LandmarksMod.SURFACE_NOISE_FEATURE.get());
            }
        );

        event.register(
            RegistryKeys.DENSITY_FUNCTION_TYPE, registry -> {
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "border_chamfer_distance"), BorderChamferDistance.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "border_blurred"), BorderBlurred.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "noise2d"), Noise2D.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "noise3d"), Noise3D.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "x"), X.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "y"), Y.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "z"), Z.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "shift"), Shift.CODEC_HOLDER.codec());
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
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_surface_y"), FeatureSurfaceY.CODEC_HOLDER.codec());
                registry.register(Identifier.of(LandmarksMod.MOD_ID, "feature_user_parameter"), FeatureUserParameter.CODEC_HOLDER.codec());
            }
        );
    }
}
