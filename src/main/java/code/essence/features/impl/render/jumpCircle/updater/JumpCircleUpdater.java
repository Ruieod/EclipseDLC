package code.essence.features.impl.render.jumpCircle.updater;


import code.essence.common.animation.Direction;
import code.essence.features.impl.render.jumpCircle.JumpCircleUtility;
import code.essence.features.impl.render.jumpCircle.data.JumpCircleLayer;
import code.essence.features.impl.render.jumpCircle.data.JumpCircleStorage;
import code.essence.features.impl.render.jumpCircle.receiver.JumpCircleModule;

public record JumpCircleUpdater(JumpCircleModule module) {
    public void update() {
        JumpCircleStorage storage = module.getStorage();

        for (JumpCircleLayer jumpCircleLayer : storage.getJumpCircleLayers()) {
            if (JumpCircleUtility.isExpired(jumpCircleLayer.startTime(), module.getLifeTime().getValue())) {
                jumpCircleLayer.animation().setDirection(Direction.BACKWARDS);
            }
        }

        storage.getJumpCircleLayers().removeIf(layer -> layer.animation().isFinished(Direction.BACKWARDS));
    }
}
