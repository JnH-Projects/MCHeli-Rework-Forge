package com.jnhprojects.mcheli.weapon;

import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * One trigger pull, the port of {@code MCH_WeaponParam}. The weapon set fills in the aim and the
 * muzzle position; the client fills in the two option values (a locked target, a fire mode) and
 * the server receives them with the fire request.
 */
public final class Shot {
    /** The vehicle firing, or the player for hand held weapons. */
    public final Entity shooter;
    /** Who pulled the trigger. Damage and kills are credited to this entity. */
    public final @Nullable Entity user;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public float roll;
    public int option1;
    public int option2;
    public boolean infiniteAmmo;
    /** Set by the weapon set when the magazine ran dry with this shot. */
    public boolean reload;

    public Shot(Entity shooter, @Nullable Entity user) {
        this.shooter = shooter;
        this.user = user;
        this.x = shooter.getX();
        this.y = shooter.getY();
        this.z = shooter.getZ();
    }

    /** The entity whose view steers guided weapons: the user if there is one, else the shooter. */
    public Entity aimer() {
        return user != null ? user : shooter;
    }
}
