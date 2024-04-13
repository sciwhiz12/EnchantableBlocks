package com.mrbysco.enchantableblocks.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class ExportingEnchantment extends AbstractBlockEnchantment {
	public ExportingEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot... equipmentSlots) {
		super(rarity, category, equipmentSlots);
	}

	public int getMinCost(int pEnchantmentLevel) {
		return 20;
	}

	public int getMaxCost(int pEnchantmentLevel) {
		return 55;
	}
}
