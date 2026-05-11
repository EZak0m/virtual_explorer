package com.example.virtualexplorer.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class VirtualExplorerSavedData extends SavedData {

    private final Set<ChunkPos> exploredChunks = new HashSet<>();
    private static final String FILE_NAME = "virtualexplorer_data";

    public VirtualExplorerSavedData() {}

    public static VirtualExplorerSavedData load(CompoundTag nbt, HolderLookup.Provider provider) {
        VirtualExplorerSavedData data = new VirtualExplorerSavedData();
        ListTag list = nbt.getList("ExploredChunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag chunkTag = list.getCompound(i);
            int x = chunkTag.getInt("x");
            int z = chunkTag.getInt("z");
            data.exploredChunks.add(new ChunkPos(x, z));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (ChunkPos pos : exploredChunks) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putInt("x", pos.x);
            chunkTag.putInt("z", pos.z);
            list.add(chunkTag);
        }
        nbt.put("ExploredChunks", list);
        return nbt;
    }

    public boolean isExplored(ChunkPos pos) {
        return exploredChunks.contains(pos);
    }

    public void markExplored(ChunkPos pos) {
        if (exploredChunks.add(pos)) {
            setDirty();
        }
    }

    public static VirtualExplorerSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VirtualExplorerSavedData::new, VirtualExplorerSavedData::load, null),
                FILE_NAME
        );
    }
}
