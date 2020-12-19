package cgc.artifactbox.data.equip

class Slot {
    //槽位所在位置
    var position = 0

    var condition: Condition? = null

    var frontLock: FrontLock? = null

    var frontEnchase: FrontEnchase? = null

    var item: Item? = null

    //槽位开启条件
    data class Condition(
        val type: String,
        val value: Int,
        val lore: String
    )

    //前置解锁限制
    data class FrontLock(
        var status: Boolean,
        var value: Int,
        val lore: String
    )

    //前置安装限制
    data class FrontEnchase(
        var status: Boolean,
        var value: Int,
        val lore: String
    )

    //装备信息
    data class Item(
        val type: String,
        val lore: String
    )
}
