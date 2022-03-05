package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.ScrollTypeConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.DeathInfoManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "copyFrom", at = @At(value = "TAIL"))
    private void onRespawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (alive) return;

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        UUID userId = player.getUuid();

        if (player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        // In case some items are by mistake placed in the inventory that should not be there
        player.getInventory().clear();
        for (YigdApi yigdApi : Yigd.apiMods) {
            yigdApi.dropAll(player);
        }

        List<Object> modSoulbounds = DeadPlayerData.Soulbound.getModdedSoulbound(userId);
        DefaultedList<ItemStack> soulboundItems = DeadPlayerData.Soulbound.getSoulboundInventory(userId);

        if (soulboundItems != null || modSoulbounds != null) {

            if (soulboundItems != null && soulboundItems.size() > 0) {
                List<ItemStack> armorInventory = soulboundItems.subList(36, 40);
                List<ItemStack> mainInventory = soulboundItems.subList(0, 36);

                for (ItemStack itemStack : armorInventory) {
                    EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(itemStack); // return EquipmentSlot

                    if (itemStack.isEmpty()) continue;

                    player.equipStack(equipmentSlot, itemStack);
                }

                player.equipStack(EquipmentSlot.OFFHAND, soulboundItems.get(40));

                PlayerInventory inventory = player.getInventory();
                for (int i = 0; i < Math.min(inventory.size(), mainInventory.size()); i++) { // Replace main inventory from grave
                    inventory.setStack(i, mainInventory.get(i));
                }

                if (soulboundItems.size() > 41) {
                    for (int i = 41; i < Math.min(soulboundItems.size(), inventory.size()); i++) {
                        inventory.setStack(i, soulboundItems.get(i));
                    }
                }
            }

            // Modded soulbound doesn't work without this because of cardinal components
            Yigd.NEXT_TICK.add(() -> {
                if (modSoulbounds != null && modSoulbounds.size() > 0) {
                    for (int i = 0; i < Math.min(Yigd.apiMods.size(), modSoulbounds.size()); i++) {
                        YigdApi yigdApi = Yigd.apiMods.get(i);
                        Object modSoulbound = modSoulbounds.get(i);

                        yigdApi.setInventory(modSoulbound, player);
                    }
                }
            });

            DeadPlayerData.Soulbound.dropModdedSoulbound(userId);
            DeadPlayerData.Soulbound.dropSoulbound(userId);
        }

        try {
            List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);
            if (deadPlayerData != null && deadPlayerData.size() > 0) {
                DeadPlayerData latestDeath = deadPlayerData.get(deadPlayerData.size() - 1);
                BlockPos deathPos = latestDeath.gravePos;

                giveScroll(player, deathPos);

                if (deathPos != null && YigdConfig.getConfig().graveSettings.tellDeathPos) {
                    player.sendMessage(new TranslatableText("text.yigd.message.grave_location_info", deathPos.getX(), deathPos.getY(), deathPos.getZ(), latestDeath.dimensionName), false);
                }
            }
        }
        catch (Exception e) {
            Yigd.LOGGER.warn("Death data did not generate\n" + e);
        }
    }

    private void giveScroll(PlayerEntity player, BlockPos refPos) {
        YigdConfig.UtilitySettings utilityConfig = YigdConfig.getConfig().utilitySettings;
        if (utilityConfig.scrollItem.scrollType == ScrollTypeConfig.DISABLED || !utilityConfig.scrollItem.retrieveOnRespawn) return;
        ItemStack stack = Yigd.SCROLL_ITEM.getDefaultStack();

        NbtCompound posNbt = NbtHelper.fromBlockPos(refPos);
        stack.setSubNbt("ref", posNbt);

        player.giveItemStack(stack);
    }

    @Redirect(method = "createEndSpawnPlatform", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean createEndSpawnPlatform(ServerWorld world, BlockPos blockPos, BlockState blockState) {
        BlockEntity be = world.getBlockEntity(blockPos);
        if (!(be instanceof GraveBlockEntity grave) || grave.getGraveOwner() == null) return world.setBlockState(blockPos, blockState);
        return false;
    }
}