package com.github.mochi7054.collapser;

import com.github.mochi7054.ReplicateMekanism;
import com.github.mochi7054.block.ReplicaTier;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class CollapserMenu extends MekanismTileContainer<CollapserBlockEntity> {

    public CollapserMenu(int containerId, Inventory inv, CollapserBlockEntity tile) {
        super(ReplicateMekanism.COLLAPSER_CONTAINER, containerId, inv, tile);
    }

    public CollapserMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, getTileFromBuf(buf, CollapserBlockEntity.class, inv));
    }

    private static <TILE extends mekanism.common.tile.base.TileEntityMekanism> TILE getTileFromBuf(FriendlyByteBuf buf, Class<TILE> type, Inventory inv) {
        if (buf == null) {
            return null;
        }
        return mekanism.common.util.WorldUtils.getTileEntity(type, inv.player.level(), buf.readBlockPos());
    }

    @Override
    protected void addInventorySlots(Inventory playerInventory) {
        ReplicaTier tier = getTileEntity() != null ? getTileEntity().getTier() : ReplicaTier.STANDARD;
        int yOffset = tier == ReplicaTier.STANDARD ? 94 : 104;
        int xOffset = switch (tier) {
            case STANDARD, BASIC, ADVANCED -> 8;
            case ELITE -> 10;
            case ULTIMATE -> 29;
        };

        // Main Inventory (3 rows of 9 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + col + row * 9;
                int x = xOffset + col * 18;
                int y = yOffset + row * 18;
                this.addSlot(new mekanism.common.inventory.container.slot.MainInventorySlot(playerInventory, slotIndex, x, y));
            }
        }

        // Hotbar (9 slots)
        int hotbarY = tier == ReplicaTier.STANDARD ? 152 : 162;
        for (int col = 0; col < 9; col++) {
            int x = xOffset + col * 18;
            this.addSlot(new mekanism.common.inventory.container.slot.HotBarSlot(playerInventory, col, x, hotbarY));
        }
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slotId) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            net.minecraft.world.inventory.Slot slot = this.slots.get(slotId);
            if (slot == null || !slot.hasItem()) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            net.minecraft.world.item.ItemStack itemstack1 = slot.getItem();
            net.minecraft.world.item.ItemStack itemstack = itemstack1.copy();

            // マシンスロットからの Shift+クリック -> プレイヤーインベントリへ
            if (!(slot instanceof mekanism.common.inventory.container.slot.MainInventorySlot) && 
                !(slot instanceof mekanism.common.inventory.container.slot.HotBarSlot)) {
                
                int startPlayer = -1;
                for (int s = 0; s < this.slots.size(); s++) {
                    net.minecraft.world.inventory.Slot sl = this.slots.get(s);
                    if (sl instanceof mekanism.common.inventory.container.slot.MainInventorySlot || 
                        sl instanceof mekanism.common.inventory.container.slot.HotBarSlot) {
                        if (startPlayer == -1) startPlayer = s;
                    }
                }

                if (startPlayer != -1) {
                    if (!this.moveItemStackTo(itemstack1, startPlayer, this.slots.size(), true)) {
                        return net.minecraft.world.item.ItemStack.EMPTY;
                    }
                }

                if (itemstack1.isEmpty()) {
                    slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                if (itemstack1.getCount() == itemstack.getCount()) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }

                slot.onTake(player, itemstack1);
                this.broadcastChanges();
                return itemstack;
            }

            // プレイヤーインベントリからの Shift+クリック -> マシンスロットへ
            if (slot instanceof mekanism.common.inventory.container.slot.MainInventorySlot || 
                slot instanceof mekanism.common.inventory.container.slot.HotBarSlot) {
                
                int endMachine = -1;
                for (int s = 0; s < this.slots.size(); s++) {
                    net.minecraft.world.inventory.Slot sl = this.slots.get(s);
                    if (sl instanceof mekanism.common.inventory.container.slot.MainInventorySlot || 
                        sl instanceof mekanism.common.inventory.container.slot.HotBarSlot) {
                        endMachine = s;
                        break;
                    }
                }

                if (endMachine > 0) {
                    if (!this.moveItemStackTo(itemstack1, 0, endMachine, false)) {
                        return net.minecraft.world.item.ItemStack.EMPTY;
                    }
                }

                if (itemstack1.isEmpty()) {
                    slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                if (itemstack1.getCount() == itemstack.getCount()) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }

                slot.onTake(player, itemstack1);
                this.broadcastChanges();
                return itemstack;
            }
        }

        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
