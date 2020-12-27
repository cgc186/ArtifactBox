package cgc.artifactbox.data

import com.github.bryanser.brapi.util.KConfigurationSerializable
import org.bukkit.inventory.ItemStack

class PageInfo : KConfigurationSerializable {
    lateinit var name: String
    val slots = mutableMapOf<Int, SlotInfo>()

    private var _slots: List<SlotInfo>
        get() = slots.values.toList()
        set(value) {
            slots.clear()
            for(s in value){
                slots[s.index] = s
            }
        }

    class SlotInfo() : KConfigurationSerializable {
        var index: Int = 0
        var unlock = false
        var item: ItemStack? = null

        constructor(index:Int):this(){
            this.index = index
        }

        constructor(args: Map<String, Any?>) : this() {
            args.deserializeExist()
        }
    }

    constructor(name: String) {
        this.name = name
    }

    constructor(args: Map<String, Any?>) {
        args.deserializeExist()
    }
}