package cgc.artifactbox

import cgc.artifactbox.data.PageInfo
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ArtifactBox : JavaPlugin() {
    override fun onLoad() {
        Plugin = this
        ConfigurationSerialization.registerClass(PageInfo.SlotInfo::class.java)
        ConfigurationSerialization.registerClass(PageInfo::class.java)
        ConfigurationSerialization.registerClass(PlayerData::class.java)
    }

    override fun onEnable() {
        val f = File(dataFolder,"config.yml")
        if(!f.exists()){
            saveDefaultConfig()
        }
        val config = YamlConfiguration.loadConfiguration(f)
        SQLManager.init(config.getConfigurationSection("Mysql"))
        Setting.loadConfig()
        Bukkit.getPluginManager().registerEvents(PlayerData.Companion, this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(args.isEmpty() || args[0].equals("help", true)){
            return !sender.isOp
        }
        if(args[0].equals("open", true) && args.size > 1 && sender is Player){
            val target = args[1]
            val page = Setting.pages[target] ?: run{
                sender.sendMessage("§c找不到装备页: ${target}")
                return true
            }
            if(!sender.hasPermission("sxpage.open.${page.name}")){
                if(!sender.hasPermission("sxpage.${page.name}.use")){
                    sender.sendMessage("§6你没有查看这个装备页的权限")
                    return true
                }
            }
            page.view.openView(sender)
            return true
        }
        if(args[0].equals("reload", true) && sender.isOp){
            Setting.loadConfig()
            sender.sendMessage("§6重载完成")
            return true
        }
        return !sender.isOp
    }

    companion object {
        lateinit var Plugin: ArtifactBox
    }
}