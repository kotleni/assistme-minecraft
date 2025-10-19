package kotleni.assistme.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

fun interface BreakBlockCallback {
    fun onBreakBlock(position: BlockPos, block: BlockState)

    companion object {
        @JvmField
        val EVENT: Event<BreakBlockCallback?>? = EventFactory.createArrayBacked(BreakBlockCallback::class.java) { listeners ->
            BreakBlockCallback { pos, block ->
                for (listener in listeners) {
                    listener?.onBreakBlock(pos, block)
                }
            }
        }
    }
}