package com.example.virtualexplorer.inventory;

import com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity;
import com.example.virtualexplorer.init.BlockInit;
import com.example.virtualexplorer.init.MenuInit;
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
        this(windowId, playerInventory, playerInventory.player.level().getBlockEntity(buf.readBlockPos()), new net.minecraft.world.inventory.SimpleContainerData(37));
    }

    public VirtualMappingTableMenu(int windowId, Inventory playerInventory, BlockEntity entity, net.minecraft.world.inventory.ContainerData data) {
        super(MenuInit.VIRTUAL_MAPPING_TABLE_MENU.get(), windowId);
        this.blockEntity = (VirtualMappingTableBlockEntity) entity;
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = data;
        
        this.addDataSlots(data); // ここで登録

        IItemHandler inventory = blockEntity.getInventory();
        
        // 0: モジュールスロット
        this.addSlot(new SlotItemHandler(inventory, 0, 30, 20));
        // 1〜4: アップグレードスロット (縦に並べる)
        this.addSlot(new SlotItemHandler(inventory, 1, 8, 20));
        this.addSlot(new SlotItemHandler(inventory, 2, 8, 38));
        this.addSlot(new SlotItemHandler(inventory, 3, 8, 56));
        this.addSlot(new SlotItemHandler(inventory, 4, 8, 74));
        // 5: 地図スロット
        this.addSlot(new SlotItemHandler(inventory, 5, 30, 38));
        
        // 14: フィルタースロット
        this.addSlot(new SlotItemHandler(inventory, 14, 30, 56));
        
        // 6〜13: 出力スロット (4x2)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                this.addSlot(new SlotItemHandler(inventory, 6 + col + row * 4, 100 + col * 18, 20 + row * 18));
            }
        }
        
        int playerInvX = 48; // (256 - 162) / 2
        int playerInvY = 104;

        // プレイヤーインベントリ
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, playerInvX + col * 18, playerInvY + row * 18));
            }
        }

        // ホットバー
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, playerInvX + col * 18, playerInvY + 58));
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

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        int progressBarSize = 24; // 矢印の幅
        return maxProgress != 0 && progress != 0 ? progress * progressBarSize / maxProgress : 0;
    }

    public int getRawProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
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
            if (index < 15) { // BlockEntity -> Player
                if (!this.moveItemStackTo(stack, 15, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 15, false)) { // Player -> BlockEntity
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
