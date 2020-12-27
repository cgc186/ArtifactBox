package cgc.artifactbox.data

import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection

class Suit(config:ConfigurationSection) {
    val name = config.name
    val lore = ChatColor.translateAlternateColorCodes('&', config.getString("lore"))
    val effect = config.getConfigurationSection("effect").let{
        val map = mutableMapOf<Int,List<String>>()
        for(key in it.getKeys(false)){
            map[key.toInt()] = it.getStringList(key).map{
                ChatColor.translateAlternateColorCodes('&', it)
            }
        }
        map
    }
}