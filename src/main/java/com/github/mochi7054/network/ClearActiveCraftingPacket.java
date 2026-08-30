package com.github.mochi7054.network;

import com.github.mochi7054.imaginator.ImaginatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClearActiveCraftingPacket {
    private final BlockPos pos;
    private final int slotIndex;

    public ClearActiveCraftingPacket(BlockPos pos, int slotIndex) {
        this.pos = pos;
        this.slotIndex = slotIndex;
    }

    public static void encode(ClearActiveCraftingPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.slotIndex);
    }

    public static ClearActiveCraftingPacket decode(FriendlyByteBuf buf) {
        return new ClearActiveCraftingPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(ClearActiveCraftingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().isLoaded(msg.pos)) {
                if (player.level().getBlockEntity(msg.pos) instanceof ImaginatorBlockEntity tile) {
                    tile.clearActiveCrafting(msg.slotIndex);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}