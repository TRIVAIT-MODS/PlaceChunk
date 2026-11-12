package org.trivait.placechunk;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trivait.iba.api.IconButtons;
import org.trivait.placechunk.config.PlaceChunkConfig;
import org.trivait.placechunk.gui.PCCButton;

public class PlaceChunk implements ModInitializer {
	public static final String MOD_ID = "placechunk";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static PlaceChunkConfig CONFIG;

	@Override
	public void onInitialize() {
		AutoConfig.register(PlaceChunkConfig.class, GsonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(PlaceChunkConfig.class).getConfig();

		if (FabricLoader.getInstance().isModLoaded("iconbuttonsapi")) {
			IconButtons.addTitleScreenButton(new PCCButton(), () -> CONFIG.modEnabled);
		}

		ChunkReplacer.init();

		PlayerBlockBreakEvents.AFTER.register((level, player, blockPos, blockState, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer && CONFIG.modEnabled && CONFIG.onBreak) {
				if (ChunkReplacer.isValidTarget(serverLevel, blockPos, blockState)) {
					ChunkReplacer.replaceChunkAt(serverLevel, blockPos, blockState);
				}
			}
		});

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer && CONFIG.modEnabled && CONFIG.onPlace) {
				ItemStack stack = player.getItemInHand(hand);

				if (stack.getItem() instanceof BlockItem blockItem) {
					BlockPlaceContext context = new BlockPlaceContext(player, hand, stack, hitResult);
					if (context.canPlace()) {
						BlockState clickedState = serverLevel.getBlockState(hitResult.getBlockPos());
						boolean isInteractive = clickedState.useWithoutItem(serverLevel, player, hitResult).consumesAction();

						if (!isInteractive || player.isSecondaryUseActive()) {
							BlockState stateToPlace = blockItem.getBlock().getStateForPlacement(context);
							BlockPos targetPos = context.getClickedPos();

							if (ChunkReplacer.isValidTarget(serverLevel, targetPos, stateToPlace)
									&& stateToPlace.canSurvive(serverLevel, targetPos)
									&& serverLevel.isUnobstructed(stateToPlace, targetPos, CollisionContext.of(player))) {
								ChunkReplacer.replaceChunkAt(serverLevel, targetPos, stateToPlace);
							}
						}
					}
				}
			}
			return InteractionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer && CONFIG.modEnabled && CONFIG.onPlace) {
				ItemStack stack = player.getItemInHand(hand);

				if (stack.getItem() instanceof BucketItem && stack.getItem() != Items.BUCKET) {
					BlockHitResult hitResult = getPlayerPOVHitResult(serverLevel, player, ClipContext.Fluid.NONE);
					if (hitResult.getType() == HitResult.Type.BLOCK) {
						BlockState fluidState = getFluidBlockStateFromBucket(stack);
						if (fluidState != null) {
							BlockPos targetPos = hitResult.getBlockPos().relative(hitResult.getDirection());
							ChunkReplacer.replaceChunkAt(serverLevel, targetPos, fluidState);
						}
					}
				}
			}
			return InteractionResult.PASS;
		});
	}

	private static BlockState getFluidBlockStateFromBucket(ItemStack stack) {
		if (stack.is(Items.WATER_BUCKET)) {
			return Blocks.WATER.defaultBlockState();
		}
		if (stack.is(Items.LAVA_BUCKET)) {
			return Blocks.LAVA.defaultBlockState();
		}
		return null;
	}

	private static BlockHitResult getPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluidMode) {
		float xRot = player.getXRot();
		float yRot = player.getYRot();
		Vec3 eyePos = player.getEyePosition();
		float f2 = Mth.cos(-yRot * ((float) Math.PI / 180F) - (float) Math.PI);
		float f3 = Mth.sin(-yRot * ((float) Math.PI / 180F) - (float) Math.PI);
		float f4 = -Mth.cos(-xRot * ((float) Math.PI / 180F));
		float f5 = Mth.sin(-xRot * ((float) Math.PI / 180F));
		float f6 = f3 * f4;
		float f7 = f2 * f4;
		double reach = player.blockInteractionRange();
		Vec3 endPos = eyePos.add((double) f6 * reach, (double) f5 * reach, (double) f7 * reach);
		return level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, fluidMode, player));
	}
}
