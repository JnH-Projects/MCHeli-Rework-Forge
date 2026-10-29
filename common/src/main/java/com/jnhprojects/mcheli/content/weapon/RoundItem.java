package com.jnhprojects.mcheli.content.weapon;

/**
 * {@code Item = count, name[, damage]}: what one resupply of the weapon costs. The name and
 * damage value are the 1.7.10 ones, as written in the file; they are mapped to current items when
 * the cost is paid.
 */
public record RoundItem(int count, String item, int damage) {
}
