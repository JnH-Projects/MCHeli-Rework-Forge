package com.jnhprojects.mcheli.supply;

/**
 * How much fuel, ammunition and repair one supply step moves, after the original's
 * {@code updateFuel}, {@code supplyAmmoToOtherAircraft} and {@code repairOtherAircraft}. Kept apart
 * from the entities so the numbers can be checked without a game.
 */
public final class SupplyRules {
    /** The most a fuel slot pours into the tank per step, every half second. */
    public static final int FUEL_SLOT_POUR = 100;
    /** Ticks between two steps of the fuel slots: half a second. */
    public static final int FUEL_SLOT_INTERVAL = 10;
    /** Ticks between two rounds of resupply from an ammunition truck: two seconds. */
    public static final int AMMO_TRUCK_INTERVAL = 40;
    /** Ticks between two repairs from a repair vehicle: one second. */
    public static final int REPAIR_INTERVAL = 20;

    private SupplyRules() {
    }

    /**
     * Fuel one slot pours this step.
     *
     * @param missing  room left in the tank
     * @param contents fuel left in the can
     */
    public static int fuelFromCan(int missing, int contents) {
        return Math.max(Math.min(Math.min(missing, FUEL_SLOT_POUR), contents), 0);
    }

    /**
     * Rounds an ammunition truck adds to one weapon per step: a tenth of the weapon's
     * {@code MaxAmmo}, at least one, and never past the maximum.
     *
     * @param carried magazine and reserve together
     */
    public static int roundsFromTruck(int carried, int maxAmmo) {
        if (carried >= maxAmmo) {
            return 0;
        }
        return Math.min(Math.max(maxAmmo / 10, 1), maxAmmo - carried);
    }

    /** Hit points a repair vehicle gives back per step, never past the maximum. */
    public static int repair(int hp, int maxHp, int value) {
        return Math.max(Math.min(value, maxHp - hp), 0);
    }
}
