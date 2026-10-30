package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.weapon.Combat;
import com.jnhprojects.mcheli.weapon.Explosions;
import com.jnhprojects.mcheli.weapon.ModDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Everything a weapon fires: rounds, rockets, missiles, bombs and torpedoes. A port of
 * {@code MCH_EntityBaseBullet}.
 *
 * <p>Projectiles fly on both sides, the client only for looks; hits, fuses and guidance are decided
 * on the server. They are never saved: like the original, one found in a loaded chunk vanishes.
 */
public abstract class ProjectileEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_WEAPON = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BOMBLET = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    /** Beyond 5820 blocks from whoever fired it, a projectile is dropped. */
    private static final double MAX_RANGE_SQ = 3.38724E7;

    protected @Nullable Entity shooter;
    protected @Nullable Entity user;
    protected @Nullable Entity target;
    private @Nullable WeaponDefinition weapon;

    public int explosionPower;
    public int explosionPowerInWater;
    public int power = 1;
    public int piercing;
    public int delayFuse;
    public int sprinkleTime;
    public double acceleration = 1.0;
    public double accelerationFactor = 1.0;
    private int age;

    public Vec3 prevMotion = Vec3.ZERO;
    /** Position two ticks ago, used to spread trail particles over the distance covered. */
    protected double xo2;
    protected double yo2;
    protected double zo2;

    protected ProjectileEntity(EntityType<? extends ProjectileEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WEAPON, "");
        builder.define(DATA_TARGET, 0);
        builder.define(DATA_BOMBLET, false);
    }

    /**
     * The second constructor of the original: place the projectile and point it along
     * {@code (mx, my, mz)} at {@code acceleration} blocks per tick, capped at 3.9.
     */
    public void launch(double x, double y, double z, double mx, double my, double mz, float yaw, float pitch, double acceleration) {
        moveTo(x, y, z, yaw, pitch);
        xo2 = x;
        yo2 = y;
        zo2 = z;
        double a = Math.min(acceleration, 3.9);
        double d = Math.sqrt(mx * mx + my * my + mz * mz);
        Vec3 motion = d > 0.0 ? new Vec3(mx * a / d, my * a / d, mz * a / d) : Vec3.ZERO;
        setDeltaMovement(motion);
        prevMotion = motion;
        this.acceleration = a;
    }

    // Weapon

    public void setWeapon(String name) {
        WeaponDefinition def = MCHeli.content().weapon(name);
        if (def == null) {
            return;
        }
        weapon = def;
        if (!level().isClientSide()) {
            entityData.set(DATA_WEAPON, name);
        }
        if (def.bomblet > 0) {
            sprinkleTime = def.bombletSTime;
        }
        piercing = def.piercing;
        if (boostsAcceleration() && def.acceleration > 4.0F) {
            accelerationFactor = def.acceleration / 4.0F;
        }
    }

    /** Rounds and rockets faster than 4 blocks a tick move several steps per tick instead. */
    protected boolean boostsAcceleration() {
        return false;
    }

    public @Nullable WeaponDefinition weapon() {
        return weapon;
    }

    /** Model drawn when the weapon names none, one of the original's {@code MCH_DefaultBulletModels}. */
    public abstract String defaultModel();

    /** The model to draw: {@code ModelBomblet} for bomblets, else {@code ModelBullet}, else the default. */
    public String model() {
        if (weapon == null) {
            return defaultModel();
        }
        String name = isBomblet() ? weapon.bombletModel : weapon.bulletModel;
        return name.isEmpty() ? defaultModel() : name;
    }

    public String weaponName() {
        return entityData.get(DATA_WEAPON);
    }

    /** {@code setParameterFromWeapon}: power and charges from the weapon file, and who to credit. */
    public void setParameters(WeaponDefinition def, @Nullable Entity shooter, @Nullable Entity user) {
        explosionPower = def.explosion;
        explosionPowerInWater = def.explosionInWater;
        power = def.power;
        piercing = def.piercing;
        this.shooter = shooter;
        this.user = user;
    }

    /** The same, inherited from a parent projectile, for bomblets. */
    public void setParameters(ProjectileEntity parent) {
        explosionPower = parent.explosionPower;
        explosionPowerInWater = parent.explosionPowerInWater;
        power = parent.power;
        piercing = parent.piercing;
        shooter = parent.shooter;
        user = parent.user;
    }

    public void setOwners(@Nullable Entity shooter, @Nullable Entity user) {
        this.shooter = shooter;
        this.user = user;
    }

    public @Nullable Entity user() {
        return user;
    }

    public @Nullable Entity shooter() {
        return shooter;
    }

    public boolean isBomblet() {
        return entityData.get(DATA_BOMBLET);
    }

    public void setBomblet() {
        sprinkleTime = 0;
        entityData.set(DATA_BOMBLET, true);
    }

    public void setTarget(@Nullable Entity entity) {
        target = entity;
        if (!level().isClientSide()) {
            entityData.set(DATA_TARGET, entity != null ? entity.getId() : 0);
        }
    }

    public @Nullable Entity target() {
        return target;
    }

    /** A flare seduced this missile: it loses its target and is gone. */
    public void decoy() {
        target = null;
        discard();
    }

    public int age() {
        return age;
    }

    public float gravity() {
        return weapon != null ? weapon.gravity : 0.0F;
    }

    public float gravityInWater() {
        return weapon != null ? weapon.gravityInWater : 0.0F;
    }

    // Tick

    @Override
    public void tick() {
        if (level().isClientSide() && age == 0) {
            int targetId = entityData.get(DATA_TARGET);
            if (targetId > 0) {
                target = level().getEntity(targetId);
            }
        }
        Vec3 motion = getDeltaMovement();
        if (!motion.equals(prevMotion)) {
            faceMotion();
        }
        prevMotion = motion;
        age++;
        xo2 = xo;
        yo2 = yo;
        zo2 = zo;
        super.tick();

        if (weapon == null) {
            if (age >= 2) {
                discard();
                return;
            }
            setWeapon(weaponName());
            if (weapon == null) {
                return;
            }
        }

        if (!level().isClientSide()) {
            if (getY() <= level().getMaxBuildHeight() - 1 && !level().isLoaded(blockPosition())) {
                if (weapon.delayFuse <= 0) {
                    discard();
                    return;
                }
                if (delayFuse == 0) {
                    delayFuse = weapon.delayFuse;
                }
            }
            if (delayFuse > 0) {
                delayFuse--;
                if (delayFuse == 0) {
                    onTimeout();
                    discard();
                    return;
                }
            }
            if (!isValid()) {
                discard();
                return;
            }
            if (weapon.timeFuse > 0 && age > weapon.timeFuse) {
                onTimeout();
                discard();
                return;
            }
            if (weapon.explosionAltitude > 0 && isGroundWithin(weapon.explosionAltitude)) {
                impact(new Impact(position(), null, null, Direction.DOWN), 1.0F);
            }
        }

        motion = getDeltaMovement();
        setDeltaMovement(motion.x, motion.y + (isInWater() ? gravityInWater() : gravity()), motion.z);
        if (!isRemoved()) {
            checkCollisions();
        }
        motion = getDeltaMovement().scale(accelerationFactor);
        double nx = getX() + motion.x;
        double ny = getY() + motion.y;
        double nz = getZ() + motion.z;
        if (level().isClientSide()) {
            splash(nx, ny, nz);
        }
        if (isInWater()) {
            Vec3 m = getDeltaMovement();
            level().addParticle(ParticleTypes.BUBBLE, nx - m.x * 0.25, ny - m.y * 0.25, nz - m.z * 0.25, m.x, m.y, m.z);
        }
        setPos(nx, ny, nz);
    }

    /** Turns the nose along the flight path, the way every projectile class did after moving. */
    protected void faceMotion() {
        Vec3 m = getDeltaMovement();
        setYRot((float) Math.toDegrees(Math.atan2(m.z, m.x)) - 90.0F);
        setXRot(-(float) Math.toDegrees(Math.atan2(m.y, Math.sqrt(m.x * m.x + m.z * m.z))));
    }

    /** {@code setMotion}: flies straight along {@code (dx, dy, dz)} at full speed. */
    protected void aimAlong(double dx, double dy, double dz) {
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d > 0.0) {
            setDeltaMovement(dx * acceleration / d, dy * acceleration / d, dz * acceleration / d);
        }
    }

    /** {@code guidanceToTarget}: turns a seventh of the way towards the target each tick. */
    protected void guideTo(double tx, double ty, double tz) {
        double dx = tx - getX();
        double dy = ty - getY();
        double dz = tz - getZ();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d <= 0.0) {
            return;
        }
        Vec3 m = getDeltaMovement();
        setDeltaMovement((m.x * 6.0 + dx * acceleration / d) / 7.0,
                (m.y * 6.0 + dy * acceleration / d) / 7.0,
                (m.z * 6.0 + dz * acceleration / d) / 7.0);
        faceMotion();
    }

    /** {@code checkValid}: whoever fired it still exists and is not too far away. */
    private boolean isValid() {
        if (shooter == null && user == null) {
            return false;
        }
        if (user != null && user.isRemoved()) {
            return false;
        }
        Entity origin = user != null ? user : shooter;
        double dx = getX() - origin.getX();
        double dz = getZ() - origin.getZ();
        return dx * dx + dz * dz < MAX_RANGE_SQ;
    }

    private boolean isGroundWithin(int depth) {
        BlockPos.MutableBlockPos pos = blockPosition().mutable();
        for (int i = 0; i < depth; i++) {
            pos.move(Direction.DOWN);
            if (!level().getBlockState(pos).isAir()) {
                return true;
            }
        }
        return false;
    }

    /** Spawns {@code count} trail particles spread over the last two ticks of flight. */
    protected void trail(String name, int count, float size) {
        if (!level().isClientSide() || name.isEmpty() || count < 1 || count > 50) {
            return;
        }
        ParticleOptions particle = trailParticle(name);
        double x = (getX() - xo) / count;
        double y = (getY() - yo) / count;
        double z = (getZ() - zo) / count;
        double x2 = (xo - xo2) / count;
        double y2 = (yo - yo2) / count;
        double z2 = (zo - zo2) / count;
        for (int i = 0; i < count; i++) {
            double px = (xo + x * i + xo2 + x2 * i) / 2.0;
            double py = (yo + y * i + yo2 + y2 * i) / 2.0;
            double pz = (zo + z * i + zo2 + z2 * i) / 2.0;
            level().addAlwaysVisibleParticle(particle, true, px, py, pz, 0.0, 0.0, 0.0);
        }
    }

    /**
     * The original's own smoke particle ({@code explode}) was a large white puff; the cloud is the
     * closest vanilla one. Other names are 1.7.10 particle names.
     */
    private static ParticleOptions trailParticle(String name) {
        return switch (name) {
            case "explode", "cloud" -> ParticleTypes.CLOUD;
            case "flame" -> ParticleTypes.FLAME;
            case "largesmoke" -> ParticleTypes.LARGE_SMOKE;
            case "heart" -> ParticleTypes.HEART;
            case "fireworksspark" -> ParticleTypes.FIREWORK;
            default -> ParticleTypes.SMOKE;
        };
    }

    private void splash(double nx, double ny, double nz) {
        if (weapon == null || weapon.power <= 0) {
            return;
        }
        boolean wasDry = !level().getFluidState(BlockPos.containing(xo + 0.5, yo + 0.5, zo + 0.5)).is(FluidTags.WATER);
        if (wasDry && level().getFluidState(BlockPos.containing(nx + 0.5, ny + 0.5, nz + 0.5)).is(FluidTags.WATER)) {
            float pwr = Math.min(weapon.power, 20);
            int n = random.nextInt(1 + (int) pwr / 3) + (int) pwr / 2 + 1;
            for (int i = 0; i < n; i++) {
                level().addParticle(ParticleTypes.SPLASH, nx, ny + 0.5, nz,
                        pwr * 0.03 * (random.nextDouble() - 0.5) * 0.3,
                        pwr * 0.03 * (random.nextDouble() * 0.5 + 0.5) * 1.8,
                        pwr * 0.03 * (random.nextDouble() - 0.5) * 0.3);
            }
        }
    }

    /** {@code onUpdateTimeout}: the fuse ran out in mid air. */
    protected void onTimeout() {
        if (!(level() instanceof ServerLevel server) || weapon == null) {
            return;
        }
        if (isInWater()) {
            if (explosionPowerInWater > 0) {
                explode(server, getX(), getY(), getZ(), explosionPowerInWater, explosionPowerInWater, true);
            }
        } else if (explosionPower > 0) {
            explode(server, getX(), getY(), getZ(), explosionPower, weapon.explosionBlock, false);
        } else if (explosionPower < 0) {
            Explosions.playSound(level(), getX(), getY(), getZ());
        }
    }

    /** {@code onUpdateBomblet}: once the sprinkle timer runs out the carrier splits up. */
    protected void tickBomblets() {
        if (!level().isClientSide() && sprinkleTime > 0 && !isRemoved() && weapon != null) {
            sprinkleTime--;
            if (sprinkleTime == 0) {
                for (int i = 0; i < weapon.bomblet; i++) {
                    sprinkleBomblet();
                }
                discard();
            }
        }
    }

    protected void sprinkleBomblet() {
    }

    /** Creates a copy of this projectile's kind for a bomblet, flying from here. */
    protected <T extends ProjectileEntity> @Nullable T spawnChild(EntityType<T> type, double mx, double my, double mz, float yaw, float pitch) {
        T child = type.create(level());
        if (child == null) {
            return null;
        }
        child.launch(getX(), getY(), getZ(), mx, my, mz, yaw, pitch, acceleration);
        child.setWeapon(weaponName());
        child.setParameters(this);
        return child;
    }

    // Collision

    /** Where something was hit. {@code entity} is set for entity hits, {@code block} for block hits. */
    public record Impact(Vec3 location, @Nullable Entity entity, @Nullable BlockPos block, Direction side) {
    }

    /** {@code onUpdateCollided}: the swept ray of this tick against blocks, then entities. */
    protected void checkCollisions() {
        if (weapon == null) {
            return;
        }
        Vec3 motion = getDeltaMovement().scale(accelerationFactor);
        Vec3 from = position();
        Vec3 to = from.add(motion);
        BlockHitResult blockHit = null;
        for (int i = 0; i < 5; i++) {
            blockHit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.BLOCK) {
                blockHit = null;
                break;
            }
            BlockState state = level().getBlockState(blockHit.getBlockPos());
            if (user == null || level().isClientSide() || !Combat.isBulletBreakable(state)) {
                break;
            }
            level().destroyBlock(blockHit.getBlockPos(), true, user);
        }

        if (weapon.delayFuse > 0) {
            if (blockHit != null) {
                bounce(blockHit.getDirection());
                if (delayFuse == 0) {
                    delayFuse = weapon.delayFuse;
                }
            }
            return;
        }

        if (blockHit != null) {
            to = blockHit.getLocation();
        }
        Impact entityHit = findEntity(from, to, motion);
        if (entityHit != null) {
            // A vehicle applies the factor of the box struck itself, when it takes the damage.
            impact(entityHit, 1.0F);
        } else if (blockHit != null) {
            impact(new Impact(blockHit.getLocation(), null, blockHit.getBlockPos(), blockHit.getDirection()), 1.0F);
        }
    }

    private @Nullable Impact findEntity(Vec3 from, Vec3 to, Vec3 motion) {
        AABB area = getBoundingBox().expandTowards(motion).inflate(21.0);
        Entity best = null;
        Vec3 bestAt = null;
        double bestDistance = 0.0;
        List<AircraftEntity> clipped = new ArrayList<>();
        for (Entity candidate : level().getEntities(this, area, this::canHit)) {
            Optional<Vec3> at;
            if (candidate instanceof AircraftEntity vehicle) {
                at = vehicle.clipHitBoxes(from, to, 0.3);
                clipped.add(vehicle);
            } else {
                at = candidate.getBoundingBox().inflate(0.3).clip(from, to);
            }
            if (at.isPresent()) {
                double d = from.distanceTo(at.get());
                if (best == null || d < bestDistance) {
                    best = candidate;
                    bestAt = at.get();
                    bestDistance = d;
                }
            }
        }
        // Only the vehicle actually hit keeps the factor of the box the ray struck.
        for (AircraftEntity vehicle : clipped) {
            if (vehicle != best) {
                vehicle.consumeHitBoxFactor();
            }
        }
        return best != null ? new Impact(bestAt, best, null, Direction.UP) : null;
    }

    /** {@code canBeCollidedEntity}: never the shooter, its crew, or rounds fired by the same side. */
    public boolean canHit(Entity entity) {
        if (!entity.isPickable() || entity == user || entity == shooter) {
            return false;
        }
        if (entity instanceof ProjectileEntity other) {
            if (level().isClientSide()) {
                return false;
            }
            if (other.shooter != null && other.shooter == shooter || other.user != null && other.user == user) {
                return false;
            }
        }
        return !(shooter instanceof AircraftEntity vehicle) || !vehicle.hasPassenger(entity);
    }

    /** {@code boundBullet}: a delayed fuse round bounces off what it hits. */
    private void bounce(Direction side) {
        Vec3 m = getDeltaMovement();
        float bound = weapon != null ? weapon.bound : 0.0F;
        switch (side) {
            case DOWN -> {
                if (m.y > 0.0) {
                    setDeltaMovement(m.x, -m.y * bound, m.z);
                }
            }
            case UP -> {
                if (m.y < 0.0) {
                    setDeltaMovement(m.x, -m.y * bound, m.z);
                }
            }
            case NORTH -> {
                if (m.z > 0.0) {
                    setDeltaMovement(m.x, m.y, -m.z * bound);
                } else {
                    setPos(getX(), getY(), getZ() + m.z);
                }
            }
            case SOUTH -> {
                if (m.z < 0.0) {
                    setDeltaMovement(m.x, m.y, -m.z * bound);
                } else {
                    setPos(getX(), getY(), getZ() + m.z);
                }
            }
            case WEST -> {
                if (m.x > 0.0) {
                    setDeltaMovement(-m.x * bound, m.y, m.z);
                } else {
                    setPos(getX() + m.x, getY(), getZ());
                }
            }
            case EAST -> {
                if (m.x < 0.0) {
                    setDeltaMovement(-m.x * bound, m.y, m.z);
                } else {
                    setPos(getX() + m.x, getY(), getZ());
                }
            }
        }
    }

    /** {@code onImpact}: damage what was hit, then explode or pierce through. */
    protected void impact(Impact hit, float damageFactor) {
        if (!(level() instanceof ServerLevel server)) {
            if (weapon != null && (weapon.explosion == 0 || weapon.modeNum >= 2) && hit.block() != null) {
                debris(hit);
            }
            return;
        }
        if (weapon == null) {
            discard();
            return;
        }
        if (hit.entity() != null) {
            hitEntity(hit.entity(), damageFactor);
            piercing = 0;
        }
        float expPower = explosionPower * damageFactor;
        float expPowerInWater = explosionPowerInWater * damageFactor;
        Vec3 at = hit.location();
        if (piercing > 0) {
            piercing--;
            if (expPower > 0.0F) {
                explode(server, at.x, at.y, at.z, 1.0F, 1.0F, false);
            }
            return;
        }
        if (expPowerInWater == 0.0F) {
            if (weapon.fae) {
                explodeFae(server, expPower);
            } else if (expPower > 0.0F) {
                explode(server, at.x, at.y, at.z, expPower, weapon.explosionBlock, false);
            } else if (expPower < 0.0F) {
                Explosions.playSound(level(), getX(), getY(), getZ());
            }
        } else if (hit.entity() != null) {
            if (isInWater()) {
                explode(server, at.x, at.y, at.z, expPowerInWater, expPowerInWater, true);
            } else {
                explode(server, at.x, at.y, at.z, expPower, weapon.explosionBlock, false);
            }
        } else if (isInWater() || hit.block() != null && isWaterAround(hit.block())) {
            BlockPos b = hit.block() != null ? hit.block() : blockPosition();
            explode(server, b.getX(), b.getY(), b.getZ(), expPowerInWater, expPowerInWater, true);
        } else if (expPower > 0.0F) {
            explode(server, at.x, at.y, at.z, expPower, weapon.explosionBlock, false);
        } else if (expPower < 0.0F) {
            Explosions.playSound(level(), getX(), getY(), getZ());
        }
        discard();
    }

    /** {@code MCH_Lib.isBlockInWater}: water on any side of the block. */
    private boolean isWaterAround(BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (level().getFluidState(pos.relative(d)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return level().getFluidState(pos).is(FluidTags.WATER);
    }

    /** Client side chips flying off the block a round struck. */
    private void debris(Impact hit) {
        if (weapon == null || hit.block() == null) {
            return;
        }
        BlockState state = level().getBlockState(hit.block());
        if (state.isAir()) {
            return;
        }
        float p = weapon.power;
        Vec3 m = getDeltaMovement();
        var particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < p / 3.0F; i++) {
            level().addParticle(particle,
                    hit.location().x + (random.nextFloat() - 0.5) * p / 10.0,
                    hit.location().y + 0.1,
                    hit.location().z + (random.nextFloat() - 0.5) * p / 10.0,
                    -m.x * p / 2.0, p / 2.0F, -m.z * p / 2.0);
        }
    }

    /** {@code onImpactEntity}. */
    protected void hitEntity(Entity entity, float damageFactor) {
        if (!entity.isRemoved()) {
            DamageSource source = ModDamageTypes.bullet(level(), this, user);
            Combat.hurt(entity, source, power * damageFactor, weapon);
        }
        confirmHit();
    }

    /** {@code notifyHitBullet}: the shooter hears that a round connected. */
    protected void confirmHit() {
        if (user instanceof ServerPlayer player) {
            ModSounds.playTo(player, "hit", 1.0F, 1.0F);
        }
    }

    protected void explode(ServerLevel server, double x, double y, double z, float size, float blockSize, boolean inWater) {
        if (weapon == null) {
            return;
        }
        boolean destroyBlocks = !isBomblet() || random.nextInt(3) == 0;
        Explosions.Params p = new Explosions.Params(this, user, size, blockSize, weapon.flaming, destroyBlocks, 0, inWater, weapon);
        if (Explosions.explode(server, x, y, z, p)) {
            confirmHit();
        }
    }

    /** Fuel air explosives burn whatever they catch. */
    private void explodeFae(ServerLevel server, float size) {
        Explosions.Params p = new Explosions.Params(this, user, size, weapon.explosionBlock, weapon.flaming, true, 15, false, weapon);
        if (Explosions.explode(server, getX(), getY(), getZ(), p)) {
            confirmHit();
        }
    }

    // Entity plumbing

    @Override
    public boolean hurt(DamageSource source, float damage) {
        // Something thrown at a projectile sets it off, as in the original.
        if (damage > 0.0F && source.is(DamageTypes.THROWN) && !isRemoved()) {
            impact(new Impact(position().add(0.5, 0.5, 0.5), null, blockPosition(), Direction.UP), 1.0F);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = getBoundingBox().getSize() * 4.0 * 64.0;
        return distance < d * d;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("WeaponName", weaponName());
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** The yaw for this frame, as 26.x's {@code Entity#getYRot(float)} gives it. */
    public float getYRot(float partialTicks) {
        return partialTicks == 1.0F ? getYRot() : Mth.rotLerp(partialTicks, yRotO, getYRot());
    }

    /** The pitch for this frame, as 26.x's {@code Entity#getXRot(float)} gives it. */
    public float getXRot(float partialTicks) {
        return partialTicks == 1.0F ? getXRot() : Mth.lerp(partialTicks, xRotO, getXRot());
    }
}
