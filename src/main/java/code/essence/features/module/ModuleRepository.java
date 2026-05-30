package code.essence.features.module;

import code.essence.features.impl.combat.*;
import code.essence.features.impl.misc.*;
import code.essence.features.impl.misc.creeperFarm.CreeperFarmModule;
import code.essence.features.impl.movement.*;
import code.essence.features.impl.player.*;
import code.essence.features.impl.player.autoMine.receivers.AutoMineModule;
import code.essence.features.impl.render.*;
import code.essence.features.impl.render.jumpCircle.receiver.JumpCircleModule;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;


import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<Module> modules = new ArrayList<>();

    public void setup() {
        register(
                new Particles(),
                new TargetStrafe(),
                new Strafe(),
                new AirStuck(),
            
                new ElytraMotion(),
                new HighJump(),
                new ShiftTap(),
                new AspectRatio(),
                new FreeLook(),
                new ClickPearl(),
                new ClickFriend(),
                new WindJump(),
                new TargetESP(),
                new NoWeb(),
                new ServerHelper(),
        new AutoMineModule(),
             
                new ItemScroller(),
                new Hud(),
                new ClickGui(),
                new AuctionHelper(),
                new Prediction(),
              
                new XRay(),
                new Aura(),
                new TriggerBot(),
                new AutoSwap(),
                new NoFriendDamage(),
                new HitBoxModule(),
                new AntiBot(),
                new AutoExplosion(),
                new AutoSprint(),
                new NoPush(),
                new ElytraHelper(),
                new ServerJoiner(),
                new NoDelay(),
                new Velocity(),
                new AutoRespawn(),
                new NoSlow(),
                new GuiMove(),
                new Blink(),
                new AutoTool(),
              
                new Fly(),
                new FastBreak(),
                new CameraSettings(),
                new Speed(),
                new SwingAnimation(),
                new ViewModel(),
                new BlockOverlay(),
                new ElytraGlide(),
                new Esp(),
                new NameTags(),
                new ShulkerView(),
                new BlockESP(),
                new AutoTotem(),
                new EnderChestPlus(),
                new FreeCam(),
                new ChestStealer(),
                new AutoTpAccept(),
                new AutoAuth(),
                new Arrows(),
                new AutoLeave(),
                new WorldTweaks(),
                new NoClip(),
                new NoRender(),
              
                new NameProtect(),
                new SeeInvisible(),
                new AutoArmor(),
                new AutoUse(),
                new AutoPotion(),
                new NoInteract(),
                new CrossHair(),
                new SuperFireWork(),
                new Spider(),
                new SafeWalk(),
                new ServerRPSpoofer(),
                new NoFall(),

                new DeathCoords(),
              
                new XCarry(),
                new AntiAFK(),
                new FakePing(),
                new LevitationControl(),
                new KillEffect(),
                new UseTracker(),
                new ShulkerBypass(),
             
                new ChinaHat(),
                new ElytraAura(),
                new Criticals(),
              
                new JumpCircleModule(),
                new AutoDodge(),
                new ClientSounds(),
                new SoundsRemove(),
                new Trails(),
                new TapeMouse(),
                new AutoDuel()


        );
    }

    
    public void register(Module... module) {
        modules.addAll(List.of(module));
    }

    public List<Module> modules() {
        return modules;
    }
}

