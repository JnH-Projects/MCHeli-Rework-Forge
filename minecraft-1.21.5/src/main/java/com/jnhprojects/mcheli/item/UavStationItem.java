package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.entity.UavStationEntity;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The UAV station and the portable UAV controller, {@code MCH_ItemUavStation}. Both place the same
 * entity; the kind decides its model and which drones it launches.
 */
public class UavStationItem extends PlacedEntityItem {
    private final int kind;

    public UavStationItem(int kind, Properties properties) {
        super(ModEntities.UAV_STATION, properties);
        this.kind = kind;
    }

    /** Squared to the nearest quarter turn, facing the player, as the original placed it. */
    @Override
    protected void prepare(Entity entity, @Nullable Player player) {
        if (entity instanceof UavStationEntity station) {
            station.setKind(kind);
            float yaw = player != null ? player.getYRot() : 0.0F;
            int quarter = Math.floorMod(Math.round(Mth.wrapDegrees(yaw) / 90.0F), 4);
            station.setYRot(quarter * 90.0F - 180.0F);
            station.initOffset();
        }
    }
}
