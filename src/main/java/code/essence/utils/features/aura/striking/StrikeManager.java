package code.essence.utils.features.aura.striking;

import code.essence.features.impl.combat.Criticals;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.client.managers.event.types.EventType;
import code.essence.features.impl.combat.Aura;
import code.essence.utils.features.aura.context.AutoRegressionContext;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.utils.RaycastAngle;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.features.aura.utils.Pressing;
import code.essence.features.impl.movement.AutoSprint;
import code.essence.events.item.UsingItemEvent;
import code.essence.events.packet.PacketEvent;
import code.essence.main.listener.impl.EventListener;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.interactions.inv.InventoryFlowManager;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.interactions.simulate.PlayerSimulation;
import code.essence.utils.interactions.simulate.Simulations;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StrikeManager implements QuickImports {
    private final StopWatch attackTimer = new StopWatch(), shieldWatch = new StopWatch(), sprintCooldown = new StopWatch();
    ;
    private final Pressing clickScheduler = new Pressing();
    private int count = 0;
    private boolean prevSprinting;

    void tick() {
    }

    void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
            clickScheduler.recalculate();
        }
    }

    void onUsingItem(UsingItemEvent e) {
        if (e.getType() == EventType.START && !shieldWatch.finished(50)) {
            e.cancel();
        }
    }

    private ClientCommandC2SPacket.Mode lastSprintCommand = null;
    private boolean pendingStartSprint = false;
    private boolean pendingStopSprint = false;
    private boolean didStopSprint = false;
    private boolean wasSprintingBeforeAttack = false;
    private boolean shouldUpdateMoveKeys = false;
    private static final long SPRINT_COOLDOWN_MS = 200;

    void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (!RaycastAngle.rayTrace(config) || !canAttack(config, 1)) return;

        String sprintMode = config.getSprintResetOverride() != null
                ? config.getSprintResetOverride()
                : Aura.getInstance().getSprintReset().get();

        if (sprintMode.equals("Легитно")) {
            preAttackEntity(config);

            // Если игрок на земле - атакуем сразу без проверки спринта
            if (mc.player.isOnGround()) {
                attackEntity(config);
            }
            // Если в воздухе - ждем сброса спринта для критов
            else if (!isSprinting()) {
                attackEntity(config);
            }

            postAttackEntity(config);
            return;
        }

        if (canAttack(config, 1)) preAttackEntity(config);

        if (sprintMode.equals("Не сбрасывать")) {
            attackEntity(config);
        }
        if (sprintMode.equals("Пакетно")) {
            boolean serverThinksSprinting = mc.player.isSprinting();

            if (serverThinksSprinting && !mc.player.isSubmergedInWater() && lastSprintCommand != ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                lastSprintCommand = ClientCommandC2SPacket.Mode.STOP_SPRINTING;
                mc.player.setSprinting(false);
            }
            attackEntity(config);

            if (serverThinksSprinting && !mc.player.isSubmergedInWater() && lastSprintCommand != ClientCommandC2SPacket.Mode.START_SPRINTING) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                lastSprintCommand = ClientCommandC2SPacket.Mode.START_SPRINTING;
                mc.player.setSprinting(true);
            }
            return;
        }

    }


    void preAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.isShouldUnPressShield() && mc.player.isUsingItem() && mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
            shieldWatch.reset();
        }
        String sprintMode = config.getSprintResetOverride() != null
                ? config.getSprintResetOverride()
                : Aura.getInstance().getSprintReset().get();
        if (sprintMode.equals("Легитно") ) {
            if (mc.player.isSprinting()) {
                AutoSprint.tickStop = 2;
                mc.options.sprintKey.setPressed(false);
                mc.player.setSprinting(false);
                return;
            }
            return;
        }
    }



    void postAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
    }

    void attackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {


        

        attack(config);
        breakShield(config);
        attackTimer.reset();
        count++;

        String getAimMode = Aura.aimMode.get();
        if (getAimMode.equals("ФанТайм")) {
            AutoRegressionContext.getInstance().updateLastAttackTime();
            AutoRegressionContext.hitContentQueue();

        }
    }

    private void breakShield(StrikerConstructor.AttackPerpetratorConfigurable config) {
        LivingEntity target = config.getTarget();
        Turns angleToPlayer = MathAngle.fromVec3d(mc.player.getBoundingBox().getCenter().subtract(target.getEyePos()));
        boolean targetOnShield = target.isUsingItem() && target.getActiveItem().getItem().equals(Items.SHIELD);
        boolean angle = Math.abs(TurnsConnection.computeAngleDifference(target.getYaw(), angleToPlayer.getYaw())) < 90;
        Slot axe = InventoryTask.getSlot(s -> s.getStack().getItem() instanceof AxeItem);

        if (config.isShouldBreakShield() && targetOnShield && axe != null && angle && InventoryFlowManager.script.isFinished()) {
            InventoryTask.swapHand(axe, Hand.MAIN_HAND, false);
            InventoryTask.closeScreen(true);
            attack(config);
            InventoryTask.swapHand(axe, Hand.MAIN_HAND, false, true);
            InventoryTask.closeScreen(true);
        }
    }

    private void attack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        
        boolean shouldNotAttackOnElytra = Aura.getInstance().getAttackSetting().isSelected("Не бить на элитре") && mc.player.isGliding();
        
        if (!shouldNotAttackOnElytra) {
            mc.interactionManager.attackEntity(mc.player, config.getTarget());
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private boolean isSprinting() {
        return EventListener.serverSprint && !mc.player.isGliding() && !mc.player.isTouchingWater();
    }

    public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        for (int i = 0; i <= ticks; i++) {
            if (canCrit(config, i)) {
                return true;
            }
        }
        return false;
    }

    public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (mc.player.isUsingItem() && !mc.player.getActiveItem().getItem().equals(Items.SHIELD) && config.isEatAndAttack()) {
            return false;
        }

        Aura aura = Aura.getInstance();
        boolean syncTPS = aura.isState() && aura.getAttackSetting().isSelected("Синхронизировать с ТПС");
        if (!clickScheduler.isCooldownComplete(false, syncTPS ? (int) aura.adjustTicks : 1)) {
            return false;
        }

        PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);
        if (config.isOnlyCritical() && !hasMovementRestrictions(simulated)) {
            return isPlayerInCriticalState(config, simulated, ticks);
        }

        return true;
    }

    private boolean hasMovementRestrictions(PlayerSimulation simulated) {
        return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
                || simulated.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-1e-3), Blocks.COBWEB)
                || simulated.isSubmergedInWater()
                || simulated.isInLava()
                || simulated.isClimbing()
                || !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
                || simulated.player.getAbilities().flying;
    }


    private boolean isPlayerInCriticalState(StrikerConstructor.AttackPerpetratorConfigurable config, PlayerSimulation simulated, int ticks) {
        if (Criticals.getInstance().isState() && PlayerInteractionHelper.isBoxInBlock(
                simulated.boundingBox.expand(-1e-3), Blocks.COBWEB) ) {


            boolean inWeb = PlayerInteractionHelper.isBoxInBlock(
                    simulated.boundingBox.expand(-1e-3), Blocks.COBWEB
            );
            return !simulated.onGround || inWeb || simulated.fallDistance > 0.001F;
        }

        String aimMode = Aura.aimMode.get();
        boolean fall = simulated.fallDistance > ((aimMode.equals("СпукиТайм")) ? Calculate.getRandom(0,0.2) : 0) &&  (!PlayerSimulation.simulateLocalPlayer(ticks + 1).onGround);
        boolean useSmartCrits = config.getSmartCritsOverride() != null
                ? config.getSmartCritsOverride()
                : Aura.getInstance().getSmartCrits().isValue();
        if (useSmartCrits && !mc.options.jumpKey.isPressed()) {
            return simulated.onGround || (!simulated.onGround && fall);
        } 
        LivingEntity target = config.getTarget();
        return !simulated.onGround && (fall && target != null && mc.player.distanceTo(target) <= Aura.attackRange.getValue() + 0.14F);

    }
}
