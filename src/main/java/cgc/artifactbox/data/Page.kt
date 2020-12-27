package cgc.artifactbox.data

import cgc.artifactbox.PlayerData
import com.github.bryanser.brapi.Utils
import com.github.bryanser.brapi.kview.KViewContext
import com.github.bryanser.brapi.kview.KViewHandler
import com.github.bryanser.brapi.util.EzPlaceholderExpansion
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import java.io.File

class Page(config: ConfigurationSection) {
    val name = config.getString("Setting.name")
    val title = ChatColor.translateAlternateColorCodes('&', config.getString("Setting.title"))
    val size = config.getInt("Setting.size")

    constructor(f: File) : this(YamlConfiguration.loadConfiguration(f))

    val slots = config.getConfigurationSection("Page").let {
        val array = arrayOfNulls<DisplaySlot>(size * 9)
        for (key in it.getKeys(false)) {
            val cs = it.getConfigurationSection(key)
            val type = Type.valueOf(cs.getString("type"))
            val s = type.create(cs)
            array[s.slotIndex] = s
        }
        array
    }
    val suits: List<Suit>? = config.getConfigurationSection("Suit")?.let {
        val list = mutableListOf<Suit>()
        for (key in it.getKeys(false)) {
            list.add(Suit(it.getConfigurationSection(key)))
        }
        list
    }

    inner class PageViewContext(
            p: Player
    ) : KViewContext(EzPlaceholderExpansion.setPlaceholder(p, title)) {
        val pd = PlayerData.getCache(p)!!
        val info = pd.pageInfos.getOrPut(name) {
            createPageInfo()
        }

        val pageThis = this@Page
    }

    val view = KViewHandler.createKView("Page: $name", size, {
        PageViewContext(it)
    }) {
        for ((index, ds) in slots.withIndex()) {
            if (ds == null) continue
            when (ds) {
                is Decoration -> {
                    icon(index) {
                        initDisplay {
                            ds.display(player)
                        }
                        click {
                            if (ds.commands.isNotEmpty()) {
                                for (cmd in ds.commands) {
                                    cmd(player)
                                }
                            }
                        }
                    }
                }
                is Slot -> {
                    icon(index) {
                        initDisplay {
                            val sinfo = info.slots.getOrPut(index) {
                                PageInfo.SlotInfo(index)
                            }
                            if (sinfo.item != null) {
                                return@initDisplay sinfo.item
                            }
                            if (ds.isLocked(this,player, sinfo)) {
                                return@initDisplay ds.lock(player)
                            }
                            ds.display(player)
                        }
                        click{
                            if((player.openInventory.cursor?.amount ?: 1) > 1){
                                player.sendMessage("§c一次只能放入一个物品哦")
                                return@click
                            }
                            val sinfo = info.slots.getOrPut(index) {
                                PageInfo.SlotInfo(index)
                            }
                            if(sinfo.item != null){
                                if (ds.isLocked(this,player, sinfo)) {
                                    player.sendMessage("§c该槽位已经失效 物品自动放入你的背包了")
                                    Utils.safeGiveItem(player, sinfo.item)
                                    sinfo.item = null
                                    return@click
                                }
                                val cus = player.openInventory.cursor
                                if(cus == null || cus.type == Material.AIR){
                                    player.openInventory.cursor = sinfo.item
                                    sinfo.item = null
                                    return@click
                                }
                                if(!ds.checkLore(cus)){
                                    player.sendMessage("§c这个物品不能放入这个槽位")
                                    return@click
                                }
                                player.openInventory.cursor = sinfo.item
                                sinfo.item = cus
                                return@click
                            }
                            if(ds.isLocked(this,player, sinfo)){
                                if(!ds.checkLevelAndPermission(player)){
                                    player.sendMessage("§c你没有解锁这个槽位所需的权限或等级")
                                    return@click
                                }
                                if(ds.unlock(player)){
                                    sinfo.unlock = true
                                }
                                return@click
                            }
                            val cus = player.openInventory.cursor
                            if(cus == null || cus.type == Material.AIR){
                                return@click
                            }
                            if(ds.checkLore(cus)){
                                sinfo.item = cus
                                player.openInventory.cursor = null
                            }else{
                                player.sendMessage("§6这个物品不能放入这个槽位哦")
                            }
                        }
                    }
                }
            }
        }
    }

    fun createPageInfo(): PageInfo {
        val info = PageInfo(name)
        for (slot in slots) {
            if (slot is Slot) {
                info.slots[slot.slotIndex] = PageInfo.SlotInfo(slot.slotIndex)
            }
        }
        return info
    }

    fun updatePageInfo(old: PageInfo) {
        val iter = old.slots.iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            val index = next.key
            if (slots[index] !is Slot) {
                iter.remove()
            }
        }
        for (slot in slots) {
            if (slot is Slot) {
                if (!old.slots.containsKey(slot.slotIndex)) {
                    old.slots[slot.slotIndex] = PageInfo.SlotInfo(slot.slotIndex)
                }
            }
        }
    }
}