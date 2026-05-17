package com.example.virtualexplorer.inventory;

import com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity;
import com.example.virtualexplorer.init.BlockInit;
import com.example.virtualexplorer.init.MenuInit;
import com.example.virtualexplorer.inventory.GUISettings;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class VirtualMappingTableMenu extends AbstractContainerMenu {

    private final VirtualMappingTableBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final net.minecraft.world.inventory.ContainerData data;

    public VirtualMappingTableMenu(int windowId, Inventory playerInventory, net.minecraft.network.RegistryFriendlyByteBuf buf) {
        this(windowId, playerInventory, playerInventory.player.level().getBlockEntity(buf.readBlockPos()), new net.minecraft.world.inventory.SimpleContainerData(42));
    }

    public VirtualMappingTableMenu(int windowId, Inventory playerInventory, BlockEntity entity, net.minecraft.world.inventory.ContainerData data) {
        super(MenuInit.VIRTUAL_MAPPING_TABLE_MENU.get(), windowId);
        this.blockEntity = (VirtualMappingTableBlockEntity) entity;
        this.levelAccess = (this.blockEntity != null) ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()) : ContainerLevelAccess.NULL;
        this.data = data;
        
        this.addDataSlots(data); // ここで登録

        IItemHandler inventory = (this.blockEntity != null) ? blockEntity.getInventory() : new net.neoforged.neoforge.items.ItemStackHandler(31);
        
        // 0: モジュールスロット
        this.addSlot(new SlotItemHandler(inventory, 0, GUISettings.SLOT_MODULE_X, GUISettings.SLOT_MODULE_Y));
        // 1: インストール用スロット
        this.addSlot(new SlotItemHandler(inventory, 1, GUISettings.SLOT_UPGRADE_X, GUISettings.SLOT_UPGRADE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (blockEntity == null) return super.mayPlace(stack);
                // addUpgradeをシミュレートして配置可能か判定
                return blockEntity.canAddUpgrade(stack.getItem(), stack.getCount());
            }
        });
        // 2: 地図スロット
        this.addSlot(new SlotItemHandler(inventory, 2, GUISettings.SLOT_MAP_X, GUISettings.SLOT_MAP_Y));
        // 3: フィルタースロット
        this.addSlot(new SlotItemHandler(inventory, 3, GUISettings.SLOT_FILTER_X, GUISettings.SLOT_FILTER_Y));
        
        // 4〜19: 出力スロット (4x4)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                this.addSlot(new SlotItemHandler(inventory, 4 + col + row * 4, 
                    GUISettings.SLOT_OUTPUT_X + col * GUISettings.SLOT_OUTPUT_SPACING, 
                    GUISettings.SLOT_OUTPUT_Y + row * GUISettings.SLOT_OUTPUT_SPACING) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // 手動搬入を禁止
                    }
                });
            }
        }
        
        // プレイヤーインベントリ
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                    GUISettings.PLAYER_INV_X + col * 18, 
                    GUISettings.PLAYER_INV_Y + row * 18));
            }
        }

        // ホットバー
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 
                GUISettings.PLAYER_INV_X + col * 18, 
                GUISettings.PLAYER_INV_Y + 58));
        }
    }

    public int getStatusId() {
        return this.data.get(35);
    }

    public int getTargetStructureId() {
        return this.data.get(36);
    }

    public int getGridColor(int index) {
        return this.data.get(10 + index);
    }

    public int getScaledProgress(int scale) {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        return maxProgress != 0 && progress != 0 ? progress * scale / maxProgress : 0;
    }

    public int getRawProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public int getCurrentChunkX() {
        return (this.data.get(37) & 0xFFFF) | (this.data.get(38) << 16);
    }

    public int getCurrentChunkZ() {
        return (this.data.get(39) & 0xFFFF) | (this.data.get(40) << 16);
    }

    public int getEnergy() {
        return (this.data.get(3) << 16) | (this.data.get(2) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.data.get(5) << 16) | (this.data.get(4) & 0xFFFF);
    }

    public int getScaledEnergy(int height) {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        return maxEnergy != 0 && energy != 0 ? (int)((long)energy * height / maxEnergy) : 0;
    }

    public int getFluidAmount() {
        return (this.data.get(7) << 16) | (this.data.get(6) & 0xFFFF);
    }

    public int getFluidId() {
        return this.data.get(8);
    }

    public int getScaledFluid(int height) {
        int amount = getFluidAmount();
        int maxAmount = 10000; // Tank capacity
        return maxAmount != 0 && amount != 0 ? (int)((long)amount * height / maxAmount) : 0;
    }

    public int getEnergyCost() {
        return this.data.get(41);
    }

    private void addPlayerInventory(IItemHandler playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new SlotItemHandler(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new SlotItemHandler(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, BlockInit.VIRTUAL_MAPPING_TABLE.get());
    }

    public VirtualMappingTableBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean isActive() {
        return this.data.get(9) == 1;
    }

    @Override
    public boolean clickMenuButton(net.minecraft.world.entity.player.Player player, int id) {
        if (id == 0) {
            this.blockEntity.toggleActive();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < 20) { // BlockEntity -> Player (Module, Install, Map, Filter, Outputs)
                if (!this.moveItemStackTo(stack, 20, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 4, false)) { // Player -> BlockEntity (Special slots only)
                // Note: Slots 4-19 (Outputs) are skipped because we don't want to move player items into output slots manually via shift-click usually, 
                // but the moveItemStackTo range [0, 4) only covers special slots.
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}
