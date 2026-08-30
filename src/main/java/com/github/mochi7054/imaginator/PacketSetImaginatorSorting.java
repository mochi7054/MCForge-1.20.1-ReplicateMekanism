package com.github.mochi7054.imaginator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetImaginatorSorting {
    private final BlockPos pos;
    private final boolean sorting;

    public PacketSetImaginatorSorting(BlockPos pos, boolean sorting) {
        this.pos = pos;
        this.sorting = sorting;
    }

    public static void encode(PacketSetImaginatorSorting msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.sorting);
    }

    public static PacketSetImaginatorSorting decode(FriendlyByteBuf buf) {
        return new PacketSetImaginatorSorting(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(PacketSetImaginatorSorting msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().isLoaded(msg.pos)) {
                if (player.level().getBlockEntity(msg.pos) instanceof ImaginatorBlockEntity tile) {
                    tile.sorting = msg.sorting;
                    tile.markForSave();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}