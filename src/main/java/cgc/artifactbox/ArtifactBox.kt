package cgc.artifactbox

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class ArtifactBox : JavaPlugin() {
    override fun onEnable() {
        EquipmentSetting.loadConfig()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return super.onCommand(sender, command, label, args)
    }

    companion object {
        lateinit var Plugin: ArtifactBox
    }
}