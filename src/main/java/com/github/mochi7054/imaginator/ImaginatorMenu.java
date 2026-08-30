package com.github.mochi7054.imaginator;

import com.github.mochi7054.ReplicateMekanism;
import com.github.mochi7054.block.ReplicaTier;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class ImaginatorMenu extends MekanismTileContainer<ImaginatorBlockEntity> {

    public ImaginatorMenu(int containerId, Inventory inv, ImaginatorBlockEntity tile) {
        super(ReplicateMekanism.IMAGINATOR_CONTAINER, containerId, inv, tile);
    }

    public ImaginatorMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, getTileFromBuf(buf, ImaginatorBlockEntity.class, inv));
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

    private boolean isInputSlot(net.minecraft.world.inventory.Slot slot) {
        if (slot instanceof mekanism.common.inventory.container.slot.InventoryContainerSlot containerSlot) {
            ImaginatorBlockEntity tile = getTileEntity();
            if (tile != null) {
                return tile.inputSlots.contains(containerSlot.getInventorySlot());
            }
        }
        return false;
    }

    private int getInputSlotIndex(net.minecraft.world.inventory.Slot slot) {
        if (slot instanceof mekanism.common.inventory.container.slot.InventoryContainerSlot containerSlot) {
            ImaginatorBlockEntity tile = getTileEntity();
            if (tile != null) {
                return tile.inputSlots.indexOf(containerSlot.getInventorySlot());
            }
        }
        return -1;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, net.minecraft.world.entity.player.Player player) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            net.minecraft.world.inventory.Slot slot = this.slots.get(slotId);
            if (isInputSlot(slot)) {
                ImaginatorBlockEntity tile = getTileEntity();
                if (tile != null && tile.sorting) {
                    if (getInputSlotIndex(slot) > 0) {
                        return;
                    }
                }
                net.minecraft.world.item.ItemStack carried = this.getCarried();

                if (clickType == net.minecraft.world.inventory.ClickType.QUICK_MOVE) {
                    slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                    this.broadcastChanges();
                    return;
                }

                if (clickType == net.minecraft.world.inventory.ClickType.PICKUP) {
                    net.minecraft.world.item.ItemStack currentStack = slot.getItem();
                    if (carried.isEmpty()) {
                        slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                    } else {
                        var compound = com.github.mochi7054.collapser.CollapserBlockEntity.getMatterCompoundSafe(
                            com.github.mochi7054.imaginator.ImaginatorBlockEntity.getReplicatingStackFromInput(carried)
                        );
                        if (compound != null && !compound.getValues().isEmpty()) {
                            if (!currentStack.isEmpty() && net.minecraft.world.item.ItemStack.isSameItemSameTags(carried, currentStack)) {
                                slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                            } else {
                                net.minecraft.world.item.ItemStack copy = carried.copy();
                                copy.setCount(1);
                                slot.set(copy);
                            }
                        }
                    }
                    this.broadcastChanges();
                    return;
                }

                if (clickType == net.minecraft.world.inventory.ClickType.THROW) {
                    slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                    this.broadcastChanges();
                    return;
                }

                return;
            }
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slotId) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            net.minecraft.world.inventory.Slot slot = this.slots.get(slotId);
            if (slot == null || !slot.hasItem()) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            if (isInputSlot(slot)) {
                slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                this.broadcastChanges();
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            net.minecraft.world.item.ItemStack itemstack1 = slot.getItem();
            net.minecraft.world.item.ItemStack itemstack = itemstack1.copy();

            // 成果物スロット等、プレイヤーインベントリ以外のスロットからの Shift+クリック
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

            // プレイヤーインベントリからの Shift+クリック（ゴーストスロット設定）
            if (slot instanceof mekanism.common.inventory.container.slot.MainInventorySlot || 
                slot instanceof mekanism.common.inventory.container.slot.HotBarSlot) {
                
                if (slot.hasItem()) {
                    net.minecraft.world.item.ItemStack stack = slot.getItem();
                    var compound = com.github.mochi7054.collapser.CollapserBlockEntity.getMatterCompoundSafe(
                        com.github.mochi7054.imaginator.ImaginatorBlockEntity.getReplicatingStackFromInput(stack)
                    );
                    if (compound != null && !compound.getValues().isEmpty()) {
                        ImaginatorBlockEntity tile = getTileEntity();
                        if (tile != null) {
                            net.minecraft.world.inventory.Slot targetSlot = null;
                            for (net.minecraft.world.inventory.Slot s : this.slots) {
                                if (isInputSlot(s)) {
                                    if (tile.sorting && getInputSlotIndex(s) > 0) {
                                        continue;
                                    }
                                    net.minecraft.world.item.ItemStack inputItem = s.getItem();
                                    if (!inputItem.isEmpty() && net.minecraft.world.item.ItemStack.isSameItemSameTags(stack, inputItem)) {
                                        targetSlot = s;
                                        break;
                                    }
                                }
                            }
                            if (targetSlot == null) {
                                for (net.minecraft.world.inventory.Slot s : this.slots) {
                                    if (isInputSlot(s)) {
                                        if (tile.sorting && getInputSlotIndex(s) > 0) {
                                            continue;
                                        }
                                        if (s.getItem().isEmpty()) {
                                            targetSlot = s;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (targetSlot != null) {
                                net.minecraft.world.item.ItemStack currentInput = targetSlot.getItem();
                                if (!currentInput.isEmpty() && net.minecraft.world.item.ItemStack.isSameItemSameTags(stack, currentInput)) {
                                    targetSlot.set(net.minecraft.world.item.ItemStack.EMPTY);
                                } else {
                                    net.minecraft.world.item.ItemStack copy = stack.copy();
                                    copy.setCount(1);
                                    targetSlot.set(copy);
                                }
                                this.broadcastChanges();
                                return net.minecraft.world.item.ItemStack.EMPTY;
                            }
                        }
                    }
                }
            }
        }

        return this.m_7648_(player, slotId);
    }
}
