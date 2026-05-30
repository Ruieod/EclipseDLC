/*
package code.essence.features.impl.render;


import code.essence.common.animation.Animation;
import code.essence.common.animation.implement.OutBack;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;



@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TargetEspModule extends Module {
    SelectSetting drawMode = new SelectSetting("target_esp.draw_mode", "")
            .value("Crystals", "Ghosts");

    BooleanSetting crystalFill = new BooleanSetting("target_esp.crystals_fill", "")
            .visible(() -> drawMode.isSelected("Crystals"));

    Animation fadeAnimation = new OutBack().setMs(500).setValue(1f);

    @NonFinal
    LivingEntity lastLiving;

    @NonFinal
    Vec3d vec3d;

    public TargetEspModule() {
        super("Target Pointer", ModuleCategory.VISUALS);
        setup(drawMode, crystalFill);
    }

    @EventHandler
    public void worldRenderEventReceive(WorldRenderEvent renderEvent) {
        LivingEntity living = null;

        if (mc.crosshairTarget instanceof EntityHitResult entityHitResult) {
            if (entityHitResult.getEntity() instanceof LivingEntity living1) {
                living = living1;
            }
        }

        if (living == null) {
            fadeAnimation.setDirection(Direction.BACKWARDS);

            if (fadeAnimation.isFinished(Direction.BACKWARDS)) {
                lastLiving = null;
                return;
            }
        } else {
            fadeAnimation.setDirection(Direction.FORWARDS);
            lastLiving = living;
        }

        if (lastLiving.isAlive()) {
            vec3d = lastLiving.getLerpedPos(renderEvent.getPartialTicks());
        } else {
            vec3d = lastLiving.getLastRenderPos();
        }

        TargetPointers targetPointers = TargetPointers.valueOf(drawMode.getSelected());
        TargetRenderProvider provider = targetPointers.getProvider();

        if (provider != null) {
            provider.render(renderEvent.getStack(), lastLiving, vec3d, renderEvent.getPartialTicks());
        }
    }

    @Override
    public void activate() {
        fadeAnimation.setDirection(Direction.FORWARDS);
        super.activate();
    }

    @Override
    public void deactivate() {
        fadeAnimation.setDirection(Direction.BACKWARDS);
        super.deactivate();
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static enum TargetPointers {
        Crystals(new CrystalTargetProvider(Blume.getInstance().getModuleProvider().get(TargetEspModule.class))),
        Ghosts(null);

        TargetRenderProvider provider;

        TargetPointers(TargetRenderProvider provider) {
            this.provider = provider;
        }
    }
}
*/