package com.mrbysco.enchantableblocks.block;

import com.mojang.serialization.MapCodec;
import com.mrbysco.enchantableblocks.block.blockentity.EnchantedChestBlockEntity;
import com.mrbysco.enchantableblocks.block.blockentity.IEnchantable;
import com.mrbysco.enchantableblocks.registry.ModEnchantments;
import com.mrbysco.enchantableblocks.registry.ModMenus;
import com.mrbysco.enchantableblocks.registry.ModRegistry;
import com.mrbysco.enchantableblocks.util.EnchantmentUtil;
import com.mrbysco.enchantableblocks.util.MiscHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class EnchantedChestBlock extends AbstractChestBlock<EnchantedChestBlockEntity> implements SimpleWaterloggedBlock {
	public static final MapCodec<EnchantedChestBlock> CODEC = simpleCodec(p_304364_ -> new EnchantedChestBlock(p_304364_, ModRegistry.ENCHANTED_CHEST_BLOCK_ENTITY::get));

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
	private static final Component CONTAINER_TITLE = Component.translatable("container.chest");

	public EnchantedChestBlock(BlockBehaviour.Properties pProperties, Supplier<BlockEntityType<? extends EnchantedChestBlockEntity>> supplier) {
		super(pProperties, supplier);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
	}

	@Override
	protected MapCodec<? extends AbstractChestBlock<EnchantedChestBlockEntity>> codec() {
		return CODEC;
	}

	public EnchantedChestBlock(Properties properties) {
		super(properties, ModRegistry.ENCHANTED_CHEST_BLOCK_ENTITY::get);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnchantedChestBlockEntity(pos, state);
	}

	@Override
	public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(BlockState state, Level level, BlockPos pos, boolean override) {
		return DoubleBlockCombiner.Combiner::acceptNone;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState pState, Level level, BlockPos pos, Player player, BlockHitResult pHitResult) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		} else {
			BlockEntity blockentity = level.getBlockEntity(pos);
			if (blockentity instanceof EnchantedChestBlockEntity chestBlockEntity) {
				int enchantmentLevel = chestBlockEntity.getEnchantmentLevel(EnchantmentUtil.getEnchantmentHolder(blockentity, ModEnchantments.STORING));
				if (enchantmentLevel < 1) {
					player.openMenu(new SimpleMenuProvider((theContainerID, theInventory, thePlayer) ->
							ChestMenu.threeRows(theContainerID, theInventory, chestBlockEntity), CONTAINER_TITLE));
				} else if (enchantmentLevel == 1) {
					player.openMenu(new SimpleMenuProvider((theContainerID, theInventory, thePlayer) ->
							ModMenus.fourRows(theContainerID, theInventory, chestBlockEntity), CONTAINER_TITLE));
				} else if (enchantmentLevel == 2) {
					player.openMenu(new SimpleMenuProvider((theContainerID, theInventory, thePlayer) ->
							ModMenus.fiveRows(theContainerID, theInventory, chestBlockEntity), CONTAINER_TITLE));
				} else {
					player.openMenu(new SimpleMenuProvider((theContainerID, theInventory, thePlayer) ->
							ChestMenu.sixRows(theContainerID, theInventory, chestBlockEntity), CONTAINER_TITLE));
				}
				player.awardStat(getOpenChestStat());

				PiglinAi.angerNearbyPiglins(player, true);
			}

			return InteractionResult.CONSUME;
		}
	}

	protected Stat<ResourceLocation> getOpenChestStat() {
		return Stats.CUSTOM.get(Stats.OPEN_CHEST);
	}

	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return level.isClientSide ? createTickerHelper(blockEntityType, ModRegistry.ENCHANTED_CHEST_BLOCK_ENTITY.get(), EnchantedChestBlockEntity::lidAnimateTick) : null;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
	                              BlockPos currentPos, BlockPos facingPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
	}

	@Override
	protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
		return false;
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource randomSource) {
		BlockEntity blockentity = level.getBlockEntity(pos);
		if (blockentity instanceof EnchantedChestBlockEntity chestBlockEntity) {
			chestBlockEntity.recheckOpen();
		}
	}

	@Override
	public Item asItem() {
		return Items.CHEST;
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		float explosionResistance = super.getExplosionResistance(state, level, pos, explosion);
		BlockEntity blockentity = level.getBlockEntity(pos);
		if (blockentity instanceof IEnchantable enchantable) {
			Holder<Enchantment> blastHolder = EnchantmentUtil.getEnchantmentHolder(blockentity, Enchantments.BLAST_PROTECTION);
			if (enchantable.hasEnchantment(blastHolder)) {
				int enchantmentLevel = enchantable.getEnchantmentLevel(blastHolder);
				explosionResistance *= ((enchantmentLevel + 1) * 30);
			}
		}
		return explosionResistance;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		BlockEntity blockentity = params.getParameter(LootContextParams.BLOCK_ENTITY);
		if (blockentity instanceof IEnchantable enchantable) {
			if (enchantable.hasEnchantment(EnchantmentUtil.getEnchantmentHolder(blockentity, Enchantments.VANISHING_CURSE))) {
				return List.of();
			}
		}
		return super.getDrops(state, params);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			BlockEntity blockentity = level.getBlockEntity(pos);
			if (blockentity instanceof EnchantedChestBlockEntity craftingTableBlockEntity) {
				if (level instanceof ServerLevel) {
					if (!craftingTableBlockEntity.hasEnchantment(EnchantmentUtil.getEnchantmentHolder(blockentity, Enchantments.VANISHING_CURSE))) {
						for (int i = 0; i < craftingTableBlockEntity.handler.getSlots(); ++i) {
							MiscHelper.spawnItemStack(level, pos.getX(), pos.getY(), pos.getZ(), craftingTableBlockEntity.handler.getStackInSlot(i));
						}
					}
				}
			}

			super.onRemove(state, level, pos, newState, isMoving);
		}
	}
}
