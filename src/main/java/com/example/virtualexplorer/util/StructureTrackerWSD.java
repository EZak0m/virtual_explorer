package com.example.virtualexplorer.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Set;

public class StructureTrackerWSD extends SavedData {
    private static final String DATA_NAME = "virtual_explorer_structures";
    private final Set<Long> exploredChunks = new HashSet<>();

    public static StructureTrackerWSD get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(
            StructureTrackerWSD::new,
            (tag, registries) -> StructureTrackerWSD.load(tag, registries)
        ), DATA_NAME);
    }

    public void markExplored(ChunkPos pos) {
        exploredChunks.add(pos.toLong());
        setDirty();
    }

    public boolean isExplored(ChunkPos pos) {
        return exploredChunks.contains(pos.toLong());
    }

    private static StructureTrackerWSD load(CompoundTag tag, HolderLookup.Provider registries) {
        StructureTrackerWSD data = new StructureTrackerWSD();
        long[] array = tag.getLongArray("ExploredChunks");
        for (long l : array) {
            data.exploredChunks.add(l);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("ExploredChunks", exploredChunks.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
}
