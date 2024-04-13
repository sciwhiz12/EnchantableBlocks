package com.mrbysco.enchantableblocks.block.blockentity;

import com.mrbysco.enchantableblocks.registry.ModRegistry;
import com.mrbysco.enchantableblocks.util.TagHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class EnchantedEnchantmentTableBlockEntity extends EnchantmentTableBlockEntity implements IEnchantable {
	protected ListTag enchantmentTag = null;
	protected final Object2IntMap<Enchantment> enchantments = new Object2IntOpenHashMap<>();

	public EnchantedEnchantmentTableBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
	}

	@Override
	public BlockEntityType<?> getType() {
		return ModRegistry.ENCHANTED_ENCHANTING_TABLE_BLOCK_ENTITY.get();
	}

	@Override
	public Map<Enchantment, Integer> getEnchantments() {
		return enchantments;
	}

	@Override
	public boolean hasEnchantment(Enchantment enchantment) {
		return this.enchantments.containsKey(enchantment);
	}

	@Override
	public int getEnchantmentLevel(Enchantment enchantment) {
		if (this.hasEnchantment(enchantment))
			return this.enchantments.get(enchantment);
		return -1;
	}

	@Override
	public boolean hasEnchantment(TagKey<Enchantment> enchantmentTag) {
		for (Enchantment enchantment : this.enchantments.keySet()) {
			if (TagHelper.matchesTag(enchantment, enchantmentTag)) {
				return true;
			}
		}
		return this.enchantments.containsKey(enchantmentTag);
	}

	@Override
	public int getEnchantmentLevel(TagKey<Enchantment> enchantmentTag) {
		for (Enchantment enchantment : this.enchantments.keySet()) {
			if (TagHelper.matchesTag(enchantment, enchantmentTag)) {
				return this.enchantments.get(enchantment);
			}
		}
		return -1;
	}

	@Override
	public void setEnchantments(ListTag enchantmentTags) {
		this.enchantmentTag = enchantmentTags;
		this.updateEnchantmentMap();
	}

	@Override
	public ListTag getEnchantmentsTag() {
		return this.enchantmentTag;
	}

	@Override
	public void updateEnchantmentMap() {
		this.enchantments.clear();
		if (this.enchantmentTag != null) {
			EnchantmentHelper.deserializeEnchantments(this.enchantmentTag).forEach((enchantment, integer) -> {
				if (enchantment != null) {
					this.enchantments.put(enchantment, integer);
				}
			});
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.contains("Enchantments")) {
			this.enchantmentTag = tag.getList("Enchantments", Tag.TAG_COMPOUND);
			this.updateEnchantmentMap();
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (this.enchantmentTag != null)
			tag.put("Enchantments", enchantmentTag);
	}

	//Sync stuff
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
		if (packet.getTag() != null)
			load(packet.getTag());
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = new CompoundTag();
		saveAdditional(tag);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		super.handleUpdateTag(tag);
	}

	@Override
	public CompoundTag getPersistentData() {
		CompoundTag tag = new CompoundTag();
		this.saveAdditional(tag);
		return tag;
	}
}
