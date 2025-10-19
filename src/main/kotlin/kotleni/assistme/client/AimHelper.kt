package kotleni.assistme.client

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.util.math.MathHelper
import kotlin.math.atan2
import kotlin.math.sqrt

object AimHelper {
    fun findBestTarget(player: ClientPlayerEntity): LivingEntity? {
        val world = player.entityWorld
        val searchBox = player.boundingBox.expand(AimAssistConfig.maxDistance)
        val playerLookVec = player.getRotationVec(1.0f)

        val potentialTargets = world.getOtherEntities(player, searchBox) { entity ->
            entity is LivingEntity && entity.isAlive && player.canSee(entity)
        }

        var bestTarget: LivingEntity? = null
        var minAngle = AimAssistConfig.activationFov

        // Separate targets into hostile and others for priority
        val hostileTargets = potentialTargets.filterIsInstance<HostileEntity>()
        val otherTargets = potentialTargets.filter { it !is HostileEntity }

        // Prioritize finding the best hostile target first
        for (target in hostileTargets) {
            val vectorToTarget = target.eyePos.subtract(player.eyePos).normalize()
            val angle = Math.toDegrees(Math.acos(playerLookVec.dotProduct(vectorToTarget)))

            if (angle < minAngle) {
                minAngle = angle.toFloat()
                bestTarget = target
            }
        }

        // If no hostile target was found in the FOV, check for other living entities
        if (bestTarget == null) {
            for (target in otherTargets) {
                val vectorToTarget = target.eyePos.subtract(player.eyePos).normalize()
                val angle = Math.toDegrees(Math.acos(playerLookVec.dotProduct(vectorToTarget)))

                if (angle < minAngle) {
                    minAngle = angle.toFloat()
                    bestTarget = target.entity
                }
            }
        }

        return bestTarget
    }

    fun smoothlyRotatePlayerToEntity(player: ClientPlayerEntity, target: Entity) {
        val dx = target.x - player.x
        val dy = target.eyeY - player.eyeY
        val dz = target.z - player.z
        val distXZ = sqrt(dx * dx + dz * dz)

        val targetYaw = Math.toDegrees(atan2(dz, dx)) - 90.0
        val targetPitch = -Math.toDegrees(atan2(dy, distXZ))

        // --- Smooth Interpolation ---
        // Get the shortest angle difference, handling the -180 to 180 degree wrap-around
        val yawDifference = MathHelper.wrapDegrees(targetYaw - player.yaw)
        val pitchDifference = targetPitch - player.pitch // Pitch doesn't wrap

        // Apply a fraction of the difference each tick, based on the smoothness factor
        val newYaw = player.yaw + yawDifference * AimAssistConfig.smoothnessFactor
        val newPitch = player.pitch + pitchDifference * AimAssistConfig.smoothnessFactor

        player.yaw = newYaw.toFloat()
        player.pitch = MathHelper.clamp(newPitch.toFloat(), -90.0f, 90.0f)
    }
}