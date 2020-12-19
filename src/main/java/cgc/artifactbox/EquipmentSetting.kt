package cgc.artifactbox

import cgc.artifactbox.data.equip.EquipPage
import cgc.artifactbox.data.equip.Slot
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object EquipmentSetting {

    val equipPageList = mutableListOf<EquipPage>()

    fun loadConfig() {
        val f = File(ArtifactBox.Plugin.dataFolder, "equipConfig.yml")
        if (!f.exists()) {
            ArtifactBox.Plugin.saveResource("equipConfig.yml", false)
        }
        val config = YamlConfiguration.loadConfiguration(f)

        config.getConfigurationSection("EquipmentPage")?.also {
            for (key in it.getKeys(false)) {
                val equipPage = EquipPage()
                it.getConfigurationSection(key)?.also { sub ->
                    // 设置部分
                    equipPageSetting(equipPage, sub)

                    equipPageSlot(equipPage, sub)
                }
                equipPageList.add(equipPage)
            }
        }
    }



    private fun equipPageSetting(equipPage: EquipPage, sub: ConfigurationSection) {
        sub.getConfigurationSection("Setting")?.also { setting ->
            equipPage.name = setting.getString("name").toString()
            setting.getConfigurationSection("condition")?.also { condition ->
                equipPage.jurisdiction = condition.getString("jurisdiction").toString()
                equipPage.takeEffect = condition.getBoolean("takeEffect")
            }
            setting.getConfigurationSection("suitEffect")?.also { suitEffect ->
                equipPage.suitStatus = suitEffect.getBoolean("status")
                equipPage.suitLocation = suitEffect.getString("location")?.split(",") as MutableList<String>
                suitEffect.getConfigurationSection("effect")?.also { effectList ->
                    for (t in effectList.getKeys(false)) {
                        effectList.getConfigurationSection(t)?.also { effect ->
                            equipPage.suitEffectList.add(
                                EquipPage.SuitEffect(
                                    effect.getInt("value"),
                                    effect.getString("attribute").toString()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun equipPageSlot(equipPage: EquipPage, sub: ConfigurationSection) {
        sub.getConfigurationSection("Slot")?.also { slotList ->
            for (t in slotList.getKeys(false)) {
                val slotData = Slot()
                slotList.getConfigurationSection(t)?.also { slot ->
                    slotData.position = slot.getInt("position")
                    slot.getConfigurationSection("condition")?.also { condition ->
                        slotData.condition = Slot.Condition(
                            condition.getString("type").toString(),
                            condition.getInt("value"),
                            condition.getString("").toString()
                        )
                    }
                    slot.getConfigurationSection("preRestriction")?.also { preRestriction ->
                        preRestriction.getConfigurationSection("frontLock")?.also { frontLock ->
                            slotData.frontLock = Slot.FrontLock(
                                frontLock.getBoolean("status"),
                                frontLock.getInt("value"),
                                frontLock.getString("lore").toString()
                            )
                        }
                        preRestriction.getConfigurationSection("frontEquipment")?.also { frontEquipment ->
                            slotData.frontEnchase = Slot.FrontEnchase(
                                frontEquipment.getBoolean("status"),
                                frontEquipment.getInt("value"),
                                frontEquipment.getString("lore").toString()
                            )
                        }
                    }
                    slot.getConfigurationSection("item")?.also { item ->
                        slotData.item = Slot.Item(
                            item.getString("type").toString(),
                            item.getString("lore").toString()
                        )
                    }
                }
                equipPage.slotList.add(slotData)
            }
        }
    }
}