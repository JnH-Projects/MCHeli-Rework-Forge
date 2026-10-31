package com.jnhprojects.mcheli.supply;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupplyRulesTest {

    @Test
    void fuelSlotPoursAtMostOneHundred() {
        assertEquals(100, SupplyRules.fuelFromCan(450, 600));
        assertEquals(30, SupplyRules.fuelFromCan(30, 600));
        assertEquals(12, SupplyRules.fuelFromCan(450, 12));
        assertEquals(0, SupplyRules.fuelFromCan(0, 600));
        assertEquals(0, SupplyRules.fuelFromCan(450, 0));
    }

    @Test
    void ammunitionTruckAddsATenthOfTheMaximum() {
        assertEquals(40, SupplyRules.roundsFromTruck(0, 400));
        // Never past the maximum, and at least one round for small magazines.
        assertEquals(10, SupplyRules.roundsFromTruck(390, 400));
        assertEquals(1, SupplyRules.roundsFromTruck(2, 4));
        assertEquals(0, SupplyRules.roundsFromTruck(400, 400));
    }

    @Test
    void repairStopsAtFullHealth() {
        assertEquals(10, SupplyRules.repair(50, 100, 10));
        assertEquals(3, SupplyRules.repair(97, 100, 10));
        assertEquals(0, SupplyRules.repair(100, 100, 10));
    }
}
