package code.essence.display.hud;

import code.essence.common.animation.implement.Decelerate;
import code.essence.common.animation.implement.EaseOut;
import code.essence.utils.display.render.post.KawaseBlur;
import com.mojang.authlib.GameProfile;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.common.animation.Animation;
import code.essence.common.repository.staff.StaffRepository;
import code.essence.common.animation.Direction;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.client.Instance;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.features.impl.render.Hud;
import code.essence.utils.client.packet.network.Network;
import code.essence.features.impl.combat.AntiBot;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.*;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;

public class StaffList extends AbstractDraggable {
    public static StaffList getInstance() {
        return Instance.getDraggable(StaffList.class);
    }

    public final Map<PlayerListEntry, Animation> list = new HashMap<>();
    private final Set<String> notifiedPlayers = new HashSet<>();
    private final Set<String> addedViaPrefix = new HashSet<>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private long lastColorChange = 0;
    private int currentColorIndex = 0;
    private static final Map<String, String> CHAR_TO_NAME = new HashMap<>();
    private static final Map<String, Integer> PREFIX_COLORS = new HashMap<>();

    static {
        CHAR_TO_NAME.put("ꔀ", "player");
        CHAR_TO_NAME.put("ꔄ", "hero");
        CHAR_TO_NAME.put("ꔈ", "titan");
        CHAR_TO_NAME.put("ꔒ", "avenger");
        CHAR_TO_NAME.put("ꔖ", "overlord");
        CHAR_TO_NAME.put("ꔠ", "magister");
        CHAR_TO_NAME.put("ꔤ", "imperator");
        CHAR_TO_NAME.put("ꔨ", "dragon");
        CHAR_TO_NAME.put("ꔲ", "bull");
        CHAR_TO_NAME.put("ꕒ", "rabbit");
        CHAR_TO_NAME.put("ꔶ", "tiger");
        CHAR_TO_NAME.put("ꕄ", "dracula");
        CHAR_TO_NAME.put("ꕖ", "bunny");
        CHAR_TO_NAME.put("ꕀ", "hydra");
        CHAR_TO_NAME.put("ꕈ", "cobra");
        CHAR_TO_NAME.put("ꔁ", "media");
        CHAR_TO_NAME.put("ꔅ", "yt");
        CHAR_TO_NAME.put("ꕠ", "d.helper");
        CHAR_TO_NAME.put("ꔉ", "helper");
        CHAR_TO_NAME.put("ꔓ", "ml.moder");
        CHAR_TO_NAME.put("ꔗ", "moder");
        CHAR_TO_NAME.put("ꔡ", "moder+");
        CHAR_TO_NAME.put("ꔥ", "st.moder");
        CHAR_TO_NAME.put("ꔩ", "gl.moder");
        CHAR_TO_NAME.put("ꔳ", "ml.admin");
        CHAR_TO_NAME.put("ꔷ", "admin");

        PREFIX_COLORS.put("media", new Color(255, 0, 0, 255).getRGB());
        PREFIX_COLORS.put("yt", new Color(255, 0, 0, 255).getRGB());
        PREFIX_COLORS.put("d.helper", new Color(255, 255, 0, 255).getRGB());
        PREFIX_COLORS.put("helper", new Color(255, 255, 0, 255).getRGB());
        PREFIX_COLORS.put("ml.moder", new Color(0, 255, 255, 255).getRGB());
        PREFIX_COLORS.put("moder", new Color(0, 0, 255, 255).getRGB());
        PREFIX_COLORS.put("moder+", new Color(0, 0, 255, 255).getRGB());
        PREFIX_COLORS.put("st.moder", new Color(128, 0, 128, 255).getRGB());
        PREFIX_COLORS.put("gl.moder", new Color(128, 0, 128, 255).getRGB());
        PREFIX_COLORS.put("ml.admin", new Color(0, 255, 255, 255).getRGB());
        PREFIX_COLORS.put("admin", new Color(255, 0, 0, 255).getRGB());
        PREFIX_COLORS.put("Vanish", new Color(255, 0, 0, 255).getRGB());
    }

    public StaffList() {
        super("Staff list", 115, 40, 80, 23, true);
        this.scaleAnimation = new EaseOut().setValue(1).setMs(100);
    }

    private static final List<String> STAFF_KEYWORDS = Arrays.asList("админ", "модер", "moder", "admin", "helper", "хелпер", "стажер", "мл.сотрудник", "ст.сотрудник", "сотрудник");

    private void logDebug(String message) {
        System.out.println("[StaffList] " + message);
    }

    private boolean isNPC(String name) {
        if (name == null) return false;
        String lowerName = name.toLowerCase();
        if (lowerName.equals("anarchy") || lowerName.equals("tokens")) {
            return true;
        }
        return lowerName.contains("npc") 
            || lowerName.contains("[znpc]")
            || lowerName.startsWith("cit-")
            || lowerName.startsWith("§")
            || name.contains("NPC")
            || name.contains("[ZNPC]");
    }

    private boolean containsStaffKeyword(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : STAFF_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            FontRenderer essenceFont = Fonts.getSize(15, Fonts.Type.ESSENCE);
            float pX = getX() + getWidth() - essenceFont.getStringWidth("p") - 8;
            float pY = getY() + 9.5f;
            float pWidth = essenceFont.getStringWidth("p");
            float pHeight = essenceFont.getStringHeight("p");
            
            if (mouseX >= pX && mouseX <= pX + pWidth && mouseY >= pY && mouseY <= pY + pHeight) {
                java.util.List<String> selected = new java.util.ArrayList<>(Hud.getInstance().interfaceSettings.getSelected());
                selected.remove("Staff list");
                Hud.getInstance().interfaceSettings.setSelected(selected);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        if (mc.world == null || mc.player == null || mc.getNetworkHandler() == null) {
            list.clear();
            return;
        }

        Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();
        Scoreboard scoreboard = mc.world.getScoreboard();
        Set<String> addedNames = new HashSet<>();
        Set<String> addedNamesLower = new HashSet<>(); 

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastColorChange >= 1000) {
                currentColorIndex = (currentColorIndex + 1) % PREFIX_COLORS.size();
                lastColorChange = System.currentTimeMillis();
            }
            return;
        }

        for (PlayerListEntry entry : playerList) {
            String name = entry.getProfile().getName();
            String nameLower = name.toLowerCase();
            
            if (addedNames.contains(name) || addedNamesLower.contains(nameLower) || list.containsKey(entry) || 
                list.keySet().stream().anyMatch(e -> e.getProfile().getName().equalsIgnoreCase(name))) {
                continue;
            }
            boolean isNPCName = isNPC(name);
            if (isNPCName) {
                continue;
            }
            String display = entry.getDisplayName() != null ? entry.getDisplayName().getString() : name;
            boolean isNPCDisplay = isNPC(display);
            if (isNPCDisplay) {
                continue;
            }
            if (AntiBot.getInstance() != null && AntiBot.getInstance().isBot(entry.getProfile().getId())) {
                continue;
            }
            PlayerEntity playerEntity = mc.world.getPlayerByUuid(entry.getProfile().getId());
            if (playerEntity != null && AntiBot.getInstance() != null && AntiBot.getInstance().isBot(playerEntity)) {
                continue;
            }
            Team playerTeam = entry.getScoreboardTeam();
            String teamPrefix = playerTeam != null ? playerTeam.getPrefix().getString() : "";
            boolean hasStaffPrefix = containsStaffKeyword(teamPrefix);
            if (hasStaffPrefix) {
                list.put(entry, new Decelerate().setMs(150).setValue(1));
                addedNames.add(name);
                addedNamesLower.add(nameLower);
                addedViaPrefix.add(name);

                if (Hud.getInstance().notificationSettings.isSelected("Входе админа") && !notifiedPlayers.contains(name)) {
                    Notifications.getInstance().addList(Text.literal(name + " - Зашел на сервер!"), 5000);
                    notifiedPlayers.add(name);
                }
            }
        }

        for (StaffRepository.Staff staff : StaffRepository.getStaff()) {
            String staffName = staff.getName();
            String staffNameLower = staffName.toLowerCase();
            
            if (addedNames.contains(staffName) || addedNamesLower.contains(staffNameLower) || 
                list.keySet().stream().anyMatch(e -> e.getProfile().getName().equalsIgnoreCase(staffName))) {
                continue;
            }
            playerList.stream()
                    .filter(p -> p.getProfile().getName().equalsIgnoreCase(staffName))
                    .filter(p -> {
                        String name = p.getProfile().getName();
                        if (isNPC(name)) {
                            return false;
                        }
                        if (AntiBot.getInstance() != null && AntiBot.getInstance().isBot(p.getProfile().getId())) {
                            return false;
                        }
                        PlayerEntity playerEntity = mc.world.getPlayerByUuid(p.getProfile().getId());
                        boolean isBot = playerEntity != null && AntiBot.getInstance() != null && AntiBot.getInstance().isBot(playerEntity);
                        return playerEntity == null || AntiBot.getInstance() == null || !isBot;
                    })
                    .findFirst()
                    .ifPresent(entry -> {
                        list.put(entry, new Decelerate().setMs(150).setValue(1));
                        addedNames.add(staffName);
                        addedNamesLower.add(staffNameLower);
                    });
        }

        List<Team> teams = new ArrayList<>(scoreboard.getTeams());
        teams.sort(Comparator.comparing(Team::getName));
        Collection<PlayerListEntry> online = mc.getNetworkHandler().getPlayerList();

        for (Team team : teams) {
            Collection<String> members = team.getPlayerList();
            if (members.size() != 1) {
                continue;
            }
            String name = members.iterator().next();
            String nameLower = name.toLowerCase();
            
            if (!namePattern.matcher(name).matches() || addedNames.contains(name) || addedNamesLower.contains(nameLower) ||
                list.keySet().stream().anyMatch(e -> e.getProfile().getName().equalsIgnoreCase(name))) {
                continue;
            }
            boolean isNPCName = isNPC(name);
            if (isNPCName) {
                continue;
            }
            PlayerListEntry entry = online.stream()
                    .filter(e -> e.getProfile() != null && name.equals(e.getProfile().getName()))
                    .findFirst()
                    .orElse(null);
            if (entry != null) {
                if (AntiBot.getInstance() != null && AntiBot.getInstance().isBot(entry.getProfile().getId())) {
                    continue;
                }
                String display = entry.getDisplayName() != null ? entry.getDisplayName().getString() : name;
                if (isNPC(display)) {
                    continue;
                }
                PlayerEntity playerEntity = mc.world.getPlayerByUuid(entry.getProfile().getId());
                if (playerEntity != null && AntiBot.getInstance() != null && AntiBot.getInstance().isBot(playerEntity)) {
                    continue;
                }
                continue;
            }
            UUID generatedUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
            if (AntiBot.getInstance() != null && AntiBot.getInstance().isBot(generatedUUID)) {
                continue;
            }
            PlayerEntity playerEntity = mc.world.getPlayerByUuid(generatedUUID);
            if (playerEntity != null && AntiBot.getInstance() != null && AntiBot.getInstance().isBot(playerEntity)) {
                continue;
            }
            
            if (list.keySet().stream().anyMatch(e -> e.getProfile().getName().equalsIgnoreCase(name))) {
                continue;
            }
            String teamPrefix = team.getPrefix().getString();
            String prefix = CHAR_TO_NAME.entrySet().stream()
                    .filter(e -> teamPrefix.contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse("");
            MutableText displayName = Text.empty();
            if (Network.isReallyWorld()) {
                displayName.append(Text.literal(name).formatted(Formatting.GRAY))
                        .append(Text.literal(" [").formatted(Formatting.GRAY))
                        .append(Text.literal(prefix.isEmpty() ? "V" : prefix).formatted(Formatting.RESET))
                        .append(Text.literal("]").formatted(Formatting.GRAY));
            } else {
                displayName.append(Text.literal("[").formatted(Formatting.GRAY))
                        .append(Text.literal(prefix.isEmpty() ? "V" : prefix).formatted(Formatting.RESET))
                        .append(Text.literal("] ").formatted(Formatting.GRAY))
                        .append(Text.literal(name).formatted(Formatting.GRAY));
            }
            GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), name);
            PlayerListEntry fake = new PlayerListEntry(fakeProfile, mc.isInSingleplayer());
            fake.setDisplayName(displayName);
            fake.setListOrder(Integer.MIN_VALUE);
            list.put(fake, new Decelerate().setMs(150).setValue(1));
            addedNames.add(name);
            addedNamesLower.add(nameLower);

            if (Hud.getInstance().notificationSettings.isSelected("Входе админа") && !notifiedPlayers.contains(name)) {
                Notifications.getInstance().addList(Text.literal(name + " - Зашел на сервер!"), 5000);
                notifiedPlayers.add(name);
            }
        }

        list.entrySet().removeIf(entry -> {
            String name = entry.getKey().getProfile().getName();
            boolean isFromRepo = StaffRepository.isStaff(name);
            boolean isFromPrefix = addedViaPrefix.contains(name);
            boolean inPlayerList = playerList.stream().anyMatch(p -> p.getProfile().getName().equals(name));
            boolean inTeam = scoreboard.getTeams().stream().flatMap(t -> t.getPlayerList().stream()).anyMatch(name::equals);

            boolean stillHasStaffPrefix = false;
            if (isFromPrefix && inPlayerList) {
                PlayerListEntry currentEntry = playerList.stream()
                        .filter(p -> p.getProfile().getName().equals(name))
                        .findFirst().orElse(null);
                if (currentEntry != null) {
                    String display = currentEntry.getDisplayName() != null ? currentEntry.getDisplayName().getString() : name;
                    Team playerTeam = currentEntry.getScoreboardTeam();
                    stillHasStaffPrefix = containsStaffKeyword(display) || (playerTeam != null && containsStaffKeyword(playerTeam.getPrefix().getString()));
                }
            }
            
            boolean shouldRemove = false;
            if (isFromRepo) {
                if (!inPlayerList) {
                    shouldRemove = true;
                }
            } else if (isFromPrefix) {
                if (!inPlayerList || !stillHasStaffPrefix) {
                    shouldRemove = true;
                }
            } else {
                if (inPlayerList || !inTeam) {
                    shouldRemove = true;
                }
            }
            
            if (shouldRemove) {
          
                entry.getValue().setDirection(Direction.BACKWARDS);
            }
            if (entry.getValue().isFinished(Direction.BACKWARDS)) {
             
                notifiedPlayers.remove(name);
                addedViaPrefix.remove(name);
                if (!inPlayerList && Hud.getInstance().notificationSettings.isSelected("Выходе админа")) {
                    Notifications.getInstance().addList(Text.literal(name + " - Вышел с сервера!"), 5000);
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.SuisseIntlMedium);
        FontRenderer fontPlayer = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
        FontRenderer items = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);
        long activeStaff = list.entrySet().stream().filter(e -> !e.getValue().isFinished(Direction.BACKWARDS)).count();
        String staffCountText = String.valueOf(activeStaff);
        float textWidth = items.getStringWidth(staffCountText);
        float boxWidth = textWidth + 6;
        if (Hud.blur.isValue()) {
            Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), getX(), getY(), getWidth(), getHeight(), 5.5f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
        }
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(5.5f)
                .color(ThemeManager.BackgroundGui.getColor())
                .build());
        
        rectangle.render(ShapeProperties.create(matrix, getX() + 3, getY() + 3, getWidth() - 6, 15.5F)
                .round(4f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ColorAssist.getClientColor(),ColorAssist.getClientColor(), ColorAssist.getClientColor2(),ColorAssist.getClientColor2())
                .build());

        rectangle.render(ShapeProperties.create(matrix, getX() + 3, getY() + 21.5F, getWidth() - 6, getHeight() - 24.5F)
                .round(4f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ThemeManager.BackgroundSettings.getColor())
                .build());
        
        Fonts.getSize(15, Fonts.Type.ESSENCE).drawString(matrix, "u", getX() + 8f, getY() + 9.5f, ThemeManager.textColor.getColor());
        font.drawString(matrix, getName(), getX() + 18, getY() + 9.5f, ThemeManager.textColor.getColor());
        FontRenderer essenceFont = Fonts.getSize(15, Fonts.Type.ESSENCE);
        essenceFont.drawString(matrix, "p", getX() + getWidth() - essenceFont.getStringWidth("p") - 8, getY() + 9.9f, ThemeManager.textColor.getColor());
        float centerX = getX() + getWidth() / 2.0F;
        int offset = 26;
        int maxWidth = 65;
        Collection<PlayerListEntry> playerList = Objects.requireNonNull(mc.player).networkHandler.getPlayerList();
        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            String name = "Example";
            final String prefix = "Vanish";
            int textColor = ThemeManager.textColor.getColor();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int prefixColor = PREFIX_COLORS.getOrDefault(prefix, new Color(255, 0, 0, 255).getRGB());
            float prefixWidth = fontPlayer.getStringWidth(prefix);
            float prefixBoxWidth = prefixWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
                if (baseRenderer instanceof LivingEntityRenderer<?, ?, ?>) {
                    LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer = (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;
                    LivingEntityRenderState state = renderer.getAndUpdateRenderState(mc.player, tickCounter.getTickDelta(false));
                    Identifier textureLocation = renderer.getTexture(state);
                    
                    Render2D.drawTexture(context, textureLocation, getX() + 7.5f, centerY - .5f, 8, 3f, 8, 8, 64, ColorAssist.getRect(1), ColorAssist.multRed(-1, 1));
                }
                rectangle.render(ShapeProperties.create(matrix, getX() + 18F, centerY + 3.5, 2, 2)
                        .round(2 / 2f)
                        .color(new Color(87, 87, 90, 255).getRGB())
                        .build());
                fontPlayer.drawString(matrix, name, getX() + 22.5f, centerY + 2.5, colorWithAlpha);
                rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - prefixBoxWidth - 8, centerY - 1.5f, prefixBoxWidth, 10F)
                        .round(2)
                        .thickness(2)
                        .outlineColor(new Color(33, 33, 33, 255).getRGB())
                        .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                        .build());
                fontPlayer.drawString(matrix, prefix, getX() + getWidth() - prefixWidth - 11, centerY + 2, ThemeManager.textColor.getColor());
            });
            int width = (int) (fontPlayer.getStringWidth(name) + fontPlayer.getStringWidth(prefix) + 35);
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        } else {
            for (Map.Entry<PlayerListEntry, Animation> staff : list.entrySet()) {
                PlayerListEntry player = staff.getKey();
                if (player == null) {
                    continue;
                }
                String name = player.getProfile().getName();
                float centerY = getY() + offset;
                float animation = staff.getValue().getOutput().floatValue();
                boolean isVisible = playerList.stream().anyMatch(p -> p.getProfile().getName().equals(name));
                PlayerListEntry renderEntry = isVisible ?
                        playerList.stream().filter(p -> p.getProfile().getName().equals(name)).findFirst().orElse(player) :
                        player;
                String displayName = renderEntry.getDisplayName() != null ? renderEntry.getDisplayName().getString() : name;
                final String prefix = CHAR_TO_NAME.entrySet().stream()
                        .filter(e -> displayName.contains(e.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse("Vanish");
                int prefixColor = PREFIX_COLORS.getOrDefault(prefix, new Color(255, 0, 0, 255).getRGB());
                Identifier skinTexture = renderEntry.getSkinTextures().texture();
                int textColor = ThemeManager.textColor.getColor();
                int textAlpha = 255;
                int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
                float prefixWidth = fontPlayer.getStringWidth(prefix);
                float prefixBoxWidth = prefixWidth + 6;
                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
                    if (baseRenderer instanceof LivingEntityRenderer<?, ?, ?>) {
                        LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer = (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;
                        LivingEntityRenderState state = renderer.getAndUpdateRenderState(mc.player, tickCounter.getTickDelta(false));
                        Identifier textureLocation = renderer.getTexture(state);
                        
                        Render2D.drawTexture(context, textureLocation, getX() + 7.5f, centerY - .5f, 8, 3f, 8, 8, 64, ColorAssist.getRect(1), ColorAssist.multRed(-1, 1));
                    }
                    rectangle.render(ShapeProperties.create(matrix, getX() + 18F, centerY + 3.5, 2, 2)
                            .round(2 / 2f)
                            .color(new Color(87, 87, 90, 255).getRGB())
                            .build());
                    fontPlayer.drawString(matrix, name, getX() + 22.5f, centerY + 2.5, colorWithAlpha);
                    rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - prefixBoxWidth - 8, centerY - 1.5f, prefixBoxWidth, 10F)
                            .round(2)
                            .thickness(2)
                            .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                            .build());
                       fontPlayer.drawString(matrix, prefix, getX() + getWidth() - prefixWidth - 11, centerY + 2, ThemeManager.textColor.getColor());
                });
                int width = (int) (fontPlayer.getStringWidth(name) + fontPlayer.getStringWidth(prefix) + 35);
                maxWidth = (int) Math.max(width, maxWidth);
                offset += (int) (11 * animation);
            }
        }
        setWidth(maxWidth + 21);
        setHeight(offset + 3);
    }
}
