package com.b1n4ry.yigd.block.entity;

import com.b1n4ry.yigd.Yigd;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class GraveBlockEntity extends BlockEntity implements BlockEntityClientSerializable {
    private GameProfile graveOwner;
    private int storedXp;
    private String customName;
    private DefaultedList<ItemStack> storedInventory;
    private List<List<ItemStack>> moddedInventories;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }
    public GraveBlockEntity(String customName, BlockPos pos, BlockState state) {
        super(Yigd.GRAVE_BLOCK_ENTITY, pos, state);

        this.graveOwner = null;
        this.storedXp = 0;
        this.customName = customName;
        this.storedInventory = DefaultedList.ofSize(41, ItemStack.EMPTY);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        tag.putInt("StoredXp", storedXp);
        tag.put("Items", Inventories.writeNbt(new NbtCompound(), this.storedInventory, true));
        tag.putInt("ItemCount", this.storedInventory.size());

        if (graveOwner != null) tag.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.graveOwner));
        if (customName != null) tag.putString("CustomName", customName);

        if (moddedInventories != null) {
            NbtList modList = new NbtList();
            for (List<ItemStack> inv : moddedInventories) {
                DefaultedList<ItemStack> list = DefaultedList.of();
                list.addAll(inv);
                modList.add(Inventories.writeNbt(new NbtCompound(), list));
            }
            tag.put("ModdedInventoryItems", modList);
        }

        return tag;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);

        this.storedInventory = DefaultedList.ofSize(tag.getInt("ItemCount"), ItemStack.EMPTY);

        Inventories.readNbt(tag.getCompound("Items"), this.storedInventory);

        this.storedXp = tag.getInt("StoredXp");

        if(tag.contains("owner")) this.graveOwner = NbtHelper.toGameProfile(tag.getCompound("owner"));
        if(tag.contains("CustomName")) this.customName = tag.getString("CustomName");

        if (tag.contains("ModdedInventoryItems")) {
            NbtList modList = tag.getList("ModdedInventoryItems", NbtList.LIST_TYPE);

            moddedInventories = new ArrayList<>();
            for (NbtElement mod : modList) {
                if (!(mod instanceof NbtCompound modNbt)) continue;
                DefaultedList<ItemStack> inventory = DefaultedList.of();
                Inventories.readNbt(modNbt, inventory);

                moddedInventories.add(inventory.stream().toList());
            }
        }
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        if(tag.contains("owner")) this.graveOwner = NbtHelper.toGameProfile(tag.getCompound("owner"));
        if(tag.contains("CustomName")) this.customName = tag.getString("CustomName");
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        if (graveOwner != null) tag.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.graveOwner));
        if (customName != null) tag.putString("CustomName", customName);
        return tag;
    }

    public void setGraveOwner(GameProfile owner) {
        this.graveOwner = owner;
    }
    public void setStoredXp(int xp) {
        this.storedXp = xp;
    }
    public void setCustomName(String name) {
        this.customName = name;
    }
    public void setInventory(DefaultedList<ItemStack> inventory) {
        this.storedInventory = inventory;
    }
    public void setModdedInventories(List<List<ItemStack>> inventories) {
        this.moddedInventories = inventories;
    }


    public GameProfile getGraveOwner() {
        return this.graveOwner;
    }
    public String getCustomName() {
        return customName;
    }
    public DefaultedList<ItemStack> getStoredInventory() {
        return storedInventory;
    }
    public int getStoredXp() {
        return storedXp;
    }
    public List<List<ItemStack>> getModdedInventories() {
        return moddedInventories;
    }
}
