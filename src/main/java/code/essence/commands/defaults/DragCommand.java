package code.essence.commands.defaults;

import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.client.logs.Logger;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Formatting;
import code.essence.utils.client.managers.api.command.Command;
import code.essence.utils.client.managers.api.command.argument.IArgConsumer;
import code.essence.utils.client.managers.api.command.exception.CommandException;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.Essence;
import code.essence.display.hud.Notifications;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.utils.client.managers.api.draggable.DraggableRepository;
import code.essence.utils.client.managers.file.FileController;
import code.essence.utils.client.managers.file.impl.ElementsFile;

import java.io.File;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DragCommand extends Command implements QuickImports {
    protected DragCommand() {
        super("drag");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        Logger.info("[DragCommand] Команда execute вызвана, label: " + label);
        Logger.info("[DragCommand] args.hasAny() = " + args.hasAny());
        
        try {
            if (!args.hasAny()) {
                Logger.info("[DragCommand] Аргументы отсутствуют");
                ChatMessage.brandmessage("Использование: .drag reset");
                return;
            }
            
            Logger.info("[DragCommand] Аргументы есть, получаю первую подкоманду...");
            String subCommand = args.getString();
            Logger.info("[DragCommand] Получена подкоманда: '" + subCommand + "'");
            
            if (subCommand.equalsIgnoreCase("reset")) {
                Logger.info("[DragCommand] Выполняется reset");
                resetAllDraggables();
                Notifications.getInstance().addList("Позиции всех элементов HUD " + Formatting.GREEN + "сброшены", 3000);
                Logger.info("[DragCommand] Reset завершен успешно");
            } else {
                Logger.warn("[DragCommand] Неизвестная подкоманда: " + subCommand);
                ChatMessage.brandmessage("Неизвестная подкоманда: " + subCommand);
                ChatMessage.brandmessage("Использование: .drag reset");
            }
        } catch (Exception e) {
            Logger.error("[DragCommand] Ошибка при выполнении команды: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void resetAllDraggables() {
        DraggableRepository repo = Essence.getInstance().getDraggableRepository();
        Logger.info("[DragCommand] Начало сброса позиций элементов. Всего элементов: " + repo.draggable().size());
        
        int resetCount = 0;
        for (AbstractDraggable draggable : repo.draggable()) {
            String name = draggable.getName();
            int oldX = draggable.getX();
            int oldY = draggable.getY();
            int newX = oldX;
            int newY = oldY;
            
            Logger.info("[DragCommand] Обработка элемента: '" + name + "' (текущая позиция: " + oldX + ", " + oldY + ")");
            
            if (name.equals("Info")) {
                newX = 0;
                newY = 0;
            } else if (name.equals("Watermark")) {
                newX = 10;
                newY = 10;
            } else if (name.equals("Target Hud")) {
                newX = 10;
                newY = 80;
            } else if (name.equals("Keybinds")) {
                newX = 300;
                newY = 40;
            } else if (name.equals("Armor")) {
                newX = 10;
                newY = 120;
            } else if (name.equals("Cooldowns")) {
                newX = 10;
                newY = 40;
            } else if (name.equals("Staff list")) {
                newX = 115;
                newY = 40;
            } else if (name.equals("Potions")) {
                newX = 200;
                newY = 40;
            } else if (name.equals("Binds")) {
                newX = 10;
                newY = 180;
            } else if (name.equals("Notifications")) {
                newX = 0;
                newY = 350;
            } else if (name.equals("Inventory")) {
                newX = 385;
                newY = 40;
            } else {
                Logger.warn("[DragCommand] Элемент '" + name + "' не найден в списке для сброса");
                continue;
            }
            
            if (newX != oldX || newY != oldY) {
                draggable.setX(newX);
                draggable.setY(newY);
                resetCount++;
                Logger.info("[DragCommand] Позиция элемента '" + name + "' изменена с (" + oldX + ", " + oldY + ") на (" + newX + ", " + newY + ")");
            } else {
                Logger.info("[DragCommand] Позиция элемента '" + name + "' уже правильная (" + newX + ", " + newY + ")");
            }
        }
        
        Logger.info("[DragCommand] Сброшено позиций: " + resetCount + " из " + repo.draggable().size());
        
        try {
            FileController fileController = Essence.getInstance().getFileController();
            Logger.info("[DragCommand] Сохранение файла элементов...");
            fileController.saveFile(ElementsFile.class);
            Logger.info("[DragCommand] Файл элементов успешно сохранен");
        } catch (Exception e) {
            Logger.error("[DragCommand] Ошибка при сохранении файла элементов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny() && args.getArgs().size() == 1) {
            return Stream.of("reset");
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление позициями элементов HUD";
    }

    @Override
    public java.util.List<String> getLongDesc() {
        return java.util.Arrays.asList(
                "Управление позициями элементов HUD",
                "",
                "Использование:",
                "> drag reset - Сбрасывает позиции всех элементов HUD на дефолтные"
        );
    }
}

