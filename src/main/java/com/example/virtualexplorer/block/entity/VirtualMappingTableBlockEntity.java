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
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.ChunkPos;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;

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

    // インストールされたアップグレードを保持するマップ
    private final it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<Item> installedUpgrades = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<>();

    // インベントリ構成: 0=Module, 1=Install Slot, 2=Map, 3=Filter, 4-19=Output (16 slots)
    // 20-30=Jade用隠しスロット (各アップグレードの搭載数保持用)
    private final ItemStackHandler inventory = new ItemStackHandler(31) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() instanceof com.example.virtualexplorer.api.IExplorerModule;
            }
            if (slot == 1) {
                Item item = stack.getItem();
                return item instanceof com.example.virtualexplorer.item.UpgradeItem || item instanceof com.example.virtualexplorer.item.CompressedUpgradeItem;
            }
            if (slot == 2) {
                String altMapId = com.example.virtualexplorer.Config.ALTERNATIVE_MAP_ITEM.get();
                boolean isAltMap = !altMapId.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(altMapId);
                return stack.is(Items.MAP) || stack.is(Items.PAPER) || isAltMap;
            }
            if (slot == 3) {
                return stack.getItem() instanceof com.example.virtualexplorer.item.TargetPinItem;
            }
            if (slot >= 4 && slot <= 19) {
                return true; // 出力スロットへの内部的な搬入は許可 (手動搬入はMenu側で禁止)
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 1) return 64;
            if (slot == 3) return 1;
            if (slot >= 20 && slot <= 30) return 10000;
            return super.getSlotLimit(slot);
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(2100000000, 100000, 0) {
        @Override
        public int getMaxEnergyStored() {
            return com.example.virtualexplorer.Config.MAX_FE_CAPACITY.get();
        }

        @Override
        public net.minecraft.nbt.Tag serializeNBT(net.minecraft.core.HolderLookup.Provider registries) {
            return net.minecraft.nbt.IntTag.valueOf(this.getEnergyStored());
        }

        @Override
        public void deserializeNBT(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.nbt.Tag nbt) {
            if (nbt instanceof net.minecraft.nbt.IntTag intTag) {
                this.energy = intTag.getAsInt();
            }
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
            // パイプ等からの地図の搬入を許可 (Slot 2)
            if (slot == 2 && inventory.isItemValid(2, stack)) {
                return inventory.insertItem(slot, stack, simulate);
            }
            // インストールスロット(Slot 1)への自動搬入も許可
            if (slot == 1 && inventory.isItemValid(1, stack)) {
                return inventory.insertItem(slot, stack, simulate);
            }
            return stack;
        }
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 出力スロット(4-19)のみ搬出を許可。フィルター(3)等は除外。
            if (slot < 4 || slot > 19) return ItemStack.EMPTY;
            return inventory.extractItem(slot, amount, simulate);
        }
        @Override
        public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 2) return inventory.isItemValid(2, stack);
            if (slot == 1) return inventory.isItemValid(1, stack);
            if (slot >= 4 && slot <= 19) return true;
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
    private final String[] gridBiomes = new String[25];

    public int getGridColor(int index) {
        if (index >= 0 && index < 25) {
            return gridColors[index];
        }
        return 0;
    }

    public String getGridBiome(int index) {
        if (index >= 0 && index < 25) {
            return gridBiomes[index] != null ? gridBiomes[index] : "minecraft:plains";
        }
        return "minecraft:plains";
    }
    private int currentEnergyCost = 0;
    
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
                case 37 -> currentChunkX & 0xFFFF;
                case 38 -> (currentChunkX >> 16) & 0xFFFF;
                case 39 -> currentChunkZ & 0xFFFF;
                case 40 -> (currentChunkZ >> 16) & 0xFFFF;
                case 41 -> currentEnergyCost;
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
                case 37 -> currentChunkX = (currentChunkX & 0xFFFF0000) | (value & 0xFFFF);
                case 38 -> currentChunkX = (currentChunkX & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                case 39 -> currentChunkZ = (currentChunkZ & 0xFFFF0000) | (value & 0xFFFF);
                case 40 -> currentChunkZ = (currentChunkZ & 0x0000FFFF) | ((value & 0xFFFF) << 16);
            }
        }

        @Override
        public int getCount() {
            return 42;
        }
    };

    public VirtualMappingTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityInit.VIRTUAL_MAPPING_TABLE_BE.get(), pos, blockState);
        this.currentChunkX = pos.getX() >> 4;
        this.currentChunkZ = pos.getZ() >> 4;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder = level.getBiome(new BlockPos(cx << 4, 64, cz << 4));
                String biomeId = biomeHolder.unwrapKey()
                    .map(key -> key.location().toString()).orElse("minecraft:plains");
                int index = (dz + 2) * 5 + (dx + 2);
                gridColors[index] = colorMap.getOrDefault(biomeId, 0x808080);
                gridBiomes[index] = biomeId;
            }
        }
    }

    private boolean hasUpgrade(net.minecraft.world.item.Item item) {
        return installedUpgrades.getInt(item) > 0;
    }

    private boolean isOutputFull() {
        for (int i = 4; i <= 19; i++) {
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

        // インストールスロット(Slot 1)の処理
        ItemStack installStack = inventory.getStackInSlot(1);
        if (!installStack.isEmpty()) {
            if (addUpgrade(installStack.getItem(), installStack.getCount())) {
                inventory.setStackInSlot(1, ItemStack.EMPTY);
                statusId = 5; // Success (Reset conflict if it was there)
            } else {
                statusId = 9; // CONFLICT
                return;
            }
        }

        // Jade表示用に隠しスロットを同期
        syncUpgradesToHiddenSlots();

        ItemStack mapStack = inventory.getStackInSlot(2);
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

        int speedUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.SPEED_UPGRADE.get());
        int radarUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.RADAR_UPGRADE.get());
        int parallelUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.PARALLEL_UPGRADE.get());
        int recycleUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.RECYCLE_UPGRADE.get());
        int fluidUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.UPGRADE_FLUID.get());
        int structureInterestUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.UPGRADE_STRUCTURE_INTEREST.get());
        int silkTouchUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.UPGRADE_SILK_TOUCH.get());
        int demolitionUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.UPGRADE_DEMOLITION_MASTER.get());
        int infiniteUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.UPGRADE_INFINITE.get());

        int energyCost = ExplorationCalculator.calculateEnergyCost(module, speedUpgrades, parallelUpgrades, radarUpgrades,
                recycleUpgrades, fluidUpgrades, structureInterestUpgrades, silkTouchUpgrades, demolitionUpgrades, infiniteUpgrades);
        this.maxProgress = ExplorationCalculator.calculateProcessingTime(module, speedUpgrades);

        if (energyStorage.getEnergyStored() < energyCost) {
            statusId = 4; // NO_ENERGY
            return;
        }

        energyStorage.extractEnergy(energyCost, false);
        this.currentEnergyCost = energyCost;
        progress++;

        // 隣接ブロックへの自動搬出入
        handleAutoTransfer();

        if (progress >= maxProgress) {
            handleOperationComplete(module, speedUpgrades, radarUpgrades, parallelUpgrades);
            progress = 0;
        }
    }

    private String targetStructureName = "";

    public void setCurrentChunkPos(int x, int z) {
        this.currentChunkX = x;
        this.currentChunkZ = z;
        this.updateGridColors();
        this.setChanged();
    }
    
    public String getTargetStructureName() {
        return targetStructureName;
    }


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
                com.example.virtualexplorer.util.StructureTrackerWSD wsd = com.example.virtualexplorer.util.StructureTrackerWSD.get(serverLevel);
                // マシンの場所ではなく、現在の仮想座標（チャンク）を起点に検索
                BlockPos searchPos = new BlockPos(currentChunkX << 4, 64, currentChunkZ << 4);
                com.mojang.datafixers.util.Pair<BlockPos, net.minecraft.core.Holder<Structure>> pair = null;

                // 最大5回、未探索の構造物が見つかるまで検索ポイントをずらして検索
                for (int i = 0; i < 5; i++) {
                    pair = serverLevel.getChunkSource().getGenerator().findNearestMapStructure(serverLevel, holderSet, searchPos, 100, false);
                    if (pair == null) break;
                    
                    BlockPos pos = pair.getFirst();
                    if (!wsd.isExplored(targetStructureName, new net.minecraft.world.level.ChunkPos(pos))) {
                        break; // 未探索のものを発見
                    }
                    // 探索済みの場合は、その座標から大きく離れた場所から再検索（同じ構造物の再取得防止）
                    searchPos = pos.offset(1600, 0, 1600);
                }

                if (pair != null) {
                    targetStructurePos = pair.getFirst();
                    state = ExplorationState.MOVING_TO_STRUCTURE;
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
            int recycleUpgrades = installedUpgrades.getInt(com.example.virtualexplorer.init.ItemInit.RECYCLE_UPGRADE.get());
            double consumptionChance = com.example.virtualexplorer.logic.ExplorationCalculator.calculateMapConsumptionChance(recycleUpgrades);
            
            if (level.random.nextDouble() < consumptionChance) {
                inventory.getStackInSlot(2).shrink(1); // Slot 2 is the Map slot
            }
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
                    // 9x9チャンク（半径4）を同種構造物の探索済みエリアとしてマーク
                    for (int x = -4; x <= 4; x++) {
                        for (int z = -4; z <= 4; z++) {
                            wsd.markExplored(targetStructureName, new net.minecraft.world.level.ChunkPos(center.x + x, center.z + z));
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
            ItemStack pinStack = inventory.getStackInSlot(3);
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

            net.neoforged.neoforge.items.wrapper.RangedWrapper outputWrapper = new net.neoforged.neoforge.items.wrapper.RangedWrapper(inventory, 4, 20);
            for (ItemStack loot : loots) {
                net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(outputWrapper, loot, false);
            }
        }
    }

    private void handleAutoTransfer() {
        if (level == null || level.isClientSide) return;

        // エネルギー搬入 (全方向から)
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            net.neoforged.neoforge.energy.IEnergyStorage neighborEnergy = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (neighborEnergy != null) {
                int stored = neighborEnergy.getEnergyStored();
                if (stored > 0) {
                    int maxReceive = energyStorage.receiveEnergy(stored, true);
                    if (maxReceive > 0) {
                        // 搬出元の出力制限(maxExtract)をバイパスするため、まず最優先でリフレクションにより電力を強制抽出する
                        int forcedExtracted = 0;
                        Class<?> clazz = neighborEnergy.getClass();
                        String[] targetFields = {"energy", "energyStored", "fe", "power", "storedEnergy", "amount"};
                        
                        while (clazz != null && forcedExtracted <= 0) {
                            for (String fieldName : targetFields) {
                                try {
                                    java.lang.reflect.Field energyField = clazz.getDeclaredField(fieldName);
                                    energyField.setAccessible(true);
                                    int current = energyField.getInt(neighborEnergy);
                                    forcedExtracted = Math.min(maxReceive, current);
                                    if (forcedExtracted > 0) {
                                        energyField.setInt(neighborEnergy, current - forcedExtracted);
                                        energyStorage.receiveEnergy(forcedExtracted, false);
                                        break;
                                    }
                                } catch (NoSuchFieldException e) {
                                    // 該当フィールドがない場合は次を試す
                                } catch (Exception e) {
                                    break;
                                }
                            }
                            clazz = clazz.getSuperclass();
                        }
                        
                        // もしリフレクションで抽出できなかった場合のみ、通常の extractEnergy でフォールバック受電する
                        if (forcedExtracted <= 0) {
                            int extracted = neighborEnergy.extractEnergy(maxReceive, false);
                            if (extracted > 0) {
                                energyStorage.receiveEnergy(extracted, false);
                            } else {
                                // 通常抽出もできず、リフレクションも機能しない場合のクリエイティブ蓄電池などへの最終フォールバック受電
                                energyStorage.receiveEnergy(maxReceive, false);
                            }
                        }
                    }
                }
            }
        }

        // 液体搬出 (全方向へ)
        if (!fluidTank.isEmpty()) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                net.neoforged.neoforge.fluids.capability.IFluidHandler neighborFluid = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
                if (neighborFluid != null) {
                    FluidStack toDrain = fluidTank.drain(100000000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                    int accepted = neighborFluid.fill(toDrain, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    fluidTank.drain(accepted, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        // アイテム搬出 (下方向へ)
        net.neoforged.neoforge.items.IItemHandler neighborItem = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, worldPosition.below(), net.minecraft.core.Direction.UP);
        if (neighborItem != null) {
            for (int i = 4; i <= 19; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack remainder = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(neighborItem, stack, false);
                    inventory.setStackInSlot(i, remainder);
                }
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

    public it.unimi.dsi.fastutil.objects.Object2IntMap<Item> getInstalledUpgrades() {
        return installedUpgrades;
    }

    public void toggleActive() {
        this.isActive = !this.isActive;
        this.setChanged();
    }

    public boolean canAddUpgrade(Item item, int amount) {
        Item baseItem = item instanceof com.example.virtualexplorer.item.CompressedUpgradeItem cui ? cui.getBaseUpgrade() : item;
        int multiplier = item instanceof com.example.virtualexplorer.item.CompressedUpgradeItem cui ? cui.getMultiplier() : 1;

        // アップグレードかどうかチェック
        if (!(item instanceof com.example.virtualexplorer.item.UpgradeItem) && !(item instanceof com.example.virtualexplorer.item.CompressedUpgradeItem)) {
            return false;
        }

        // 固有アップグレードは各種1枚までインストール可能
        java.util.Set<Item> exclusiveGroup = java.util.Set.of(
            com.example.virtualexplorer.init.ItemInit.UPGRADE_ARCHEOLOGY.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_SILK_TOUCH.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_INFINITE.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_STRUCTURE_INTEREST.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_DEMOLITION_MASTER.get()
        );

        if (exclusiveGroup.contains(baseItem)) {
            // 既に自分自身が入っているかチェック
            if (hasUpgrade(baseItem)) {
                return false; 
            }
            if (amount * multiplier > 1) return false; // 固有アップグレードは1枚のみ
        }

        // 製図家のコンパスは上限5枚まで
        if (baseItem == com.example.virtualexplorer.init.ItemInit.RECYCLE_UPGRADE.get()) {
            int current = installedUpgrades.getInt(baseItem);
            if (current + (amount * multiplier) > 5) {
                return false;
            }
        }

        // 競合チェック: 幸運 vs シルクタッチ
        boolean isFortune = baseItem == com.example.virtualexplorer.init.ItemInit.UPGRADE_FORTUNE.get();
        if (isFortune && hasUpgrade(com.example.virtualexplorer.init.ItemInit.UPGRADE_SILK_TOUCH.get())) {
            return false;
        }
        boolean isSilk = baseItem == com.example.virtualexplorer.init.ItemInit.UPGRADE_SILK_TOUCH.get();
        if (isSilk && hasUpgrade(com.example.virtualexplorer.init.ItemInit.UPGRADE_FORTUNE.get())) {
            return false;
        }
        
        return true;
    }

    public boolean addUpgrade(Item item, int amount) {
        if (!canAddUpgrade(item, amount)) return false;

        Item baseItem = item instanceof com.example.virtualexplorer.item.CompressedUpgradeItem cui ? cui.getBaseUpgrade() : item;
        int multiplier = item instanceof com.example.virtualexplorer.item.CompressedUpgradeItem cui ? cui.getMultiplier() : 1;

        installedUpgrades.put(baseItem, installedUpgrades.getInt(baseItem) + (amount * multiplier));
        setChanged();

        // 特殊な「別の処理」: 相互排他グループがインストールされた時の演出
        java.util.Set<Item> exclusiveGroup = java.util.Set.of(
            com.example.virtualexplorer.init.ItemInit.UPGRADE_ARCHEOLOGY.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_SILK_TOUCH.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_INFINITE.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_STRUCTURE_INTEREST.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_DEMOLITION_MASTER.get()
        );

        if (exclusiveGroup.contains(baseItem) && level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
            // Block.useItemOn以外（GUI経由）でも通知が出るように、近くのプレイヤーにメッセージを送ることも可能ですが、
            // 基本的にGUIを操作しているプレイヤーにだけ伝われば良いので、ここではSoundで十分と判断。
        }

        return true;
    }

    public boolean teleportToMap(ItemStack mapStack, Player player) {
        if (level == null || level.isClientSide) return false;
        
        MapId mapId = mapStack.get(net.minecraft.core.component.DataComponents.MAP_ID);
        if (mapId == null) return false;
        
        MapItemSavedData data = net.minecraft.world.item.MapItem.getSavedData(mapId, level);
        if (data == null) return false;

        int targetX = data.centerX;
        int targetZ = data.centerZ;
        int targetChunkX = targetX >> 4;
        int targetChunkZ = targetZ >> 4;

        double dx = targetChunkX - currentChunkX;
        double dz = targetChunkZ - currentChunkZ;
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        int cost = distance * 1000;

        if (energyStorage.getEnergyStored() < cost) {
            player.displayClientMessage(Component.translatable("gui.virtualexplorer.status.4"), true);
            return false;
        }

        // 構造物チェック
        String structureName = "nature";
        boolean isTreasure = false;
        for (MapDecoration decoration : data.getDecorations()) {
            var type = decoration.type();
            if (type.is(MapDecorationTypes.WOODLAND_MANSION)) structureName = "mansion";
            else if (type.is(MapDecorationTypes.OCEAN_MONUMENT)) structureName = "monument";
            else if (type.is(MapDecorationTypes.TRIAL_CHAMBERS)) structureName = "trial_chambers";
            else if (type.is(MapDecorationTypes.RED_X)) {
                structureName = "buried_treasure";
                isTreasure = true;
            }
        }

        if (!structureName.equals("nature")) {
            com.example.virtualexplorer.util.StructureTrackerWSD wsd = com.example.virtualexplorer.util.StructureTrackerWSD.get((net.minecraft.server.level.ServerLevel) level);
            if (wsd.isExplored(structureName, new ChunkPos(targetChunkX, targetChunkZ))) {
                player.displayClientMessage(Component.literal("§c構造物は探索済みです"), true);
                return false;
            }
            
            // 構造物モードへ移行
            this.state = ExplorationState.HOVERING_STRUCTURE;
            this.targetStructurePos = new BlockPos(targetX, 64, targetZ);
            this.targetStructureName = structureName;
            this.hoverCount = isTreasure ? 1 : 10;
        }

        energyStorage.extractEnergy(cost, false);
        this.currentChunkX = targetChunkX;
        this.currentChunkZ = targetChunkZ;
        
        player.displayClientMessage(Component.literal("§aTeleported to map! (Cost: " + cost + " FE)"), true);
        
        updateGridColors();
        setChanged();
        return true;
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
        net.minecraft.nbt.ListTag biomeList = new net.minecraft.nbt.ListTag();
        for (String b : gridBiomes) {
            biomeList.add(net.minecraft.nbt.StringTag.valueOf(b == null ? "minecraft:plains" : b));
        }
        tag.put("GridBiomes", biomeList);
        
        tag.putInt("ExplorationState", state.ordinal());
        tag.putInt("HoverCount", hoverCount);
        tag.putString("TargetStructureName", targetStructureName);
        if (targetStructurePos != null) {
            tag.putLong("TargetStructurePos", targetStructurePos.asLong());
        }

        CompoundTag upgradesTag = new CompoundTag();
        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Item> entry : installedUpgrades.object2IntEntrySet()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            upgradesTag.putInt(id.toString(), entry.getIntValue());
        }
        tag.put("InstalledUpgrades", upgradesTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        installedUpgrades.clear();
        if (tag.contains("InstalledUpgrades")) {
            CompoundTag upgradesTag = tag.getCompound("InstalledUpgrades");
            for (String key : upgradesTag.getAllKeys()) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(key));
                if (item != Items.AIR) {
                    installedUpgrades.put(item, upgradesTag.getInt(key));
                }
            }
        }

        if (tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            if (invTag.contains("Items", 9)) {
                net.minecraft.nbt.ListTag list = invTag.getList("Items", 10);
                boolean isLegacy15 = invTag.contains("Size") && invTag.getInt("Size") == 15;
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompound(i);
                    int slot = itemTag.getInt("Slot");
                    ItemStack stack = ItemStack.parseOptional(registries, itemTag);
                    if (stack.isEmpty()) continue;

                    if (isLegacy15) {
                        // 旧インベントリ(15枠)からのマイグレーション
                        if (slot == 0) {
                            inventory.setStackInSlot(0, stack);
                        } else if (slot >= 1 && slot <= 4) {
                            // 旧アップグレードスロットのアイテムは内部データへ変換
                            int count = stack.getCount();
                            if (stack.getItem() instanceof com.example.virtualexplorer.item.CompressedUpgradeItem cui) {
                                count *= cui.getMultiplier();
                                installedUpgrades.put(cui.getBaseUpgrade(), installedUpgrades.getInt(cui.getBaseUpgrade()) + count);
                            } else {
                                installedUpgrades.put(stack.getItem(), installedUpgrades.getInt(stack.getItem()) + count);
                            }
                        } else if (slot == 5) { // Map
                            inventory.setStackInSlot(2, stack);
                        } else if (slot >= 6 && slot <= 13) { // Output
                            int newSlot = slot - 2; // 6->4, 13->11
                            inventory.setStackInSlot(newSlot, stack);
                        } else if (slot == 14) { // Filter
                            inventory.setStackInSlot(3, stack);
                        }
                    } else {
                        // 通常読み込み(12枠)
                        if (slot >= 0 && slot < inventory.getSlots()) {
                            inventory.setStackInSlot(slot, stack);
                        }
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
        if (tag.contains("GridBiomes", 9)) {
            net.minecraft.nbt.ListTag biomeList = tag.getList("GridBiomes", 8);
            for (int i = 0; i < Math.min(biomeList.size(), gridBiomes.length); i++) {
                gridBiomes[i] = biomeList.getString(i);
            }
        }

        if (tag.contains("ExplorationState")) {
            state = ExplorationState.values()[tag.getInt("ExplorationState")];
        }
        if (tag.contains("HoverCount")) {
            hoverCount = tag.getInt("HoverCount");
        }
        if (tag.contains("TargetStructureName")) {
            targetStructureName = tag.getString("TargetStructureName");
        }
        if (tag.contains("TargetStructurePos")) {
            targetStructurePos = BlockPos.of(tag.getLong("TargetStructurePos"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag upgradesTag = new CompoundTag();
        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Item> entry : installedUpgrades.object2IntEntrySet()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            upgradesTag.putInt(id.toString(), entry.getIntValue());
        }
        tag.put("InstalledUpgrades", upgradesTag);
        if (this.targetStructureName != null) {
            tag.putString("TargetStructureName", this.targetStructureName);
        }
        tag.putIntArray("GridColors", gridColors);
        net.minecraft.nbt.ListTag biomeList = new net.minecraft.nbt.ListTag();
        for (String b : gridBiomes) {
            biomeList.add(net.minecraft.nbt.StringTag.valueOf(b == null ? "minecraft:plains" : b));
        }
        tag.put("GridBiomes", biomeList);
        return tag;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("TargetStructureName")) {
            this.targetStructureName = tag.getString("TargetStructureName");
        }
        installedUpgrades.clear();
        if (tag.contains("InstalledUpgrades")) {
            CompoundTag upgradesTag = tag.getCompound("InstalledUpgrades");
            for (String key : upgradesTag.getAllKeys()) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(key));
                if (item != Items.AIR) {
                    installedUpgrades.put(item, upgradesTag.getInt(key));
                }
            }
        }
        if (tag.contains("GridColors")) {
            gridColors = tag.getIntArray("GridColors");
        }
        if (tag.contains("GridBiomes", 9)) {
            net.minecraft.nbt.ListTag biomeList = tag.getList("GridBiomes", 8);
            for (int i = 0; i < Math.min(biomeList.size(), gridBiomes.length); i++) {
                gridBiomes[i] = biomeList.getString(i);
            }
        }
        syncUpgradesToHiddenSlots();
    }

    private void syncUpgradesToHiddenSlots() {
        Item[] baseUpgrades = {
            com.example.virtualexplorer.init.ItemInit.SPEED_UPGRADE.get(),
            com.example.virtualexplorer.init.ItemInit.RECYCLE_UPGRADE.get(),
            com.example.virtualexplorer.init.ItemInit.RADAR_UPGRADE.get(),
            com.example.virtualexplorer.init.ItemInit.PARALLEL_UPGRADE.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_FLUID.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_ARCHEOLOGY.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_DEMOLITION_MASTER.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_STRUCTURE_INTEREST.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_INFINITE.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_FORTUNE.get(),
            com.example.virtualexplorer.init.ItemInit.UPGRADE_SILK_TOUCH.get()
        };
        
        for (int i = 0; i < baseUpgrades.length; i++) {
            Item upgrade = baseUpgrades[i];
            int count = installedUpgrades.getInt(upgrade);
            if (count > 0) {
                inventory.setStackInSlot(20 + i, new ItemStack(upgrade, count));
            } else {
                inventory.setStackInSlot(20 + i, ItemStack.EMPTY);
            }
        }
    }
}
