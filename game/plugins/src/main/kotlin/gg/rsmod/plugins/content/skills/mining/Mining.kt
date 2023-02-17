package gg.rsmod.plugins.content.skills.mining

import gg.rsmod.game.fs.def.ItemDef
import gg.rsmod.game.fs.def.ObjectDef
import gg.rsmod.game.model.entity.DynamicObject
import gg.rsmod.game.model.entity.GameObject
import gg.rsmod.game.model.entity.Player
import gg.rsmod.game.model.queue.QueueTask
import gg.rsmod.plugins.api.EquipmentType
import gg.rsmod.plugins.api.Skills
import gg.rsmod.plugins.api.cfg.Items
import gg.rsmod.plugins.api.ext.*
import java.util.*

object Mining {
    suspend fun mineRock(it: QueueTask, obj: GameObject, rock: RockType) {
        val p = it.player

        if (!canMine(it, p, obj, rock)) {
            return
        }

        val oreName = p.world.definitions.get(ItemDef::class.java, rock.reward).name.lowercase()

        val pick = PickaxeType.values.firstOrNull {
            p.getSkills()
                .getMaxLevel(Skills.MINING) >= it.level && (p.equipment.contains(it.item) || p.inventory.contains(it.item))
        }!!

        p.filterableMessage("You swing your pick at the rock.")

        while (true) {
            if (!canMine(it, p, obj, rock)) {
                p.animate(-1)
                break
            }
            p.animate(pick.animation)
            it.wait(pick.ticksBetweenRolls)
            val level = p.getSkills().getCurrentLevel(Skills.MINING)
            if (interpolate(rock.lowChance, rock.highChance, level) > RANDOM.nextInt(255)) {
                p.filterableMessage("You manage to get some $oreName")

                var chanceOfGem = p.world.random(256)
                if (p.hasEquipped(EquipmentType.AMULET, Items.AMULET_OF_GLORY_1, Items.AMULET_OF_GLORY_2, Items.AMULET_OF_GLORY_3, Items.AMULET_OF_GLORY_4, Items.AMULET_OF_GLORY_T, Items.AMULET_OF_GLORY_T1, Items.AMULET_OF_GLORY_T2, Items.AMULET_OF_GLORY_T3, Items.AMULET_OF_GLORY_T4, Items.AMULET_OF_GLORY_T_10719, Items.AMULET_OF_GLORY_8283)) {
                    chanceOfGem = p.world.random(86)
                }

                if(chanceOfGem == 1) {
                    p.inventory.add(Items.UNCUT_DIAMOND + (p.world.random(0..3) * 2))
                }

                if (p.hasEquipped(EquipmentType.CHEST, Items.VARROCK_ARMOUR_1, Items.VARROCK_ARMOUR_2, Items.VARROCK_ARMOUR_3, Items.VARROCK_ARMOUR_4)) {
                    if ((rock.varrockArmourAffected - (p.getEquipment(EquipmentType.CHEST)?.id ?: -1)) >= 0) {
                        p.inventory.add(rock.reward)
                    }
                }

                p.inventory.add(rock.reward)
                p.addXp(Skills.MINING, rock.experience)
                p.animate(-1)
                val depletedRockId = p.world.definitions.get(ObjectDef::class.java, obj.id).depleted
                if (depletedRockId != -1) {
                    val world = p.world
                    world.queue {
                        val depletedOre = DynamicObject(obj, depletedRockId)
                        world.remove(obj)
                        world.spawn(depletedOre)
                        // TODO: add support mining guild runite ore respawn timer
                        wait(rock.respawnDelay)
                        world.remove(depletedOre)
                        world.spawn(DynamicObject(obj))
                    }
                }
                break
            }
        }
    }

    private suspend fun canMine(it: QueueTask, p: Player, obj: GameObject, rock: RockType): Boolean {
        if (!p.world.isSpawned(obj)) {
            return false
        }
        val pick = PickaxeType.values.firstOrNull {
            p.getSkills()
                .getMaxLevel(Skills.MINING) >= it.level && (p.equipment.contains(it.item) || p.inventory.contains(it.item))
        }
        if (pick == null) {
            it.messageBox("You need a pickaxe to mine this rock. You do not have a pickaxe<br><br>which you have the Mining level to use.")
            return false
        }
        if (p.getSkills().getMaxLevel(Skills.MINING) < rock.level) {
            it.messageBox("You need a Mining level of ${rock.level} to mine this rock.")
            return false
        }
        if (p.inventory.isFull) {
            it.messageBox("Your inventory is too full to hold any more ores.")
            return false
        }
        return true
    }
}