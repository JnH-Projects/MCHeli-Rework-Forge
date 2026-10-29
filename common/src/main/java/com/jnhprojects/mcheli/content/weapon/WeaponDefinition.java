package com.jnhprojects.mcheli.content.weapon;

import com.jnhprojects.mcheli.content.Definition;
import com.jnhprojects.mcheli.content.InvalidDefinitionException;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.jnhprojects.mcheli.content.DefinitionValues.lower;
import static com.jnhprojects.mcheli.content.DefinitionValues.split;
import static com.jnhprojects.mcheli.content.DefinitionValues.toBool;
import static com.jnhprojects.mcheli.content.DefinitionValues.toFloat;
import static com.jnhprojects.mcheli.content.DefinitionValues.toInt;

/**
 * A weapon from {@code assets/mcheli/weapons}. Ported from {@code MCH_WeaponInfo}, keeping its
 * keys, defaults, ranges and the fix-ups of {@code checkData}.
 */
public final class WeaponDefinition extends Definition {
    public static final String FOLDER = "weapons";

    // Bits of Target, as the original packed them.
    public static final int TARGET_OTHERS = 1;
    public static final int TARGET_MONSTERS = 2;
    public static final int TARGET_PLAYERS = 4;
    public static final int TARGET_GROUND_VEHICLES = 8;
    public static final int TARGET_HELICOPTERS = 16;
    public static final int TARGET_PLANES = 32;
    public static final int TARGET_BLOCK = 64;

    public String displayName;
    public @Nullable WeaponType type;
    public String group = "";
    public int power;
    public float acceleration = 1.0F;
    public float accelerationInWater = 1.0F;
    public int explosion;
    public int explosionBlock = -1;
    public int explosionInWater;
    public int explosionAltitude;
    public int delayFuse;
    public float bound;
    public int timeFuse;
    public boolean flaming;
    public Sight sight = Sight.NONE;
    public float[] zoom = {1.0F};
    public int delay = 10;
    public int reloadTime = 30;
    public int round;
    public int suppliedNum = 1;
    public int maxAmmo;
    public final List<RoundItem> roundItems = new ArrayList<>();
    public int soundDelay;
    public float soundVolume = 1.0F;
    public float soundPitch = 1.0F;
    public float soundPitchRandom = 0.1F;
    public String sound;
    public int lockTime = 30;
    public boolean ridableOnly;
    public float proximityFuseDist;
    public int rigidityTime = 7;
    public float accuracy;
    public int bomblet;
    public int bombletSTime = 10;
    public float bombletDiff = 0.3F;
    public int modeNum;
    public int fixMode;
    public int piercing;
    public int heatCount;
    public int maxHeatCount;
    public boolean fae;
    public boolean guidedTorpedo;
    public float gravity;
    public float gravityInWater;
    public float velocityInWater = 0.999F;
    public boolean destruct;
    public String trajectoryParticle = "explode";
    public int trajectoryParticleStartTick;
    public boolean disableSmoke;
    public @Nullable Cartridge cartridge;
    public WeaponColor color = WeaponColor.WHITE;
    public WeaponColor colorInWater = WeaponColor.WHITE;
    public float smokeSize = 2.0F;
    public int smokeNum = 1;
    public int smokeMaxAge = 100;
    public String dispenseItem = "";
    public int dispenseDamage;
    public int dispenseRange = 1;
    public int recoilBufCount = 2;
    public int recoilBufCountSpeed = 3;
    public float length;
    public float radius;
    /** Half angle of the targeting pod cone, derived from {@link #length} and {@link #radius}. */
    public float angle;
    public boolean displayMortarDistance;
    public boolean fixCameraPitch;
    public float cameraRotationSpeedPitch = 1.0F;
    public int target = TARGET_OTHERS;
    public int markTime;
    public float recoil;
    public String bulletModel = "";
    public String bombletModel = "";
    /** {@code DamageFactor = kind, factor}, keyed by {@code player}, {@code helicopter}, {@code plane}, {@code tank} or {@code vehicle}. */
    public final Map<String, Float> damageFactors = new LinkedHashMap<>();
    public final List<MuzzleFlash> muzzleFlashes = new ArrayList<>();
    public final List<MuzzleFlash> muzzleSmokes = new ArrayList<>();

    public WeaponDefinition(String name) {
        super(name);
        this.displayName = name;
        this.sound = name + "_snd";
    }

    /** What the weapon sight does. {@code MoveSight} and {@code MissileSight} in the files. */
    public enum Sight {
        NONE, LOCK, ROCKET
    }

    @Override
    protected void registerKeys(KeyTable keys) {
        keys.put("DisplayName", v -> displayName = v.trim());
        keys.put("Type", v -> {
            type = WeaponType.of(v);
            if (type == WeaponType.BOMB || type == WeaponType.DISPENSER) {
                gravity = -0.03F;
                gravityInWater = -0.03F;
            }
        });
        keys.put("Group", v -> group = lower(v));
        keys.put("Power", v -> power = toInt(v));
        keys.put("Sound", v -> sound = lower(v));
        keys.put("Acceleration", v -> acceleration = toFloat(v, 0.0F, 100.0F));
        keys.put("AccelerationInWater", v -> accelerationInWater = toFloat(v, 0.0F, 100.0F));
        keys.put("Gravity", v -> gravity = toFloat(v, -50.0F, 50.0F));
        keys.put("GravityInWater", v -> gravityInWater = toFloat(v, -50.0F, 50.0F));
        keys.put("VelocityInWater", v -> velocityInWater = toFloat(v));
        keys.put("Explosion", v -> explosion = toInt(v, 0, 50));
        keys.put("ExplosionBlock", v -> explosionBlock = toInt(v, 0, 50));
        keys.put("ExplosionInWater", v -> explosionInWater = toInt(v, 0, 50));
        keys.put("ExplosionAltitude", v -> explosionAltitude = toInt(v, 0, 100));
        keys.put("TimeFuse", v -> timeFuse = toInt(v, 0, 100000));
        keys.put("DelayFuse", v -> delayFuse = toInt(v, 0, 100000));
        keys.put("Bound", v -> bound = toFloat(v, 0.0F, 100000.0F));
        keys.put("Flaming", v -> flaming = toBool(v));
        keys.put("DisplayMortarDistance", v -> displayMortarDistance = toBool(v));
        keys.put("FixCameraPitch", v -> fixCameraPitch = toBool(v));
        keys.put("CameraRotationSpeedPitch", v -> cameraRotationSpeedPitch = toFloat(v, 0.0F, 100.0F));
        keys.put("Sight", v -> {
            String s = lower(v);
            if (s.equals("movesight")) {
                sight = Sight.ROCKET;
            } else if (s.equals("missilesight")) {
                sight = Sight.LOCK;
            }
        });
        keys.put("Zoom", v -> {
            String[] s = split(v);
            if (s.length > 0) {
                zoom = new float[s.length];
                for (int i = 0; i < s.length; i++) {
                    zoom[i] = toFloat(s[i], 0.1F, 10.0F);
                }
            }
        });
        keys.put("Delay", v -> delay = toInt(v, 0, 100000));
        keys.put("ReloadTime", v -> reloadTime = toInt(v, 3, 1000));
        keys.put("Round", v -> round = toInt(v, 1, 30000));
        keys.put("MaxAmmo", v -> maxAmmo = toInt(v, 0, 30000));
        keys.put("SuppliedNum", v -> suppliedNum = toInt(v, 1, 30000));
        keys.put("Item", this::parseRoundItem);
        keys.put("SoundDelay", v -> soundDelay = toInt(v, 0, 1000));
        // The original read SoundPattern and then ignored it.
        keys.put("SoundPattern", v -> { });
        keys.put("SoundVolume", v -> soundVolume = toFloat(v, 0.0F, 1000.0F));
        keys.put("SoundPitch", v -> soundPitch = toFloat(v, 0.0F, 1.0F));
        keys.put("SoundPitchRandom", v -> soundPitchRandom = toFloat(v, 0.0F, 1.0F));
        keys.put("LockTime", v -> lockTime = toInt(v, 2, 1000));
        keys.put("RidableOnly", v -> ridableOnly = toBool(v));
        keys.put("ProximityFuseDist", v -> proximityFuseDist = toFloat(v, 0.0F, 2000.0F));
        keys.put("RigidityTime", v -> rigidityTime = toInt(v, 0, 1000000));
        keys.put("Accuracy", v -> accuracy = toFloat(v, 0.0F, 1000.0F));
        keys.put("Bomblet", v -> bomblet = toInt(v, 0, 1000));
        keys.put("BombletSTime", v -> bombletSTime = toInt(v, 0, 1000));
        keys.put("BombletDiff", v -> bombletDiff = toFloat(v, 0.0F, 1000.0F));
        keys.put("RecoilBufCount", v -> {
            String[] s = split(v);
            if (s.length >= 1) {
                recoilBufCount = toInt(s[0], 1, 10000);
            }
            if (s.length >= 2 && recoilBufCount > 2) {
                recoilBufCountSpeed = Math.min(toInt(s[1], 1, 10000) - 1, recoilBufCount / 2);
            }
        });
        keys.put("ModeNum", v -> modeNum = toInt(v, 0, 1000));
        keys.put("FixMode", v -> fixMode = toInt(v, 0, 10));
        keys.put("Piercing", v -> piercing = toInt(v, 0, 100000));
        keys.put("HeatCount", v -> heatCount = toInt(v, 0, 100000));
        keys.put("MaxHeatCount", v -> maxHeatCount = toInt(v, 0, 100000));
        keys.put("ModelBullet", v -> bulletModel = lower(v));
        keys.put("ModelBomblet", v -> bombletModel = lower(v));
        keys.put("FAE", v -> fae = toBool(v));
        keys.put("GuidedTorpedo", v -> guidedTorpedo = toBool(v));
        keys.put("Destruct", v -> destruct = toBool(v));
        keys.put("AddMuzzleFlash", this::parseMuzzleFlash);
        keys.put("AddMuzzleFlashSmoke", this::parseMuzzleSmoke);
        keys.put("TrajectoryParticle", v -> {
            trajectoryParticle = lower(v);
            if (trajectoryParticle.equals("none")) {
                trajectoryParticle = "";
            }
        });
        keys.put("TrajectoryParticleStartTick", v -> trajectoryParticleStartTick = toInt(v, 0, 10000));
        keys.put("DisableSmoke", v -> disableSmoke = toBool(v));
        keys.put("SetCartridge", this::parseCartridge);
        keys.putAliased("BulletColor", this::parseColor);
        keys.putAliased("BulletColorInWater", this::parseColor);
        keys.putAliased("SmokeColor", this::parseColor);
        keys.put("SmokeSize", v -> smokeSize = toFloat(v, 0.0F, 100.0F));
        keys.put("SmokeNum", v -> smokeNum = toInt(v, 1, 100));
        keys.put("SmokeMaxAge", v -> smokeMaxAge = toInt(v, 2, 1000));
        keys.put("DispenseItem", v -> {
            String[] s = split(v);
            if (s.length >= 2) {
                dispenseDamage = toInt(s[1], 0, 100000000);
            }
            dispenseItem = lower(s[0]);
        });
        keys.put("DispenseRange", v -> dispenseRange = toInt(v, 1, 100));
        keys.put("Length", v -> length = toInt(v, 1, 300));
        keys.put("Radius", v -> radius = toInt(v, 1, 1000));
        keys.put("Target", this::parseTarget);
        keys.put("MarkTime", v -> markTime = toInt(v, 1, 30000) + 1);
        keys.put("Recoil", v -> recoil = toFloat(v, 0.0F, 100.0F));
        keys.put("DamageFactor", this::parseDamageFactor);
    }

    private void parseRoundItem(String data) {
        String[] s = split(data);
        if (s.length >= 2 && !s[1].isEmpty() && roundItems.size() < 3) {
            int count = toInt(s[0], 1, 64);
            int damage = s.length >= 3 ? toInt(s[2], 0, 100000000) : 0;
            roundItems.add(new RoundItem(count, lower(s[1]), damage));
        }
    }

    private void parseMuzzleFlash(String data) {
        String[] s = split(data);
        if (s.length >= 7) {
            muzzleFlashes.add(new MuzzleFlash(toFloat(s[0]), toFloat(s[1]), 0.0F, toInt(s[2]),
                    color255(s[3], s[4], s[5], s[6]), 1));
        }
    }

    private void parseMuzzleSmoke(String data) {
        String[] s = split(data);
        if (s.length >= 9) {
            muzzleSmokes.add(new MuzzleFlash(toFloat(s[0]), toFloat(s[2]), toFloat(s[3]), toInt(s[4]),
                    color255(s[5], s[6], s[7], s[8]), toInt(s[1], 1, 1000)));
        }
    }

    private static WeaponColor color255(String a, String r, String g, String b) {
        return new WeaponColor(toFloat(a) / 255.0F, toFloat(r) / 255.0F, toFloat(g) / 255.0F, toFloat(b) / 255.0F);
    }

    private void parseCartridge(String data) {
        String[] s = split(data);
        if (s.length > 0 && !s[0].isEmpty()) {
            float acceleration = s.length >= 2 ? toFloat(s[1]) : 0.0F;
            float yaw = s.length >= 3 ? toFloat(s[2]) : 0.0F;
            float pitch = s.length >= 4 ? toFloat(s[3]) : 0.0F;
            float scale = s.length >= 5 ? toFloat(s[4]) : 1.0F;
            float gravity = s.length >= 6 ? toFloat(s[5]) : -0.04F;
            float bound = s.length >= 7 ? toFloat(s[6]) : 0.5F;
            cartridge = new Cartridge(lower(s[0]), acceleration, yaw, pitch, scale, gravity, bound);
        }
    }

    private void parseColor(String key, String data) {
        String[] s = split(data);
        if (s.length >= 4) {
            WeaponColor c = new WeaponColor(toInt(s[0], 0, 255) / 255.0F, toInt(s[1], 0, 255) / 255.0F,
                    toInt(s[2], 0, 255) / 255.0F, toInt(s[3], 0, 255) / 255.0F);
            if (key.equals("bulletcolorinwater")) {
                colorInWater = c;
            } else {
                color = c;
            }
        }
    }

    private void parseTarget(String data) {
        if (data.contains("block")) {
            target = TARGET_BLOCK;
            return;
        }
        target = 0;
        target |= data.contains("planes") ? TARGET_PLANES : 0;
        target |= data.contains("helicopters") ? TARGET_HELICOPTERS : 0;
        target |= data.contains("vehicles") || data.contains("tanks") ? TARGET_GROUND_VEHICLES : 0;
        target |= data.contains("players") ? TARGET_PLAYERS : 0;
        target |= data.contains("monsters") ? TARGET_MONSTERS : 0;
        target |= data.contains("others") ? TARGET_OTHERS : 0;
    }

    private void parseDamageFactor(String data) {
        String[] s = split(data);
        if (s.length < 2) {
            return;
        }
        String kind = switch (lower(s[0])) {
            case "player" -> "player";
            case "heli", "helicopter" -> "helicopter";
            case "plane" -> "plane";
            case "tank" -> "tank";
            case "vehicle" -> "vehicle";
            default -> null;
        };
        if (kind != null) {
            damageFactors.put(kind, toFloat(s[1], 0.0F, 1000000.0F));
        }
    }

    /** {@code checkData}. A file without a known {@code Type} is rejected, as the original ignored it. */
    @Override
    protected void validate() throws InvalidDefinitionException {
        if (type == null) {
            throw new InvalidDefinitionException("missing or unknown Type");
        }
        if (explosionBlock < 0) {
            explosionBlock = explosion;
        }
        if (fixMode >= modeNum) {
            fixMode = 0;
        }
        if (round <= 0 || round > maxAmmo) {
            round = maxAmmo;
        }
        if (explosion <= 0) {
            fae = false;
        }
        if (delayFuse <= 0) {
            bound = 0.0F;
        }
        if (fae) {
            explosionInWater = 0;
        }
        if (bomblet > 0 && bombletSTime < 1) {
            bombletSTime = 1;
        }
        if (destruct) {
            delay = 1000000;
        }
        angle = (float) Math.toDegrees(Math.atan2(radius, length));
    }

    /** {@code DamageFactor} for a target kind, 1 when the file sets none. */
    public float damageFactor(String kind) {
        Float f = damageFactors.get(kind);
        return f != null ? f : 1.0F;
    }
}
