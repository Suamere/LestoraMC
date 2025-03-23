package com.lestora;

import net.minecraft.client.player.LocalPlayer;

public class OrientationHelper {

    public static void snapPlayerOrientation(LocalPlayer player) {
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        // Early return if the player's current yaw and pitch are already perfectly snapped.
        if ((currentYaw == 0.0f || currentYaw == 45.0f || currentYaw == 90.0f || currentYaw == 135.0f ||
                currentYaw == 180.0f || currentYaw == 225.0f || currentYaw == 270.0f || currentYaw == 315.0f || currentYaw == 360.0f) &&
                (currentPitch == -90.0f || currentPitch == -30.0f || currentPitch == 0.0f ||
                        currentPitch == 30.0f || currentPitch == 90.0f)) {
            return;
        }

        float normalizedYaw = currentYaw % 360;
        if (normalizedYaw < 0) {
            normalizedYaw += 360;
        }

        int snappedYaw = Math.round(normalizedYaw / 45f) * 45;
        snappedYaw %= 360;

        float snappedPitch;
        if (currentPitch <= -45) {
            snappedPitch = -90;
        } else if (currentPitch < -15) {
            snappedPitch = -30;
        } else if (currentPitch <= 15) {
            snappedPitch = 0;
        } else if (currentPitch <= 45) {
            snappedPitch = 30;
        } else {
            snappedPitch = 90;
        }

        // Apply the snapped orientation to the player.
        player.setYRot(snappedYaw);
        player.setXRot(snappedPitch);
        // Also align the head rotation to match the body.
        player.yHeadRot = snappedYaw;

        if (player.getVehicle() != null) {
            player.getVehicle().setYRot(snappedYaw);
        }
    }
}