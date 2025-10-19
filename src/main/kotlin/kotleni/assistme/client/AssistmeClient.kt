package kotleni.assistme.client

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import kotleni.assistme.callbacks.BreakBlockCallback
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Colors
import net.minecraft.util.math.BlockPos

sealed interface Mode {
    object None: Mode
    data class Mining(val block: Block): Mode
}

class AssistmeClient : ClientModInitializer {
    private var isEnabled = false
    private var mode: Mode = Mode.None
    private var lastAttackBlockTime: Long = 0L

    override fun onInitializeClient() {
        val client = MinecraftClient.getInstance()

        ClientCommandRegistrationCallback.EVENT.register(
            ClientCommandRegistrationCallback {
                    dispatcher: CommandDispatcher<FabricClientCommandSource?>?, registryAccess: CommandRegistryAccess? ->

                dispatcher?.register(
                    ClientCommandManager.literal("toggleassistme")
                        .executes(Command { context: CommandContext<FabricClientCommandSource?>? ->
                            isEnabled = !isEnabled
                            1
                        })
                )

        })

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if(!isEnabled) return@register
            if(mode is Mode.None) {
                tick(client)
            }

            if(mode is Mode.Mining && System.currentTimeMillis() - lastAttackBlockTime > 600) {
                mode = Mode.None
            }
        }

        AttackBlockCallback.EVENT.register { player, world, hand, pos, direction ->
            if(!isEnabled) return@register ActionResult.PASS
            val state = world.getBlockState(pos)
            if(mode !is Mode.Mining) {
                mode = Mode.Mining(state.block)
                AimHelper.smoothlyRotatePlayerToBlock(MinecraftClient.getInstance().player!!, pos)
            }
            lastAttackBlockTime = System.currentTimeMillis()
            ActionResult.PASS
        }

        BreakBlockCallback.EVENT?.register { blockPos, blockState ->
            if(!isEnabled) return@register
            val nextBlock = findNextTunnelBlock(client.player!!)

            if(nextBlock != null) {
                AimHelper.smoothlyRotatePlayerToBlock(MinecraftClient.getInstance().player!!, nextBlock)
                return@register
            }
        }

        HudRenderCallback.EVENT.register { context, renderTickCounter ->
            if(!isEnabled) return@register
            context.drawText(
                MinecraftClient.getInstance().textRenderer,
                Text.literal("mode = ${mode::class.simpleName}"),
                6, 6,
                Colors.WHITE,
                false
            )
        }
    }

    private fun tick(client: MinecraftClient) {
        if (!AimAssistConfig.isEnabled || client.player == null || client.currentScreen != null) {
            return
        }

        client.player?.let { player ->
            if (client.options.attackKey.isPressed) {
                val bestTarget = AimHelper.findBestTarget(player)

                if (bestTarget != null) {
                    AimHelper.smoothlyRotatePlayerToEntity(player, bestTarget)
                }
            }
        }
    }

    private fun findNextTunnelBlock(player: ClientPlayerEntity): BlockPos? {
        val world = player.entityWorld
        val playerPos = player.blockPos
        val facing = player.horizontalFacing

        val reach = 5

        for (distance in 1..reach) {
            val bottomPos = playerPos.offset(facing, distance)
            val topPos = bottomPos.up()

            if (!world.getBlockState(bottomPos).isAir) {
                return bottomPos
            }

            if (!world.getBlockState(topPos).isAir) {
                return topPos
            }
        }

        return null
    }
}

