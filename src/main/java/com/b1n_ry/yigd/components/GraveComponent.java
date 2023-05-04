package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.TranslatableDeathMessage;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DeathMessageType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GraveComponent {
    private final GameProfile owner;
    private final InventoryComponent inventoryComponent;
    private final ExpComponent expComponent;
    private final ServerWorld serverWorld;
    private BlockPos pos;
    private final TranslatableDeathMessage deathMessage;

    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerWorld serverWorld, Vec3d pos, TranslatableDeathMessage deathMessage) {
        this(owner, inventoryComponent, expComponent, serverWorld, BlockPos.ofFloored(pos), deathMessage);
    }
    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerWorld serverWorld, BlockPos pos, TranslatableDeathMessage deathMessage) {
        this.owner = owner;
        this.inventoryComponent = inventoryComponent;
        this.expComponent = expComponent;
        this.serverWorld = serverWorld;
        this.pos = pos;
        this.deathMessage = deathMessage;
    }

    public InventoryComponent getInventoryComponent() {
        return this.inventoryComponent;
    }

    public ExpComponent getExpComponent() {
        return this.expComponent;
    }

    public ServerWorld getServerWorld() {
        return this.serverWorld;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public TranslatableDeathMessage getDamageSource() {
        return this.deathMessage;
    }

    /**
     * Will filter through filters and stuff
     * @return where a grave can be placed based on config
     */
    public BlockPos findGravePos() {
        return null;
    }

    /**
     * Called to place down a grave block
     * @param newPos Where the grave should try to be placed
     * @param state Which block should be placed
     * @return Weather or not the grave was placed
     */
    public boolean tryPlaceGraveAt(BlockPos newPos, BlockState state) {
        this.pos = newPos;
        return this.serverWorld.setBlockState(newPos, state);
    }

    public void backUp(GameProfile profile) {
        DeathInfoManager.INSTANCE.addBackup(profile, this);
    }

    public ActionResult claim(ServerPlayerEntity player, ServerWorld world, BlockState graveBlock, BlockPos pos, ItemStack tool) {
        YigdConfig config = YigdConfig.getConfig();

        InventoryComponent currentPlayerInv = new InventoryComponent(player);
        InventoryComponent.clearPlayer(player);

        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        if (config.graveConfig.claimPriority == ClaimPriority.GRAVE) {
            extraItems.addAll(this.inventoryComponent.merge(currentPlayerInv, true));
            this.inventoryComponent.applyToPlayer(player);
        } else {
            extraItems.addAll(currentPlayerInv.merge(this.inventoryComponent, false));
            currentPlayerInv.applyToPlayer(player);
        }

        return ActionResult.FAIL;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        nbt.put("inventory", this.inventoryComponent.toNbt());
        nbt.put("exp", this.expComponent.toNbt());

        nbt.put("world", this.getWorldRegistryKeyNbt(this.serverWorld));
        nbt.put("pos", NbtHelper.fromBlockPos(this.pos));
        nbt.put("deathMessage", this.deathMessage.toNbt());

        return nbt;
    }

    public static GraveComponent fromNbt(NbtCompound nbt, MinecraftServer server) {
        GameProfile owner = NbtHelper.toGameProfile(nbt.getCompound("owner"));
        InventoryComponent inventoryComponent = InventoryComponent.fromNbt(nbt.getCompound("inventory"));
        ExpComponent expComponent = ExpComponent.fromNbt(nbt.getCompound("exp"));
        RegistryKey<World> worldKey = getRegistryKeyFromNbt(nbt.getCompound("world"));
        ServerWorld world = server.getWorld(worldKey);
        BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("pos"));
        TranslatableDeathMessage deathMessage = TranslatableDeathMessage.fromNbt(nbt.getCompound("deathMessage"));

        return new GraveComponent(owner, inventoryComponent, expComponent, world, pos, deathMessage);
    }

    private NbtCompound getWorldRegistryKeyNbt(World world) {
        RegistryKey<World> key = world.getRegistryKey();
        NbtCompound nbt = new NbtCompound();
        nbt.putString("registry", key.getRegistry().toString());
        nbt.putString("value", key.getValue().toString());

        return nbt;
    }
    private static RegistryKey<World> getRegistryKeyFromNbt(NbtCompound nbt) {
        String registry = nbt.getString("registry");
        String value = nbt.getString("value");

        RegistryKey<Registry<World>> r = RegistryKey.ofRegistry(new Identifier(registry));
        return RegistryKey.of(r, new Identifier(value));
    }
}