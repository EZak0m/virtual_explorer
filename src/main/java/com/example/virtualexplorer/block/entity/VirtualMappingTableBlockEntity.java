package com.example.virtualexplorer.block.entity;

import com.example.virtualexplorer.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import com.example.virtualexplorer.api.IExplorerModule;
import com.example.virtualexplorer.logic.ExplorationCalculator;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.StructureTags;

public class VirtualMappingTableBlockEntity extends BlockEntity implements MenuProvider {

    private final FluidTank fluidTank = new FluidTank(10000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    
    public enum ExplorationState {
        NATURE,
        SEARCHING_STRUCTURE,
        MOVING_TO_STRUCTURE,
        HOVERING_STRUCTURE
    }

    private ExplorationState state = ExplorationState.NATURE;
    private int hoverCount = 0;
    private BlockPos targetStructurePos = null;

    private boolean isActive = true;

    // インベントリ構成: 0=Module, 1-4=Upgrades, 5=Map, 6-13=Output, 14=Filter
    private final ItemStackHandler inventory = new ItemStackHandler(15) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() instanceof com.example.virtualexplorer.api.IExplorerModule;
            }
            if (slot >= 1 && slot <= 4) {
                Item item = stack.getItem();
                return item == com.example.virtualexplorer.init.ItemInit.SPEED_UPGRADE.get() || 
                       item == com.example.virtualexplorer.init.ItemInit.RECYCLE_UPGRADE.get() || 
                       item == com.example.virtualexplorer.init.ItemInit.RADAR_UPGRADE.get() || 
                       item == com.example.virtualexplorer.init.ItemInit.PARALLEL_UPGRADE.get() || 
                       item == com.example.virtualexplorer.init.ItemInit.UPGRADE_FLUID.get() ||
                       item == com.example.virtualexplorer.init.ItemInit.UPGRADE_ARCHEOLOGY.get() ||
                       item == com.example.virtualexplorer.init.ItemInit.UPGRADE_DEMOLITION_MASTER.get() ||
                       item == com.example.virtualexplorer.init.ItemInit.UPGRADE_STRUCTURE_INTEREST.get() ||
                       item == com.example.virtualexplorer.init.ItemInit.UPGRADE_INFINITE.get() ||
                       item == com.example.virtualexplorer.init.ItemInit.UPGRADE_FORTUNE.get() ||
                       item == com.example.virtualexplorer.init.ItemInit.UPGRADE_SILK_TOUCH.get();
            }
            if (slot == 5) {
                String altMapId = com.example.virtualexplorer.Config.ALTERNATIVE_MAP_ITEM.get();
                boolean isAltMap = !altMapId.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(altMapId);
                return stack.is(Items.MAP) || stack.is(Items.PAPER) || isAltMap;
            }
            if (slot == 14) {
                return stack.getItem() instanceof com.example.virtualexplorer.item.TargetPinItem;
            }
            return slot >= 6 && slot <= 13;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot >= 1 && slot <= 4) return com.example.virtualexplorer.Config.MAX_UPGRADES_PER_SLOT.get();
            if (slot == 14) return 1;
            return super.getSlotLimit(slot);
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(2000000000, 100000, 0) {
        @Override
        public int getMaxEnergyStored() {
            return com.example.virtualexplorer.Config.MAX_FE_CAPACITY.get();
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int maxTransfer = com.example.virtualexplorer.Config.MAX_FE_TRANSFER_RATE.get();
            int actualReceive = Math.min(maxReceive, maxTransfer);
            int energyReceived = Math.min(getMaxEnergyStored() - this.energy, actualReceive);
            if (!simulate) {
                this.energy += energyReceived;
            }
            return energyReceived;
        }
    };

    private final net.neoforged.neoforge.items.IItemHandler hopperHandler = new net.neoforged.neoforge.items.IItemHandler() {
        @Override
        public int getSlots() { return inventory.getSlots(); }
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // パイプ等からの地図の搬入を許可
            if (slot == 5 && inventory.isItemValid(5, stack)) {
                return inventory.insertItem(slot, stack, simulate);
            }
            return stack;
        }
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 出力スロット(6-13)のみ搬出を許可。フィルター(14)等は除外。
            if (slot < 6 || slot == 14) return ItemStack.EMPTY;
            return inventory.extractItem(slot, amount, simulate);
        }
        @Override
        public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 5) return inventory.isItemValid(5, stack);
            if (slot >= 6) return true;
            return false;
        }
    };

    private int progress = 0;
    private int maxProgress = 100;
    private int statusId = 0;
    private int targetStructureId = 0;
    private int currentChunkX;
    private int currentChunkZ;
    private int[] gridColors = new int[25];
    
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 10 && index < 35) {
                return gridColors[index - 10];
            }
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 6 -> fluidTank.getFluidAmount() & 0xFFFF;
                case 7 -> (fluidTank.getFluidAmount() >> 16) & 0xFFFF;
                case 8 -> fluidTank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluidTank.getFluid().getFluid());
                case 9 -> isActive ? 1 : 0;
                case 35 -> statusId;
                case 36 -> targetStructureId;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 10 && index < 35) {
                gridColors[index - 10] = value;
                return;
            }
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 9 -> isActive = (value == 1);
                case 35 -> statusId = value;
                case 36 -> targetStructureId = value;
            }
        }

        @Override
        public int getCount() {
            return 37;
        }
    };

    public VirtualMappingTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityInit.VIRTUAL_MAPPING_TABLE_BE.get(), pos, blockState);
        this.currentChunkX = pos.getX() >> 4;
        this.currentChunkZ = pos.getZ() >> 4;
    }

    private void updateGridColors() {
        if (level == null) return;
        
        java.util.Map<String, Integer> colorMap = new java.util.HashMap<>();
        for (Object entryObj : com.example.virtualexplorer.Config.BIOME_COLORS.get()) {
            String entry = (String) entryObj;
            int lastColon = entry.lastIndexOf(':');
            if (lastColon != -1) {
                String id = entry.substring(0, lastColon);
                String hex = entry.substring(lastColon + 1);
                try {
                    colorMap.put(id, Integer.parseInt(hex.replace("#", ""), 16));
                } catch (Exception ignored) {}
            }
        }

        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                int cx = currentChunkX + dx;
                int cz = currentChunkZ + dz;
                String biomeId = level.getBiome(new BlockPos(cx << 4, 64, cz << 4)).unwrapKey()
                    .map(key -> key.location().toString()).orElse("minecraft:plains");
                gridColors[(dz + 2) * 5 + (dx + 2)] = colorMap.getOrDefault(biomeId, 0x808080);
            }
        }
    }

    private boolean hasUpgrade(net.minecraft.world.item.Item item) {
        for (int i = 1; i <= 4; i++) {
            if (inventory.getStackInSlot(i).is(item)) return true;
        }
        return false;
    }

    private boolean isOutputFull() {
        for (int i = 6; i <= 13; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (gridColors[12] == 0) updateGridColors();

        // 基本的な稼働チェック
        if (!isActive) {
            statusId = 0; // OFF
            progress = 0;
            return;
        }

        ItemStack moduleStack = inventory.getStackInSlot(0);
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IExplorerModule module)) {
            statusId = 1; // NO_MODULE
            progress = 0;
            return;
        }

        if (isOutputFull()) {
            statusId = 2; // INVENTORY_FULL
            return;
        }

        ItemStack mapStack = inventory.getStackInSlot(5);
        if (com.example.virtualexplorer.Config.CONSUME_MAP_PER_CHUNK.get() && mapStack.isEmpty()) {
            statusId = 3; // NO_MAP
            return;
        }

        // アップグレードの取得
        boolean hasStructureInterest = hasUpgrade(com.example.virtualexplorer.init.ItemInit.UPGRADE_STRUCTURE_INTEREST.get());
        boolean hasArcheology = hasUpgrade(com.example.virtualexplorer.init.ItemInit.UPGRADE_ARCHEOLOGY.get());
        
        // 状態の自動遷移
        if (!hasStructureInterest && !hasArcheology) {
            state = ExplorationState.NATURE;
        } else if (state == ExplorationState.NATURE) {
            state = ExplorationState.SEARCHING_STRUCTURE;
        }

        // ステートマシン
        if (state == ExplorationState.SEARCHING_STRUCTURE) {
            statusId = 6; // SEARCHING
            searchNearestStructure(module.getModuleId(), hasArcheology);
        } else if (state == ExplorationState.MOVING_TO_STRUCTURE) {
            statusId = 7; // MOVING
        } else if (state == ExplorationState.HOVERING_STRUCTURE) {
            statusId = 8; // HOVERING
        } else {
            statusId = 5; // NATURE
        }

        int speedUpgrades = 0;
        int radarUpgrades = 0;
        int parallelUpgrades = 0;
        for (int i = 1; i <= 4; i++) {
            ItemStack up = inventory.getStackInSlot(i);
            if (up.is(com.example.virtualexplorer.init.ItemInit.SPEED_UPGRADE.get())) speedUpgrades += up.getCount();
            if (up.is(com.example.virtualexplorer.init.ItemInit.RADAR_UPGRADE.get())) radarUpgrades += up.getCount();
            if (up.is(com.example.virtualexplorer.init.ItemInit.PARALLEL_UPGRADE.get())) parallelUpgrades += up.getCount();
        }

        int energyCost = ExplorationCalculator.calculateEnergyCost(module, speedUpgrades, parallelUpgrades, radarUpgrades);
        this.maxProgress = ExplorationCalculator.calculateProcessingTime(module, speedUpgrades);

        if (energyStorage.getEnergyStored() < energyCost) {
            statusId = 4; // NO_ENERGY
            return;
        }

        energyStorage.extractEnergy(energyCost, false);
        progress++;

        if (progress >= maxProgress) {
            handleOperationComplete(module, speedUpgrades, radarUpgrades, parallelUpgrades);
            progress = 0;
        }
    }

    private String targetStructureName = "";

    private void searchNearestStructure(String moduleId, boolean isArcheology) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ItemStack pinStack = inventory.getStackInSlot(14);
            String pinTargetStructure = null;
            if (!pinStack.isEmpty() && pinStack.getItem() instanceof com.example.virtualexplorer.item.TargetPinItem) {
                net.minecraft.world.item.component.CustomData customData = pinStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    if (tag.contains("TargetStructure")) {
                        pinTargetStructure = tag.getString("TargetStructure");
                    }
                }
            }

            ResourceKey<Structure> structureKey;
            if (pinTargetStructure != null) {
                targetStructureName = pinTargetStructure;
                structureKey = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(pinTargetStructure));
            } else {
                structureKey = switch (moduleId) {
                    case "underground" -> {
                        targetStructureName = "mineshaft";
                        yield BuiltinStructures.MINESHAFT;
                    }
                    case "nether" -> {
                        targetStructureName = "fortress";
                        yield BuiltinStructures.FORTRESS;
                    }
                    case "end" -> {
                        targetStructureName = "end_city";
                        yield BuiltinStructures.END_CITY;
                    }
                    default -> {
                        if (isArcheology) {
                            targetStructureName = "pyramid";
                            yield BuiltinStructures.DESERT_PYRAMID;
                        } else {
                            targetStructureName = "village";
                            yield BuiltinStructures.VILLAGE_PLAINS;
                        }
                    }
                };
            }

            HolderSet<Structure> holderSet = serverLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                    .get(structureKey).map(HolderSet::direct).orElse(null);

            if (holderSet != null) {
                com.mojang.datafixers.util.Pair<BlockPos, net.minecraft.core.Holder<Structure>> pair = serverLevel.getChunkSource().getGenerator().findNearestMapStructure(serverLevel, holderSet, worldPosition, 100, false);
                if (pair != null) {
                    targetStructurePos = pair.getFirst();
                    state = ExplorationState.MOVING_TO_STRUCTURE;
                } else {
                    state = ExplorationState.NATURE;
                    targetStructureName = "";
                }
            } else {
                state = ExplorationState.NATURE;
                targetStructureName = "";
            }
        }
    }

    private void handleOperationComplete(IExplorerModule module, int speed, int radar, int parallel) {
        if (com.example.virtualexplorer.Config.CONSUME_MAP_PER_CHUNK.get()) {
            inventory.getStackInSlot(5).shrink(1);
        }

        boolean hasFortune = hasUpgrade(com.example.virtualexplorer.init.ItemInit.UPGRADE_FORTUNE.get());
        boolean hasSilkTouch = hasUpgrade(com.example.virtualexplorer.init.ItemInit.UPGRADE_SILK_TOUCH.get());
        boolean hasDemolition = hasUpgrade(com.example.virtualexplorer.init.ItemInit.UPGRADE_DEMOLITION_MASTER.get());
        boolean hasFluid = hasUpgrade(com.example.virtualexplorer.init.ItemInit.UPGRADE_FLUID.get());
        boolean hasInfinite = hasUpgrade(com.example.virtualexplorer.init.ItemInit.UPGRADE_INFINITE.get());

        int batchSize = 1 + parallel;
        for (int b = 0; b < batchSize; b++) {
            performExploration(module.getModuleId(), hasFortune, hasSilkTouch, hasDemolition, hasFluid);
        }

        if (state == ExplorationState.MOVING_TO_STRUCTURE && targetStructurePos != null) {
            moveTowardsTarget();
        } else if (state == ExplorationState.HOVERING_STRUCTURE) {
            hoverCount--;
            if (hoverCount <= 0) {
                if (!hasInfinite && level instanceof net.minecraft.server.level.ServerLevel sl) {
                    net.minecraft.world.level.ChunkPos center = new net.minecraft.world.level.ChunkPos(targetStructurePos);
                    com.example.virtualexplorer.util.StructureTrackerWSD wsd = com.example.virtualexplorer.util.StructureTrackerWSD.get(sl);
                    // 9x9チャンク（半径4）を探索済みとしてマーク
                    for (int x = -4; x <= 4; x++) {
                        for (int z = -4; z <= 4; z++) {
                            wsd.markExplored(new net.minecraft.world.level.ChunkPos(center.x + x, center.z + z));
                        }
                    }
                }
                state = ExplorationState.NATURE;
                targetStructurePos = null;
            }
        } else {
            // 通常移動
            int dir = level.random.nextInt(4);
            if (dir == 0) currentChunkX++; else if (dir == 1) currentChunkX--; else if (dir == 2) currentChunkZ++; else currentChunkZ--;
        }
        
        updateGridColors();
        setChanged();
    }

    private void moveTowardsTarget() {
        int tx = targetStructurePos.getX() >> 4;
        int tz = targetStructurePos.getZ() >> 4;
        if (currentChunkX < tx) currentChunkX++; else if (currentChunkX > tx) currentChunkX--;
        if (currentChunkZ < tz) currentChunkZ++; else if (currentChunkZ > tz) currentChunkZ--;

        if (currentChunkX == tx && currentChunkZ == tz) {
            state = ExplorationState.HOVERING_STRUCTURE;
            hoverCount = 10; // デフォルト10回
        }
    }

    private void performExploration(String moduleId, boolean fortune, boolean silkTouch, boolean demolition, boolean fluid) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            java.util.List<ItemStack> loots = new java.util.ArrayList<>();
            boolean isHovering = (state == ExplorationState.HOVERING_STRUCTURE);

            // TargetPinのバイオームフィルタをチェック
            String targetBiomeId = null;
            ItemStack pinStack = inventory.getSlots() > 14 ? inventory.getStackInSlot(14) : ItemStack.EMPTY;
            if (!pinStack.isEmpty() && pinStack.getItem() instanceof com.example.virtualexplorer.item.TargetPinItem) {
                net.minecraft.world.item.component.CustomData customData = pinStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    if (tag.contains("TargetBiome")) {
                        targetBiomeId = tag.getString("TargetBiome");
                    }
                }
            }

            // 構造物チェスト (滞在時はこちらを優先)
            if (isHovering) {
                loots.addAll(com.example.virtualexplorer.logic.LootGenerator.generateStructureLoot(serverLevel, targetStructureName, level.random));
            } else {
                // 自然生成物 (通常移動中)
                loots.addAll(com.example.virtualexplorer.logic.LootGenerator.generateNatureLoot(serverLevel, moduleId, level.random, fortune, silkTouch, targetBiomeId));
            }

            // 解体の匠
            if (demolition) {
                loots.addAll(com.example.virtualexplorer.logic.LootGenerator.generateDemolitionLoot(moduleId, level.random, isHovering, silkTouch));
            }

            // 流体
            if (fluid) {
                FluidStack fs = com.example.virtualexplorer.logic.LootGenerator.generateFluidLoot(moduleId, level.random);
                if (!fs.isEmpty()) fluidTank.fill(fs, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }

            net.neoforged.neoforge.items.wrapper.RangedWrapper outputWrapper = new net.neoforged.neoforge.items.wrapper.RangedWrapper(inventory, 6, 14);
            for (ItemStack loot : loots) {
                net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(outputWrapper, loot, false);
            }
        }
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public net.neoforged.neoforge.items.IItemHandler getHopperHandler() {
        return hopperHandler;
    }

    public boolean isActive() {
        return isActive;
    }

    public void toggleActive() {
        this.isActive = !this.isActive;
        this.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.virtualexplorer.virtual_mapping_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.example.virtualexplorer.inventory.VirtualMappingTableMenu(id, inventory, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
        
        CompoundTag fluidTag = new CompoundTag();
        fluidTank.writeToNBT(registries, fluidTag);
        tag.put("Fluid", fluidTag);
        
        tag.putBoolean("IsActive", this.isActive);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("ChunkX", currentChunkX);
        tag.putInt("ChunkZ", currentChunkZ);
        tag.putIntArray("GridColors", gridColors);
        
        tag.putInt("ExplorationState", state.ordinal());
        tag.putInt("HoverCount", hoverCount);
        if (targetStructurePos != null) {
            tag.putLong("TargetStructurePos", targetStructurePos.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // ItemStackHandler.deserializeNBTは"Size"タグでインスタンスのサイズを上書きしてしまうため、
        // 既存ワールド（旧サイズ14）での読み込み時にサイズが14に戻ってしまい、15番目のスロットアクセスでクラッシュします。
        // これを防ぐため、手動でItemsリストから読み込みます。
        if (tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            if (invTag.contains("Items", 9)) {
                net.minecraft.nbt.ListTag list = invTag.getList("Items", 10);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompound(i);
                    int slot = itemTag.getInt("Slot");
                    if (slot >= 0 && slot < inventory.getSlots()) {
                        inventory.setStackInSlot(slot, ItemStack.parseOptional(registries, itemTag));
                    }
                }
            }
        }
        energyStorage.deserializeNBT(registries, tag.get("Energy"));
        
        if (tag.contains("Fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
        }
        
        if (tag.contains("IsActive")) {
            this.isActive = tag.getBoolean("IsActive");
        }
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        currentChunkX = tag.getInt("ChunkX");
        currentChunkZ = tag.getInt("ChunkZ");
        if (tag.contains("GridColors")) {
            gridColors = tag.getIntArray("GridColors");
        }

        if (tag.contains("ExplorationState")) {
            state = ExplorationState.values()[tag.getInt("ExplorationState")];
        }
        hoverCount = tag.getInt("HoverCount");
        if (tag.contains("TargetStructurePos")) {
            targetStructurePos = BlockPos.of(tag.getLong("TargetStructurePos"));
        }
    }
}
