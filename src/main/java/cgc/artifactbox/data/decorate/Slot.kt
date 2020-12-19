package cgc.artifactbox.data.decorate

/**
 * 槽位
 */
class Slot {
    /**
     * 所在位置
     */
    var position: Int = 0

    /**
     * 物品
     */
    val itemType: String = ""

    val itemLore: String = ""

    val defaultItem: Item? = null

    val currItem: Item? = null

    data class Item(
        val data: Int,
        val id: Int,
        val lore: String
    )
}