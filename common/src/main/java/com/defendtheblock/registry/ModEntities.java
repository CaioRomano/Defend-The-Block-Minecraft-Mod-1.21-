package com.defendtheblock.registry;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.entity.projectile.WebShotEntity;
import com.defendtheblock.entity.turret.TurretEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {

    public static final EntityType<TurretEntity> ARROW_TURRET = DtbCompat.buildEntityType(
            TurretEntity::new, SpawnGroup.MISC, 0.9F, 1.4F, 10, "arrow_turret");

    public static final EntityType<WebShotEntity> WEB_SHOT = DtbCompat.buildEntityType(
            WebShotEntity::new, SpawnGroup.MISC, 0.3F, 0.3F, 6, "web_shot");

    private ModEntities() {
    }

    public static void register() {
        Registry.register(Registries.ENTITY_TYPE, DtbCompat.id("arrow_turret"), ARROW_TURRET);
        Registry.register(Registries.ENTITY_TYPE, DtbCompat.id("web_shot"), WEB_SHOT);
        FabricDefaultAttributeRegistry.register(ARROW_TURRET, TurretEntity.createTurretAttributes());
    }
}
