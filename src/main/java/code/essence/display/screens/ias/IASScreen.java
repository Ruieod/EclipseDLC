package code.essence.display.screens.ias;

import code.essence.Essence;
import code.essence.mixins.IMinecraftClient;
import code.essence.utils.client.managers.file.impl.account.Account;
import code.essence.utils.client.managers.file.impl.account.AccountRepository;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.theme.ThemeManager;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.display.scissor.ScissorAssist;
import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class IASScreen extends Screen implements QuickImports {
    public static IASScreen INSTANCE = new IASScreen();
    
    private AccountRepository accountRepository;
    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private int selectedIndex = -1;
    private static final int ITEM_HEIGHT = 25;
    private static final int PADDING = 10;
    private static final int SCROLLBAR_WIDTH = 5;

    private boolean typing = false;
    private String typedText = "";
    private int cursorPos = 0;
    private int selStart = -1;
    private int selEnd = -1;
    private float textXOffset = 0;
    private boolean editingAccount = false;
    private int editingIndex = -1;
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 16;

    public IASScreen() {
        super(Text.of("Account Switcher"));
        this.accountRepository = Essence.getInstance().getAccountRepository();
    }

    @Override
    protected void init() {
        super.init();

        if (accountRepository == null) {
            accountRepository = Essence.getInstance().getAccountRepository();
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Account"), button -> {
            if (!typing) {
                typing = true;
                editingAccount = false;
                editingIndex = -1;
                typedText = "";
                cursorPos = 0;
                clearSelection();
            } else {
                addNewAccount();
            }
        }).dimensions(this.width / 2 - 200, this.height - 30, 95, 20).build());
        

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), button -> {
            if (selectedIndex >= 0 && selectedIndex < accountRepository.accountList.size()) {
                startEditing(selectedIndex);
            }
        }).dimensions(this.width / 2 - 100, this.height - 30, 95, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), button -> {
            if (selectedIndex >= 0 && selectedIndex < accountRepository.accountList.size()) {
                accountRepository.accountList.remove(selectedIndex);
                selectedIndex = -1;
                saveAccounts();
            }
        }).dimensions(this.width / 2, this.height - 30, 95, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> {
            this.close();
        }).dimensions(this.width / 2 + 100, this.height - 30, 95, 20).build());
    }

    private void addNewAccount() {
        if (typedText == null || typedText.trim().isEmpty()) {
            typing = true;
            editingAccount = false;
            editingIndex = -1;
            return;
        }
        
        String username = typedText.trim();
        if (username.length() >= MIN_LENGTH && username.length() <= MAX_LENGTH) {
            if (!accountRepository.accountList.stream().anyMatch(account -> account.name.equals(username))) {
                String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
                accountRepository.accountList.add(new Account(username, false, false, null, offlineUuid, "0"));
                accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                typedText = "";
                cursorPos = 0;
                typing = false;
                clearSelection();
                saveAccounts();
            }
        }
    }
    
    private void startEditing(int index) {
        editingAccount = true;
        editingIndex = index;
        Account account = accountRepository.accountList.get(index);
        typedText = account.name;
        cursorPos = typedText.length();
        selStart = -1;
        selEnd = -1;
    }
    
    private void finishEditing() {
        if (editingAccount && editingIndex >= 0 && editingIndex < accountRepository.accountList.size()) {
            if (typedText.length() >= MIN_LENGTH && typedText.length() <= MAX_LENGTH) {
                String newName = typedText.trim();
                Account account = accountRepository.accountList.get(editingIndex);
                if (!newName.equals(account.name) && 
                    !accountRepository.accountList.stream().anyMatch(a -> a.name.equals(newName) && a != account)) {
                    account.name = newName;
                    account.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + newName).getBytes(StandardCharsets.UTF_8)).toString();
                    saveAccounts();
                }
            }
            editingAccount = false;
            editingIndex = -1;
            typedText = "";
            cursorPos = 0;
            clearSelection();
        }
    }
    
    private void clearSelection() {
        selStart = -1;
        selEnd = -1;
    }
    
    private boolean hasSelection() {
        return selStart >= 0 && selEnd >= 0 && selStart != selEnd;
    }
    
    private void saveAccounts() {
        try {
            Essence.getInstance().getFileController().saveFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        if (accountRepository == null) {
            accountRepository = Essence.getInstance().getAccountRepository();
        }
        
        if (accountRepository == null || accountRepository.accountList == null) {
            super.render(context, mouseX, mouseY, delta);
            return;
        }
        
        int x = this.width / 2 - 200;
        int y = this.height / 2 - 150;
        int width = 400;
        int height = 300;

        MatrixStack matrix = context.getMatrices();
        

        rectangle.render(ShapeProperties.create(matrix, x, y, width, height)
                .round(8)
                .softness(2)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ThemeManager.BackgroundGui.getColor())
                .build());

        Fonts.getSize(18, Fonts.Type.SuisseIntlMedium).drawString(matrix, "Account Switcher", 
                x + PADDING, y + PADDING, ThemeManager.textColor.getColor());

        float textFieldX = x + PADDING;
        float textFieldY = y + height - 50;
        float textFieldWidth = width - PADDING * 2;
        float textFieldHeight = 20;
        
        rectangle.render(ShapeProperties.create(matrix, textFieldX, textFieldY, textFieldWidth, textFieldHeight)
                .round(5)
                .thickness(2)
                .outlineColor(new Color(100, 100, 100, 95).getRGB())
                .color(ThemeManager.BackgroundSettings.getColor())
                .build());
        
        Fonts.Type fontType = Fonts.Type.DEFAULT;
        var font = Fonts.getSize(16, fontType);
        long currentTime = System.currentTimeMillis();
        boolean blink = (currentTime % 1000 < 500);
        
        context.enableScissor((int) (textFieldX + 3), (int) textFieldY, (int) (textFieldX + textFieldWidth - 3), (int) (textFieldY + textFieldHeight) + 5);
        
        if (typing && hasSelection()) {
            int start = Math.min(selStart, selEnd);
            int end = Math.max(selStart, selEnd);
            float selXStart = textFieldX + 5 - textXOffset + font.getStringWidth(typedText.substring(0, start));
            float selWidth = font.getStringWidth(typedText.substring(start, end));
            rectangle.render(ShapeProperties.create(matrix, selXStart, textFieldY + 13.5f, selWidth, textFieldHeight - 10).color(0xFF5585E8).build());
        }
        
        if (!typedText.isEmpty() || typing) {
            font.drawString(matrix, typedText, textFieldX + 5 - textXOffset, textFieldY + 16, ThemeManager.textColor.getColor());
        } else {
            String placeholder = editingAccount ? "Editing account..." : "Type nickname to add account";
            font.drawString(matrix, placeholder, textFieldX + 5, textFieldY + 16, new Color(150, 150, 150, 255).getRGB());
        }
        
        if (typing && blink && !hasSelection()) {
            float cursorX = textFieldX + 5 - textXOffset + font.getStringWidth(typedText.substring(0, cursorPos));
            rectangle.render(ShapeProperties.create(matrix, cursorX + 1, textFieldY + 15f, 0.5f, textFieldHeight - 13).color(ThemeManager.textColor.getColor()).build());
        }
        
        context.disableScissor();

        if (accountRepository.accountList.isEmpty()) {
            Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).drawString(matrix, "No accounts found. Type nickname above to add.", 
                    x + PADDING, y + 60, new Color(150, 150, 150, 255).getRGB());
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        float listX = x + PADDING;
        float listY = y + 35;
        float listWidth = width - PADDING * 2 - SCROLLBAR_WIDTH - 5;
        float listHeight = height - 100;
        ScissorAssist scissorManager = Essence.getInstance().getScissorManager();
        scissorManager.push(matrix.peek().getPositionMatrix(), listX, listY, listWidth, listHeight);

        smoothedScroll = MathHelper.lerp(0.1f, smoothedScroll, scroll);
        int startIndex = Math.max(0, (int) (smoothedScroll / ITEM_HEIGHT));
        int endIndex = Math.min(accountRepository.accountList.size(), startIndex + (int) (listHeight / ITEM_HEIGHT) + 1);

        for (int i = startIndex; i < endIndex; i++) {
            if (i >= accountRepository.accountList.size()) break;
            
            Account account = accountRepository.accountList.get(i);
            float itemY = listY + i * ITEM_HEIGHT - smoothedScroll;

            if (itemY + ITEM_HEIGHT < listY || itemY > listY + listHeight) {
                continue;
            }
            boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth && 
                               mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
            boolean isSelected = i == selectedIndex;
            boolean isEditing = editingAccount && editingIndex == i;
            
            int bgColor = isHovered || isSelected ? 
                new Color(ThemeManager.BackgroundSettings.getColor()).darker().getRGB() : 
                ThemeManager.BackgroundSettings.getColor();
            
            if (isEditing) {
                bgColor = new Color(100, 150, 255, 100).getRGB();
            }
            
            rectangle.render(ShapeProperties.create(matrix, listX, itemY, listWidth, ITEM_HEIGHT)
                    .round(4)
                    .color(bgColor)
                    .build());

            if (account.starred) {
                Fonts.getSize(20, Fonts.Type.ICONS).drawString(matrix, "S", 
                        listX + listWidth - 25, itemY + ITEM_HEIGHT / 2 - 8, new Color(255, 255, 0, 255).getRGB());
            }

            String displayName = account.name;
            if (account.starred) {
                displayName = "★ " + displayName;
            }
            
            Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).drawString(matrix, displayName, 
                    listX + 5, itemY + ITEM_HEIGHT / 2 - 5, ThemeManager.textColor.getColor());
        }

        scissorManager.pop();

        int visibleItems = (int) (listHeight / ITEM_HEIGHT);
        if (accountRepository.accountList.size() > visibleItems) {
            float scrollbarX = x + width - PADDING - SCROLLBAR_WIDTH;
            float scrollbarHeight = listHeight;
            float scrollbarThumbHeight = scrollbarHeight * (visibleItems / (float) accountRepository.accountList.size());
            float scrollbarThumbY = listY + (scrollbarHeight - scrollbarThumbHeight) * (smoothedScroll / 
                    Math.max(1, (accountRepository.accountList.size() - visibleItems) * ITEM_HEIGHT));

            rectangle.render(ShapeProperties.create(matrix, scrollbarX, listY, SCROLLBAR_WIDTH, scrollbarHeight)
                    .round(2)
                    .color(new Color(50, 50, 50, 255).getRGB())
                    .build());

            rectangle.render(ShapeProperties.create(matrix, scrollbarX, scrollbarThumbY, SCROLLBAR_WIDTH, scrollbarThumbHeight)
                    .round(2)
                    .color(new Color(100, 100, 100, 255).getRGB())
                    .build());
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (accountRepository == null || accountRepository.accountList == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        float listX = this.width / 2 - 200 + PADDING;
        float listY = this.height / 2 - 150 + 35;
        float listWidth = 400 - PADDING * 2 - SCROLLBAR_WIDTH - 5;
        float listHeight = 300 - 100;
        int visibleItems = (int) (listHeight / ITEM_HEIGHT);
        int startIndex = Math.max(0, (int) (smoothedScroll / ITEM_HEIGHT));

        float textFieldX = this.width / 2 - 200 + PADDING;
        float textFieldY = this.height / 2 - 150 + 300 - 50;
        float textFieldWidth = 400 - PADDING * 2;
        float textFieldHeight = 20;
        
        if (button == 0 && mouseX >= textFieldX && mouseX <= textFieldX + textFieldWidth && 
            mouseY >= textFieldY && mouseY <= textFieldY + textFieldHeight) {
            typing = true;
            cursorPos = getCursorIndexAt((float) mouseX);
            selStart = cursorPos;
            selEnd = cursorPos;
            return true;
        } else if (button == 0) {
            typing = false;
            if (!editingAccount) {
                clearSelection();
            }
        }

        for (int i = startIndex; i < Math.min(accountRepository.accountList.size(), startIndex + visibleItems + 1); i++) {
            float itemY = listY + i * ITEM_HEIGHT - smoothedScroll;

            float starX = listX + listWidth - 25;
            float starY = itemY + ITEM_HEIGHT / 2 - 8;
            float starSize = 20;
            if (button == 0 && mouseX >= starX && mouseX <= starX + starSize && 
                mouseY >= starY && mouseY <= starY + starSize) {
                Account account = accountRepository.accountList.get(i);
                account.starred = !account.starred;
                accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                saveAccounts();
                return true;
            }
            
            if (mouseX >= listX && mouseX <= listX + listWidth && 
                mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT) {
                selectedIndex = i;
                if (button == 0) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastClickTime < 300 && lastSelectedIndex == i) {
                        switchAccount(accountRepository.accountList.get(i));
                    }
                    lastClickTime = currentTime;
                    lastSelectedIndex = i;
                } else if (button == 1) {
                    accountRepository.accountList.remove(i);
                    selectedIndex = -1;
                    saveAccounts();
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private int getCursorIndexAt(float mouseX) {
        float textFieldX = this.width / 2 - 200 + PADDING;
        var font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float textStartX = textFieldX + 5 - textXOffset;
        float relativeX = mouseX - textStartX;
        
        if (relativeX < 0) return 0;
        if (relativeX > font.getStringWidth(typedText)) return typedText.length();
        
        for (int i = 0; i <= typedText.length(); i++) {
            float charX = font.getStringWidth(typedText.substring(0, i));
            if (relativeX <= charX + font.getStringWidth(typedText.substring(i, Math.min(i + 1, typedText.length()))) / 2) {
                return i;
            }
        }
        return typedText.length();
    }

    private long lastClickTime = 0;
    private int lastSelectedIndex = -1;

    private void switchAccount(Account account) {
        Session newSession = new Session(account.name, UUID.fromString(account.uuid), "0", 
                Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
        IMinecraftClient mca = (IMinecraftClient) MinecraftClient.getInstance();
        mca.setSessionT(newSession);
        MinecraftClient.getInstance().getGameProfile().getProperties().clear();
        UserApiService apiService = UserApiService.OFFLINE;
        mca.setUserApiService(apiService);
        mca.setSocialInteractionsManagerT(new SocialInteractionsManager(MinecraftClient.getInstance(), apiService));
        mca.setProfileKeys(ProfileKeys.create(apiService, newSession, MinecraftClient.getInstance().runDirectory.toPath()));
        mca.setAbuseReportContextT(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));

        this.close();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (accountRepository == null) return false;
        
        float listY = this.height / 2 - 150 + 35;
        float listHeight = 300 - 100;
        int maxScroll = Math.max(0, accountRepository.accountList.size() * ITEM_HEIGHT - (int) listHeight);
        
        scroll = MathHelper.clamp(scroll - (float) verticalAmount * 10, 0, maxScroll);
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (editingAccount) {
                finishEditing();
                return true;
            }
            this.close();
            return true;
        }
        
        if (typing) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (editingAccount) {
                    finishEditing();
                } else {
                    addNewAccount();
                }
                return true;
            }
            
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (hasSelection()) {
                    int start = Math.min(selStart, selEnd);
                    int end = Math.max(selStart, selEnd);
                    typedText = typedText.substring(0, start) + typedText.substring(end);
                    cursorPos = start;
                    clearSelection();
                } else if (cursorPos > 0) {
                    typedText = typedText.substring(0, cursorPos - 1) + typedText.substring(cursorPos);
                    cursorPos--;
                }
                return true;
            }
            
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (hasSelection()) {
                    int start = Math.min(selStart, selEnd);
                    int end = Math.max(selStart, selEnd);
                    typedText = typedText.substring(0, start) + typedText.substring(end);
                    cursorPos = start;
                    clearSelection();
                } else if (cursorPos < typedText.length()) {
                    typedText = typedText.substring(0, cursorPos) + typedText.substring(cursorPos + 1);
                }
                return true;
            }
            
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                if (cursorPos > 0) cursorPos--;
                if ((modifiers & GLFW.GLFW_MOD_SHIFT) == 0) clearSelection();
                return true;
            }
            
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                if (cursorPos < typedText.length()) cursorPos++;
                if ((modifiers & GLFW.GLFW_MOD_SHIFT) == 0) clearSelection();
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typing && typedText.length() < MAX_LENGTH) {
            if (hasSelection()) {
                int start = Math.min(selStart, selEnd);
                int end = Math.max(selStart, selEnd);
                typedText = typedText.substring(0, start) + chr + typedText.substring(end);
                cursorPos = start + 1;
                clearSelection();
            } else {
                typedText = typedText.substring(0, cursorPos) + chr + typedText.substring(cursorPos);
                cursorPos++;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}
