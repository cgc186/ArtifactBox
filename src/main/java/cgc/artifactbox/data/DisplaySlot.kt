package cgc.artifactbox.data

import com.github.bryanser.brapi.ItemBuilder
import com.github.bryanser.brapi.Utils
import com.github.bryanser.brapi.util.EzPlaceholderExpansion
import org.black_ixx.playerpoints.PlayerPoints
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

sealed class DisplaySlot(config: ConfigurationSection) {
    val slotIndex = config.name.toInt()
    val type = Type.valueOf(config.getString("type"))
    val display = readItem(config.getConfigurationSection("display"), HOLDER)

    companion object {
        const val HOLDER = "§r§0§d§e§r§a§c§r"
        const val LOCKED = "§r§0§l§l§r§a§c§r"

        fun isHolderItem(item: ItemStack?): Boolean {
            if (item == null || !item.hasItemMeta()) {
                return false
            }
            val im = item.itemMeta
            if (!im.hasDisplayName()) {
                return false
            }
            return im.displayName.contains(HOLDER)
        }

        fun isLockedItem(item: ItemStack?): Boolean {
            if (item == null || !item.hasItemMeta()) {
                return false
            }
            val im = item.itemMeta
            if (!im.hasDisplayName()) {
                return false
            }
            return im.displayName.contains(LOCKED)
        }

        fun readItem(config: ConfigurationSection, sp: String = ""): (Player) -> ItemStack {
            val id = Material.getMaterial(config.getInt("id"))
            val durability = config.getInt("durability", 0)
            val name = ChatColor.translateAlternateColorCodes('&', config.getString("name"))
            val lore = config.getStringList("lore").map {
                ChatColor.translateAlternateColorCodes('&', it)
            }
            val enchEffect = config.getBoolean("enchEffect", false)
            val amount = config.getInt("amount", 1)
            return fun(p): ItemStack {
                return ItemBuilder.createItem(id, durability = durability, amount = amount) {
                    name(sp + EzPlaceholderExpansion.setPlaceholder(p, name))
                    lore {
                        for (s in lore) {
                            +EzPlaceholderExpansion.setPlaceholder(p, s)
                        }
                    }
                    if (enchEffect) {
                        ench(Enchantment.DURABILITY to 1)
                    }
                }
            }
        }
    }
}

class Decoration(config: ConfigurationSection) : DisplaySlot(config) {
    val commands = config.getStringList("command")?.mapNotNull {
        val str = it.split(":", limit = 2)
        val cmd = { p: Player ->
            str[1].replace("%player%", p.name)
        }
        when (str[0].toLowerCase()) {
            "p" -> fun(p: Player) {
                Bukkit.dispatchCommand(p, cmd(p))
            }
            "op" -> fun(p) {
                val op = p.isOp
                try {
                    p.isOp = true
                    Bukkit.dispatchCommand(p, cmd(p))
                } catch (e: Throwable) {
                } finally {
                    p.isOp = op
                }
            }
            "cmd" -> fun(p) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd(p))
            }
            else -> null
        }
    } ?: listOf()
}

class Slot(config: ConfigurationSection) : DisplaySlot(config) {
    val lock = readItem(config.getConfigurationSection("lock"), LOCKED)

    val money = config.getDouble("setting.unlock.money", -1.0)
    val level = config.getInt("setting.unlock.level", -1)
    val point = config.getInt("setting.unlock.point", -1)
    val permission = config.getString("setting.unlock.permission") ?: null
    val lore = config.getStringList("setting.lock").map {
        ChatColor.translateAlternateColorCodes('&', it)
    }
    val depend = config.getIntegerList("setting.unlock.depend")

    fun checkLore(item: ItemStack?): Boolean {
        if (item == null || !item.hasItemMeta()) {
            return false
        }
        val im = item.itemMeta
        if (!im.hasLore()) {
            return false
        }
       for(str in im.lore){
           for(need in lore){
               if(str.contains(need)){
                   return true
               }
           }
       }
        return false
    }

    fun isLocked(ctx: Page.PageViewContext, p: Player, info: PageInfo.SlotInfo): Boolean {
        if(!p.hasPermission("sxpage.${ctx.pageThis.name}.use")){
            if(!p.hasPermission("sxpage.${ctx.pageThis.name}.${slotIndex}")){
                return true
            }
        }
        if(depend.isNotEmpty()){
            for(need in depend){
                val si = ctx.info.slots[need] ?: continue
                if(si.item == null){
                    return true
                }
            }
        }
        if (money > 0 || point > 0) {
            if (!info.unlock) {
                return true
            }
        }
        if (level > 0 && p.level < level) {
            return true
        }
        permission?.also {
            if (!p.hasPermission(it)) {
                return true
            }
        }
        return false
    }

    fun checkLevelAndPermission(p: Player): Boolean {
        if (level > 0 && p.level < level) {
            return false
        }
        permission?.also {
            if (!p.hasPermission(it)) {
                return false
            }
        }
        return true
    }

    fun unlock(p: Player): Boolean {
        if (money > 0) {
            Utils.economy?.apply {
                val has = getBalance(p)
                if (has < money) {
                    p.sendMessage("§6你的金钱不足${money}")
                    return false
                }
            }
        }
        if (point > 0) {
            val has = pointApi.look(p.name)
            if (has < point) {
                p.sendMessage("§6你的点卷不足$point")
                return false
            }
        }

        if (money > 0) {
            Utils.economy?.apply {
                withdrawPlayer(p, money)
            }
        }
        pointApi.take(p.name, point)
        return true
    }

    companion object {
        val pointApi by lazy {
            PlayerPoints.getPlugin(PlayerPoints::class.java).api
        }
    }
}