package cgc.artifactbox.data

import org.bukkit.configuration.ConfigurationSection

enum class Type(
        val create: (ConfigurationSection) -> DisplaySlot
) {
    DECORATION(::Decoration),
    SLOT(::Slot)
}