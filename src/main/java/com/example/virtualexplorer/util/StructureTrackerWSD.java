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
    private final java.util.Map<String, Set<Long>> exploredChunksPerType = new java.util.HashMap<>();

    public static StructureTrackerWSD get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(
            StructureTrackerWSD::new,
            (tag, registries) -> StructureTrackerWSD.load(tag, registries)
        ), DATA_NAME);
    }

    public void markExplored(String type, ChunkPos pos) {
        exploredChunksPerType.computeIfAbsent(type, k -> new HashSet<>()).add(pos.toLong());
        setDirty();
    }

    public boolean isExplored(String type, ChunkPos pos) {
        Set<Long> explored = exploredChunksPerType.get(type);
        return explored != null && explored.contains(pos.toLong());
    }

    public boolean isAnyExplored(ChunkPos pos) {
        long l = pos.toLong();
        for (Set<Long> set : exploredChunksPerType.values()) {
            if (set.contains(l)) return true;
        }
        return false;
    }

    private static StructureTrackerWSD load(CompoundTag tag, HolderLookup.Provider registries) {
        StructureTrackerWSD data = new StructureTrackerWSD();
        CompoundTag typesTag = tag.getCompound("Types");
        for (String type : typesTag.getAllKeys()) {
            long[] array = typesTag.getLongArray(type);
            Set<Long> set = new HashSet<>();
            for (long l : array) set.add(l);
            data.exploredChunksPerType.put(type, set);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag typesTag = new CompoundTag();
        for (java.util.Map.Entry<String, Set<Long>> entry : exploredChunksPerType.entrySet()) {
            typesTag.putLongArray(entry.getKey(), entry.getValue().stream().mapToLong(Long::longValue).toArray());
        }
        tag.put("Types", typesTag);
        return tag;
    }
}
