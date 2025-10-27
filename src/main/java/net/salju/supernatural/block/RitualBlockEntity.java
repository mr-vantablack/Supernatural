package net.salju.supernatural.block;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.salju.supernatural.init.SupernaturalBlockEntities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;

public class RitualBlockEntity extends BaseContainerBlockEntity {
	private NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(1, ItemStack.EMPTY);
	private static int timer = 0;
    private long lastUsedTime = 0;
    private long cooldownDuration;

	public RitualBlockEntity(BlockPos pos, BlockState state) {
		super(SupernaturalBlockEntities.RITUAL.get(), pos, state);
        setCooldownDuration(400);
	}
    public void doCooldown(){
        if (level != null) this.lastUsedTime = level.getGameTime();
    }
    public void setCooldownDuration(int duration) {
        this.cooldownDuration = duration;
    }
    public boolean isOnCooldown() {
        if (level == null) return false;
        return level.getGameTime() - lastUsedTime < cooldownDuration;
    }
    public long getRemainingCooldown() {
        if (level == null) return 0;
        return Math.max(0, cooldownDuration - (level.getGameTime() - lastUsedTime));
    }
    public String getCooldownMessage() {
        if (level == null) return "";
        long ticksLeft = getRemainingCooldown();
        long secondsLeft = (ticksLeft + 19) / 20;
        var pos = getBlockPos();
        spawnParticles(level, pos, ParticleTypes.SMOKE, 2, 0.3, 0.02);
      /*
         if (level.random.nextInt(5) == 0) {
            spawnParticles(level, pos, ParticleTypes.PORTAL, 1, 0.1, 0.01);
        }

        if (secondsLeft > 60) {
            long minutes = secondsLeft / 60;
            long seconds = secondsLeft % 60;
            return String.format("§4Нет ответа... %d:%02d", minutes, seconds);
        } else {
            return String.format("§4Нет ответа... %d сек.", secondsLeft);
        }*/
        return "";
    }
    private void spawnParticles(Level level, BlockPos pos, ParticleOptions particle,
                                int count, double spread, double speed) {
        if (level.isClientSide()) {
            for (int i = 0; i < count; i++) {
                double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * spread;
                double y = pos.getY() + 1.0 + (level.random.nextDouble() - 0.5) * spread * 0.5;
                double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * spread;

                level.addParticle(particle, x, y, z,
                        (level.random.nextDouble() - 0.5) * speed,
                        level.random.nextDouble() * speed,
                        (level.random.nextDouble() - 0.5) * speed);
            }
        } else {
            ServerLevel serverLevel = (ServerLevel) level;
            serverLevel.sendParticles(particle,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    count, spread, spread * 0.5, spread, speed);
        }
    }
	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		ContainerHelper.saveAllItems(tag, this.stacks);
        tag.putLong("LastUsedTime", lastUsedTime);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, this.stacks);
        this.lastUsedTime = tag.getLong("LastUsedTime");
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection queen, ClientboundBlockEntityDataPacket packet) {
		if (packet != null && packet.getTag() != null) {
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
			ContainerHelper.loadAllItems(packet.getTag(), this.stacks);
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = new CompoundTag();
		ContainerHelper.saveAllItems(tag, this.stacks);
		return tag;
	}

	@Override
	public Component getDefaultName() {
		return Component.translatable("block.supernatural.ritual_altar");
	}

	@Override
	public AbstractContainerMenu createMenu(int i, Inventory bag) {
		return null;
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return this.getItem(0).isEmpty();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public ItemStack getItem(int i) {
		return this.stacks.get(i);
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack stack) {
		return this.isEmpty();
	}

	@Override
	public ItemStack removeItemNoUpdate(int i) {
		return ContainerHelper.takeItem(this.stacks, i);
	}

	@Override
	public ItemStack removeItem(int i, int e) {
		this.updateBlock();
		return this.getItem(i).is(Items.AMETHYST_SHARD) ? ItemStack.EMPTY : ContainerHelper.removeItem(this.stacks, i, e);
	}

	@Override
	public void setItem(int i, ItemStack stack) {
		this.stacks.set(i, stack.copy());
		this.getItem(i).setCount(1);
		this.updateBlock();
	}

	@Override
	public void clearContent() {
		this.stacks.clear();
		this.updateBlock();
	}

	public void updateBlock() {
		this.setChanged();
		this.getLevel().updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
	}

	public void dropItem(int i) {
		Containers.dropItemStack(this.getLevel(), (double) this.getBlockPos().getX(), (double) this.getBlockPos().getY(), (double) this.getBlockPos().getZ(), this.getItem(i));
		this.updateBlock();
	}

	public void cloneItem(ItemStack stack) {
		Containers.dropItemStack(this.getLevel(), (double) this.getBlockPos().getX(), (double) this.getBlockPos().getY(), (double) this.getBlockPos().getZ(), stack);
	}

	public static void tick(Level world, BlockPos pos, BlockState state, RitualBlockEntity target) {
		if (world instanceof ServerLevel lvl && !target.isEmpty() && !target.isOnCooldown()) {
			++target.timer;
			if (target.timer >= 5) {
				lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, (pos.getX() + 0.5), (pos.getY() + 0.5), (pos.getZ() + 0.5), 2, 0.1, 0.15, 0.1, 0);
				target.timer = 0;
			}
		}
	}
}
