package com.github.mochi7054.network;

import com.github.mochi7054.imaginator.ImaginatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleAutoSortPacket {
    private final BlockPos pos;

    public ToggleAutoSortPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ToggleAutoSortPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static ToggleAutoSortPacket decode(FriendlyByteBuf buf) {
        return new ToggleAutoSortPacket(buf.readBlockPos());
    }

    public static void handle(ToggleAutoSortPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().isLoaded(msg.pos)) {
                var be = player.level().getBlockEntity(msg.pos);
                if (be instanceof com.github.mochi7054.collapser.CollapserBlockEntity collapser) {
                    collapser.setSorting(!collapser.isSorting());
                } else if (be instanceof ImaginatorBlockEntity tile) {
                    tile.toggleAutoSort();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}