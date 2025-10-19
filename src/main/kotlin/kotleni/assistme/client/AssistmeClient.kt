package kotleni.assistme.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.util.hit.EntityHitResult

class AssistmeClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            tick(client)
        }
    }

    private fun tick(client: MinecraftClient) {
        if (!AimAssistConfig.isEnabled || client.player == null || client.currentScreen != null) {
            return
        }

        client.player?.let { player ->
            val bestTarget = AimHelper.findBestTarget(player)

            if (bestTarget != null) {
                AimHelper.smoothlyRotatePlayerToEntity(player, bestTarget)
            }
        }
    }
}

