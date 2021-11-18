package com.b1n4ry.yigd.core;

import com.b1n4ry.yigd.Yigd;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class YigdCommand {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("retrieve_grave")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> {
                        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");

                        return retrieveGrave(player);
                    })
                )
                .executes(context -> {
                    PlayerEntity player = context.getSource().getPlayer();

                    return retrieveGrave(player);
                })
            );
        });
    }

    private static int retrieveGrave(PlayerEntity player) {
        UUID userId = player.getUuid();

        if (!Yigd.deadPlayerData.hasStoredInventory(userId)) {
            player.sendMessage(Text.of("Could not find grave to fetch"), true);
            return -1;
        }

        BlockPos gravePos = Yigd.deadPlayerData.getDeathPos(userId);
        DefaultedList<ItemStack> items = Yigd.deadPlayerData.getDeathPlayerInventory(userId);

        int xp = Yigd.deadPlayerData.getDeathXp(userId);

        GraveHelper.RetrieveItems(player, items, xp);

        player.world.removeBlock(gravePos, false);
        player.sendMessage(Text.of("Retrieved grave remotely successfully"), true);

        return 1;
    }
}