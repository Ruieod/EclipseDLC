package code.essence;

import code.essence.commands.manager.CommandRepository;
import code.essence.utils.client.managers.file.exception.FileProcessingException;
import code.essence.utils.client.logs.Logger;
import code.essence.utils.display.scissor.ScissorAssist;
import net.fabricmc.api.ModInitializer;
import code.essence.common.repository.box.BoxESPRepository;
import code.essence.common.repository.rct.RCTRepository;
import code.essence.common.repository.way.WayRepository;
import code.essence.common.discord.DiscordManager;
import code.essence.utils.client.managers.api.draggable.DraggableRepository;
import code.essence.utils.client.managers.file.*;
import code.essence.common.repository.macro.MacroRepository;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.features.module.ModuleProvider;
import code.essence.features.module.ModuleRepository;
import code.essence.features.module.ModuleSwitcher;
import code.essence.utils.client.sound.SoundManager;
import code.essence.display.screens.clickgui.MenuScreen;
import code.essence.utils.connection.cloud.CloudConfigWebSocketClient;
import code.essence.main.client.ClientInfo;
import code.essence.main.client.ClientInfoProvider;
import code.essence.main.listener.ListenerRepository;
import code.essence.commands.CommandDispatcher;
import code.essence.utils.features.aura.striking.StrikerConstructor;
import com.google.common.eventbus.EventBus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;

import code.essence.utils.client.managers.file.impl.account.AccountRepository;
import code.essence.utils.client.managers.file.impl.AccountFile;


@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Essence implements ModInitializer {
    @Getter
    static Essence instance;
    EventManager eventManager = new EventManager();
    EventBus eventBus = new EventBus();
    ModuleRepository moduleRepository;
    ModuleSwitcher moduleSwitcher;
    CommandRepository commandRepository;
    CommandDispatcher commandDispatcher;
    BoxESPRepository boxESPRepository = new BoxESPRepository(eventManager);
    MacroRepository macroRepository = new MacroRepository(eventManager);
    WayRepository wayRepository = new WayRepository(eventManager);
    RCTRepository RCTRepository = new RCTRepository(eventManager);
    code.essence.features.impl.misc.HolyWorldAutoJoin holyWorldAutoJoin;
    ModuleProvider moduleProvider;
    DraggableRepository draggableRepository;
    DiscordManager discordManager;
    FileRepository fileRepository;
    FileController fileController;
    ScissorAssist scissorManager = new ScissorAssist();
    ClientInfoProvider clientInfoProvider;
    ListenerRepository listenerRepository;
    StrikerConstructor attackPerpetrator = new StrikerConstructor();
    CloudConfigWebSocketClient cloudConfigClient;
    
    AccountRepository accountRepository;
    @Getter
    String nativeUsername = "t.me/qstarlab | t.me/mincedclient";
    @Getter
    String nativeUserIdentifier = "1337";
    @Getter
    String nativeUserRole = "admin";
    boolean initialized;
    boolean showIrcMessages = false;
    ScheduledExecutorService reconnectScheduler;
    boolean reconnecting = false;

    @Override
    public void onInitialize() {
        instance = this;

        
        initClientInfoProvider();
        initModules();
        initDraggable();
        initFileManager();
        initCommands();
        initListeners();
        initDiscordRPC();
        initHolyWorldAutoJoin();

        SoundManager.init();

        MenuScreen menuScreen = new MenuScreen();
        menuScreen.initialize();
        initialized = true;
    }
    

    private void initWebSocketClient() {
        try {
            
            if (cloudConfigClient != null) {
                cloudConfigClient.connect();
            }
        } catch (Exception e) {
            Logger.error("Failed to initialize WebSocket client: " + e.getMessage());
        }
    }

    private void initDraggable() {
        draggableRepository = new DraggableRepository();
        draggableRepository.setup();
    }

    private void initModules() {
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
    }

    private void initCommands() {
        commandRepository = new CommandRepository();
        commandDispatcher = new CommandDispatcher(eventManager);
    }

    private void initDiscordRPC() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return;
        }
        discordManager = new DiscordManager();
        discordManager.init();
    }

    private void initHolyWorldAutoJoin() {
        holyWorldAutoJoin = new code.essence.features.impl.misc.HolyWorldAutoJoin();
        eventManager.register(holyWorldAutoJoin);
    }

    private void initClientInfoProvider() {
        try {
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            File clientDirectory = new File(gameDir, "Essence");
            File filesDirectory = new File(clientDirectory, "files");
            File moduleFilesDirectory = new File(filesDirectory,"config");
            clientInfoProvider = new ClientInfo("Essence", nativeUsername, nativeUserRole, clientDirectory, filesDirectory, moduleFilesDirectory);
        } catch (Exception e) {
            Logger.error("Failed to initialize client info provider: " + e.getMessage());
        }
    }

    private void initFileManager() {
        if (clientInfoProvider == null) {
            Logger.error("ClientInfoProvider is null, cannot initialize file manager");
            return;
        }
        DirectoryCreator directoryCreator = new DirectoryCreator();
        directoryCreator.createDirectories(clientInfoProvider.clientDir(), clientInfoProvider.filesDir(), clientInfoProvider.configsDir());
        fileRepository = new FileRepository();
        fileRepository.setup(this);
        accountRepository = new AccountRepository();
        fileRepository.getClientFiles().add(new AccountFile(accountRepository));
        fileController = new FileController(fileRepository.getClientFiles(), clientInfoProvider.filesDir(), clientInfoProvider.configsDir());
        try {
            fileController.loadFiles();
            
            code.essence.utils.config.ConfigManager.loadDefaultConfig();
        } catch (FileProcessingException e) {
            Logger.error("Failed to load files: " + e.getMessage());
        }
    }

    private void initListeners() {
        listenerRepository = new ListenerRepository();
        listenerRepository.setup();
    }
    }
