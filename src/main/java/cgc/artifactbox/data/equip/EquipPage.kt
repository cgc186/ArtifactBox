package cgc.artifactbox.data.equip

class EquipPage {

    var name: String = ""

    var jurisdiction: String = ""
    var takeEffect: Boolean = false

    //套装效果
    var suitStatus: Boolean = false
    var suitLocation = mutableListOf<String>()
    val suitEffectList = mutableListOf<SuitEffect>()

    data class SuitEffect(
        var value: Int,
        var attribute: String
    )

    val slotList = mutableListOf<Slot>()
}