package net.salju.supernatural.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.salju.supernatural.init.SupernaturalTags;
import net.salju.supernatural.init.SupernaturalItems;
import net.salju.supernatural.init.SupernaturalEffects;
import net.salju.supernatural.init.SupernaturalDamageTypes;
import net.salju.supernatural.init.SupernaturalConfig;
import net.salju.supernatural.init.SupernaturalBlocks;
import net.salju.supernatural.events.SupernaturalManager;
import net.salju.supernatural.entity.Angel;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public class Rituals {
	public static void doRitual(ItemStack stack, ItemStack offer, ServerLevel lvl, Player player, BlockPos pos) {
		if (lvl.getBlockEntity(pos) instanceof RitualBlockEntity target) {
			ItemStack off = player.getOffhandItem();
			if (lvl.getBrightness(LightLayer.BLOCK, pos) < 6 && (lvl.getBrightness(LightLayer.SKY, pos) < 6 || !lvl.isDay())) {
				int i = SupernaturalManager.getPower(lvl, pos);
				int e = SupernaturalManager.getSoulLevel(SupernaturalManager.getSoulgem(offer));
				if (i == 28 && e >= 0 && stack.is(SupernaturalItems.GRAVE_SOIL.get())) {
					defaultResult(target, offer, lvl, player, pos);
					if (SupernaturalConfig.SACRIFICE.get()) {
						Mob sacrifice = getSacrifice(lvl, offer, target.getRenderBoundingBox().inflate(12.85D));
						(sacrifice != null ? sacrifice : player).hurt(SupernaturalDamageTypes.causeRitualDamage(player.level().registryAccess(), player), Float.MAX_VALUE);
					}
					summonMob(lvl, pos.above(), offer);
					lvl.playSound(null, pos, SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS, 1.0F, (float) (0.8F + (Math.random() * 0.2)));
				}
                else if (i == 12 && e >= 3 && stack.is(Items.REDSTONE_BLOCK)) {
                    defaultResult(target, offer, lvl, player, pos);
                    target.setItem(0, new ItemStack(Items.LAPIS_BLOCK));
                }
                else if (i == 20 && e >= 5 && stack.is(Items.GOLDEN_APPLE)) {
                    defaultResult(target, offer, lvl, player, pos);
                    target.setItem(0, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
                }
                else if (i == 12 && e >= 2 && stack.is(Items.MUSIC_DISC_13)) {
                    defaultResult(target, offer, lvl, player, pos);
                    target.setItem(0, new ItemStack(Items.MUSIC_DISC_11));
                }
                else if (i == 8 && e >= 2 && stack.is(Items.OBSIDIAN)) {
                    defaultResult(target, offer, lvl, player, pos);
                    target.setItem(0, new ItemStack(Items.CRYING_OBSIDIAN));
                }
                else if (i == 20 && e >= 5 && stack.is(Items.ARMOR_STAND)) {
                    defaultResult(target, offer, lvl, player, pos);
                    target.setItem(0, new ItemStack(SupernaturalItems.ANGEL_STATUE.get()));
                }
                else if (i == 8 && e >= 1 && stack.is(Items.EXPERIENCE_BOTTLE)) {
                    defaultResult(target, offer, lvl, player, pos);
                    target.setItem(0, new ItemStack((SupernaturalItems.ECTOPLASM.get())));
                }
                else if (i == 20 && e >= 5 && stack.is(SupernaturalItems.BLOOD.get()) && !SupernaturalManager.isVampire(player) && !player.isHurt())
                {
					Player victim = lvl.getPlayerByUUID(SupernaturalManager.getUUID(stack));
					Goat goat = getGoat(lvl, target.getRenderBoundingBox().inflate(12.85D));
					if (victim != null && victim == player && (goat != null || !SupernaturalConfig.SACRIFICE.get())) {
						defaultResult(target, offer, lvl, player, pos);
						player.hurt(player.damageSources().magic(), 0.25F);
						player.setHealth(1.0F);
						SupernaturalManager.setVampire(player, true);
						target.setItem(0, new ItemStack(Items.GLASS_BOTTLE));
						if (SupernaturalConfig.SACRIFICE.get()) {
							goat.hurt(SupernaturalDamageTypes.causeRitualDamage(goat.level().registryAccess(), player), Float.MAX_VALUE);
						}
					}
				}
                else if (i == 12 && e >= 1 && (stack.is(Items.IRON_INGOT) || stack.is(Items.COPPER_INGOT))) {
					defaultResult(target, offer, lvl, player, pos);
					target.setItem(0, new ItemStack(stack.is(Items.IRON_INGOT) ? Items.GOLD_INGOT : Items.IRON_INGOT));
				} else if (i == 12 && e >= 1 && stack.is(SupernaturalItems.VAMPIRE_DUST.get())) {
					defaultResult(target, offer, lvl, player, pos);
					BlockPos top = BlockPos.containing((pos.getX() + 3), (pos.getY() - 1), (pos.getZ() + 3));
					BlockPos bot = BlockPos.containing((pos.getX() - 3), (pos.getY() - 1), (pos.getZ() - 3));
					for (BlockPos poz : BlockPos.betweenClosed(top, bot)) {
						if (lvl.getBlockState(poz).is(Blocks.SOUL_SAND) || lvl.getBlockState(poz).is(Blocks.SOUL_SOIL)) {
							lvl.setBlock(poz, SupernaturalBlocks.GRAVE_SOIL.get().defaultBlockState(), 3);
						}
					}
				} else if (SupernaturalManager.isVampire(player) && off.is(SupernaturalItems.BLOOD.get())) {
					Player victim = lvl.getPlayerByUUID(SupernaturalManager.getUUID(off));
					if (victim != null) {
						if (i == 12 && e >= 4 && stack.is(SupernaturalItems.ECTOPLASM.get())) {
                            if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
							defaultResult(target, offer, lvl, player, pos);
                            lvl.playSound(null, pos, SoundEvents.ALLAY_DEATH, SoundSource.AMBIENT, 1.0f, 0.1f);
                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 9, false, false));
							victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
							victim.addEffect(new MobEffectInstance(SupernaturalEffects.POSSESSION.get(), 6000, 0, false, false));
							for (int r = 0; r < 3; ++r) {
								BlockPos poz = victim.blockPosition().offset(-2 + lvl.random.nextInt(5), 1, -2 + lvl.random.nextInt(5));
								EntityType.VEX.spawn(lvl, poz, MobSpawnType.MOB_SUMMONED);
							}
						} else if (i == 16 && e >= 3 && stack.is(Items.BLAZE_POWDER)) {
                            if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
							defaultResult(target, offer, lvl, player, pos);
                            lvl.playSound(null, pos, SoundEvents.BLAZE_DEATH, SoundSource.AMBIENT, 1.0f, 0.1f);
                            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 0, false, false));
						}else if (i == 20 && e >= 5 && stack.is(Items.BONE)) {
                            if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                            defaultResult(target, offer, lvl, player, pos);
                            lvl.playSound(null, pos, SoundEvents.WITHER_DEATH, SoundSource.AMBIENT, 1.0f, 0.1f);
                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 9, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0, false, false));
                        } else if (i == 12 && e >= 4 && stack.is(Items.MAGMA_CREAM)) {
                            if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                            defaultResult(target, offer, lvl, player, pos);
                            lvl.playSound(null, pos, SoundEvents.WITHER_AMBIENT, SoundSource.AMBIENT, 1.0f, 0.1f);
                            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12000, 0, false, false));
                        }else if (i == 12 && e >= 4 && stack.is(Items.ROTTEN_FLESH)) {
                            if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                            defaultResult(target, offer, lvl, player, pos);
                            lvl.playSound(null, pos, SoundEvents.ZOMBIE_HORSE_DEATH, SoundSource.AMBIENT, 1.0f, 0.1f);
                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 9, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.HUNGER, 8000, 1, false, false));
                        }else if (i == 20 && e >= 5 && stack.is(Items.NETHER_STAR)) {
                            if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                            defaultResult(target, offer, lvl, player, pos);
                            lvl.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.AMBIENT, 1.0f, 0.1f);
                            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 6000, 1, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12000, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12000, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 12000, 2, false, false));
                        }else if (i == 12 && e >= 4 && stack.is(Items.DIAMOND)) {
                            if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                            defaultResult(target, offer, lvl, player, pos);
                            lvl.playSound(null, pos, SoundEvents.AMBIENT_CAVE.get(), SoundSource.AMBIENT, 1.0f, 0.1f);
                            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 8000, 1, false, false));
                        }else if (i == 12 && e >= 4 && stack.is(Items.CARVED_PUMPKIN)) {
                            if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                            defaultResult(target, offer, lvl, player, pos);
                            lvl.playSound(null, pos, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.AMBIENT, 1.0f, 0.1f);
                            ItemStack helmet = victim.getItemBySlot(EquipmentSlot.HEAD);
                            victim.blockPosition();
                            if (!helmet.isEmpty()) {
                                target.cloneItem(helmet);
                            }
                            victim.setItemSlot(EquipmentSlot.HEAD, stack);
                        }
					}
				}
                if (ModList.get().isLoaded("born_in_chaos_v1")) { chaosRituals(stack, offer, lvl, player, pos, target, i, e); }
                if (ModList.get().isLoaded("biomesoplenty")) { bopRituals(stack, offer, lvl, player, pos, target, i, e); }
			}
		}
	}
    private static void bopRituals(ItemStack stack, ItemStack offer, ServerLevel lvl, Player player, BlockPos pos, RitualBlockEntity target, int i, int e) {
        ItemStack off = player.getOffhandItem();
        if (i == 8 && e >= 1 && stack.is(Items.OAK_SAPLING)) {

            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("biomesoplenty", "dead_sapling")));
        }
        else if (i == 8 && e >= 1 && stack.is(getItem("biomesoplenty", "fir_sapling"))) {

            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("biomesoplenty", "umbran_sapling")));
        }
        else if (i == 8 && e >= 1 && (stack.is(Items.BROWN_MUSHROOM_BLOCK) || stack.is(Items.RED_MUSHROOM_BLOCK))) {

            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("biomesoplenty", "glowshroom_block")));
        }
        else if (i == 8 && e >= 1 && stack.is(Items.MOSS_BLOCK)) {

            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("biomesoplenty", "glowing_moss_block")));
        }
        else if (i == 8 && e >= 1 && stack.is(Items.NETHERRACK)) {

            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("biomesoplenty", "flesh")));
        }
        else if (i == 12 && e >= 2 && stack.is(Items.WATER_BUCKET)) {

            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("biomesoplenty", "blood_bucket")));

        }
    }
    private static void chaosRituals(ItemStack stack, ItemStack offer, ServerLevel lvl, Player player, BlockPos pos, RitualBlockEntity target, int i, int e){
        ItemStack off = player.getOffhandItem();
        if (i == 8 && e >= 1 && stack.is(Items.LEATHER)) {

            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("born_in_chaos_v1", "monster_skin")));
        }
        else if (i == 8 && e >= 1 && stack.is(Items.BLAZE_ROD)) {
            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("born_in_chaos_v1", "dark_rod")));
        }
        else if (i == 12 && e >= 3 && stack.is(Items.TOTEM_OF_UNDYING)) {
            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("born_in_chaos_v1", "death_totem")));
        }
        else if (i == 8 && e >= 1 && stack.is(Items.NETHERITE_SCRAP)) {
            defaultResult(target, offer, lvl, player, pos);
            target.setItem(0, new ItemStack(getItem("born_in_chaos_v1", "pieceofdarkmetal")));
        }
        else if (i == 12 && e >= 3 && stack.is(Items.COAL)) {
            defaultResult(target, offer, lvl, player, pos);
            lvl.playSound(null, pos, getSound("born_in_chaos_v1","haha_lord"), SoundSource.AMBIENT, 1.0f, 1.0f);
            target.setItem(0, new ItemStack(getItem("born_in_chaos_v1", "smoldering_infernal_ember")));
        }
        else if (SupernaturalManager.isVampire(player) && off.is(SupernaturalItems.BLOOD.get())) {
            Player victim = lvl.getPlayerByUUID(SupernaturalManager.getUUID(off));
            if (victim != null) {
                 if (i == 12 && e >= 4 && stack.is(getItem("born_in_chaos_v1", "fangofthe_hound_leader"))) {
                     if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                    defaultResult(target, offer, lvl, player, pos);
                     lvl.playSound(null, pos, getSound("born_in_chaos_v1","hound_death"), SoundSource.AMBIENT, 1.0f, 0.1f);
                    victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, false));
                    victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","light_rampage"), 6000, 1, false, false));
                }
                else if (i == 20 && e >= 5 && stack.is(getItem("born_in_chaos_v1", "lifestealer_bone"))) {
                     if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                    defaultResult(target, offer, lvl, player, pos);
                     lvl.playSound(null, pos, getSound("born_in_chaos_v1","lifestealer_scream_ap"), SoundSource.AMBIENT, 1.0f, 0.1f);
                    victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, false));
                     victim.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 12000, 0, false, false));
                    victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","vampiric_touch"), 12000, 1, false, false));
                }
                 else if (i == 20 && e >= 5 && stack.is(getItem("born_in_chaos_v1", "orbofthe_summoner"))) {
                     if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                     defaultResult(target, offer, lvl, player, pos);
                     lvl.playSound(null, pos, getSound("born_in_chaos_v1","chaos_spirit_haunt"), SoundSource.AMBIENT, 1.0f, 0.1f);
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","terrifying_presence"), 1000, 1, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","stranglehold"), 100, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","gaze_of_terror"), 1000, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","bad_feeling"), 6000, 1, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","magic_depletion"), 12000, 0, false, false));
                 }
                 else if (i == 20 && e >= 5 && stack.is(getItem("born_in_chaos_v1", "transmuting_elixir"))) {
                     if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                     defaultResult(target, offer, lvl, player, pos);
                     lvl.playSound(null, pos, getSound("born_in_chaos_v1","missionary_teleport"), SoundSource.AMBIENT, 1.0f, 0.1f);
                     victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                     victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, false));
                     victim.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 12000, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","furious_rampage"), 12000, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","vampiric_touch"), 12000, 0, false, false));
                     victim.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 12000, 2, false, false));
                 }
                else if (i == 20 && e >= 4 && stack.is(getItem("born_in_chaos_v1", "sea_terror_eye"))) {
                     if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                     defaultResult(target, offer, lvl, player, pos);
                     lvl.playSound(null, pos, getSound("born_in_chaos_v1","glutton_fish_death"), SoundSource.AMBIENT, 1.0f, 0.1f);
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","stranglehold"), 300, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","gaze_of_terror"), 6000, 0, false, false));
                }
                 else if (i == 16 && e >= 4 && stack.is(getItem("born_in_chaos_v1", "ethereal_spirit"))) {
                     if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                     defaultResult(target, offer, lvl, player, pos);
                     lvl.playSound(null, pos, getSound("born_in_chaos_v1","spirit_idle"), SoundSource.AMBIENT, 1.0f, 0.1f);
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","stranglehold"), 100, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","gaze_of_terror"), 100, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","soul_stratification"), 6000, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","cursed_mark"), 6000, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","magic_depletion"), 12000, 0, false, false));
                 }
                 else if (i == 16 && e >= 4 && stack.is(getItem("born_in_chaos_v1", "corpse_maggot"))) {
                     if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                     defaultResult(target, offer, lvl, player, pos);
                     lvl.playSound(null, pos, getSound("born_in_chaos_v1","stalker_death2"), SoundSource.AMBIENT, 1.0f, 0.1f);
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","stranglehold"), 100, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","gaze_of_terror"), 800, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","undead_summonun"), 20, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","rotten_smell"), 12000, 0, false, false));
                     victim.addEffect(new MobEffectInstance(MobEffects.HUNGER, 12000, 0, false, false));
                 }
                 else if (i == 20 && e >= 5 && stack.is(getItem("born_in_chaos_v1", "fel_soil"))) {
                     if (!player.isCreative()) player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GLASS_BOTTLE));
                     defaultResult(target, offer, lvl, player, pos);
                     lvl.playSound(null, pos, getSound("born_in_chaos_v1","haha_lord"), SoundSource.AMBIENT, 1.0f, 0.3f);
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","stranglehold"), 100, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","gaze_of_terror"), 100, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","infernal_flame"), 1200, 0, false, false));
                     victim.addEffect(new MobEffectInstance(getEffect("born_in_chaos_v1","magic_depletion"), 1200, 0, false, false));
                 }
            }
        }
    }
    private static Item getItem(String modid, String itemid){
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(modid, itemid));
    }
    private static MobEffect getEffect(String modid, String effectid){
        return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(modid, effectid));
    }
    private static SoundEvent getSound(String modid, String soundid){
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(modid, soundid));
    }
	private static void defaultResult(RitualBlockEntity target, ItemStack stack, ServerLevel lvl, Player player, BlockPos pos) {
		target.clearContent();
        target.doCooldown();
		lvl.playSound(null, pos, SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS, 1.0F, (float) (0.8F + (Math.random() * 0.2)));
		lvl.sendParticles(ParticleTypes.SOUL, (pos.getX() + 0.5), (pos.getY() + 0.5), (pos.getZ() + 0.5), 21, 3, 1, 3, 0);
        int soul = SupernaturalManager.getSoulLevel(SupernaturalManager.getSoulgem(stack));
        target.setCooldownDuration(1200 * soul);
        target.doCooldown();
		if (!player.isCreative()) {
			stack.shrink(1);
		}
		for (Angel statue : lvl.getEntitiesOfClass(Angel.class, target.getRenderBoundingBox().inflate(64.85D))) {
			if (Mth.nextInt(lvl.getRandom(), 0, 25) >= 24 && !statue.isCursed()) {
				statue.getEntityData().set(Angel.CURSED, true);
				lvl.sendParticles(ParticleTypes.SOUL, (statue.getX() + 0.5), (statue.getY() + 0.5), (statue.getZ() + 0.5), 8, 0.25, 0.35, 0.25, 0);
			}
		}
		if (lvl.canSeeSky(pos)) {
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(lvl);
			bolt.moveTo(Vec3.atBottomCenterOf(pos));
			bolt.setVisualOnly(true);
			lvl.addFreshEntity(bolt);
		}
	}
	private static void summonMob(ServerLevel lvl, BlockPos pos, ItemStack stack) {
		Entity entity = EntityType.loadEntityRecursive(SupernaturalManager.getSoulTag(stack), lvl, o -> o);
		if (entity != null) {
			entity.moveTo(Vec3.atBottomCenterOf(pos));
			if (entity instanceof Villager bob) {
				ZombieVillager zomby = bob.convertTo(EntityType.ZOMBIE_VILLAGER, false);
				zomby.setVillagerData(bob.getVillagerData());
				zomby.setGossips(bob.getGossips().store(NbtOps.INSTANCE));
				zomby.setTradeOffers(bob.getOffers().createTag());
				zomby.setVillagerXp(bob.getVillagerXp());
			}
			lvl.addFreshEntity(entity);
		}
	}

	@Nullable
	private static Goat getGoat(ServerLevel lvl, AABB box) {
		for (Goat target : lvl.getEntitiesOfClass(Goat.class, box)) {
			return target;
		}
		return null;
	}

	@Nullable
	private static Mob getSacrifice(ServerLevel lvl, ItemStack stack, AABB box) {
		for (Mob target : lvl.getEntitiesOfClass(Mob.class, box)) {
			if (SupernaturalManager.getSoulLevel(SupernaturalManager.getSoulLevel(target)) >= SupernaturalManager.getSoulLevel(SupernaturalManager.getSoulgem(stack)) && !target.getType().is(SupernaturalTags.IMMUNITY)) {
				return target;
			}
		}
		return null;
	}
}
