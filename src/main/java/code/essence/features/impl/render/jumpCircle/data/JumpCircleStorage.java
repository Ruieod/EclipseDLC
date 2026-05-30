package code.essence.features.impl.render.jumpCircle.data;


import code.essence.common.animation.implement.OutBack;
import code.essence.features.impl.render.jumpCircle.receiver.JumpCircleModule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JumpCircleStorage {
    List<JumpCircleLayer> jumpCircleLayers = new CopyOnWriteArrayList<>();
    JumpCircleModule module;

    public JumpCircleStorage(JumpCircleModule module) {
        this.module = module;
    }

    public void add(Vec3d vec3d) {
        int lifeTime = (int) module.getLifeTime().getValue();

        jumpCircleLayers.add(new JumpCircleLayer(vec3d, new OutBack()
                .setMs(lifeTime)
                .setValue(1f)));
    }

    public void add(PlayerEntity player) {
        add(player.getPos().add(0f, 0.01f, 0f));
    }

    public boolean isPresent() {
        return !jumpCircleLayers.isEmpty();
    }
}
