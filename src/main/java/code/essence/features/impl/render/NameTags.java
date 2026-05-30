package code.essence.features.impl.render;

import code.essence.features.impl.combat.Aura;
import code.essence.features.module.setting.implement.*;
import code.essence.utils.display.render.post.KawaseBlur;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.client.Instance;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Vector4d;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.common.repository.friend.FriendUtils;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.client.packet.network.Network;
import code.essence.utils.math.projection.Projection;
import code.essence.events.player.TickEvent;
import code.essence.events.render.DrawEvent;
import code.essence.events.render.WorldLoadEvent;
import code.essence.features.impl.combat.AntiBot;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NameTags extends Module {

    public static NameTags getInstance() {
        return Instance.get(NameTags.class);
    }

    List<PlayerEntity> players = new ArrayList<>();

    public static MultiSelectSetting entityType = new MultiSelectSetting("Показывать", "Сущности, которые будут отображаться")
            .value("Игроков", "Мобов", "Животных", "Предметы", "Динамит", "Друзья").selected("Игроков", "Предметы");
    static MultiSelectSetting playerSetting = new MultiSelectSetting("Настройки", "Настройки для игроков")
            .value("Предметы", "Предметы в руках").selected("Предметы", "Предметы в руках").visible(() -> entityType.isSelected("Игроков")
                    || entityType.isSelected("Мобов") || entityType.isSelected("Животных") || entityType.isSelected("Друзья"));
    
    static BooleanSetting showEnchantments = new BooleanSetting("Зачарования", "Показывать зачарования на предметах")
            .setValue(false).visible(() -> playerSetting.isSelected("Предметы"));

    public NameTags() {
        super("NameTags", "NameTags", ModuleCategory.RENDER);
        setup(entityType, playerSetting, showEnchantments);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        players.clear();
    }
    
    @EventHandler
    public void onTick(TickEvent e) {
        players.clear();
        if (mc.world == null) return;
        
        mc.world.getPlayers().stream()
                .filter(player -> player != mc.player)
                .filter(player -> player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))
                .forEach(players::add);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (e == null || e.getDrawContext() == null) return;
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold);
        FontRenderer bigFont = Fonts.getSize(13 + 2, Fonts.Type.SuisseIntlMedium);
        RenderSystem.disableDepthTest();
        matrix.push();
        matrix.translate(0, 0, -1000);

        if (entityType.isSelected("Игроков") || entityType.isSelected("Друзья")) {
            for (PlayerEntity player : players) {
                if (player == null) continue;
                if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;
                boolean friend = FriendUtils.isFriend(player);
                if (!entityType.isSelected("Игроков") && !(entityType.isSelected("Друзья") && friend)) continue;
                Vector4d vec4d = Projection.getVector4D(player);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(player.getBoundingBox().getCenter());
                if (distance < 1) continue;
                if (Projection.cantSee(vec4d)) continue;

                if (playerSetting.isSelected("Предметы")) drawArmor(context, player, vec4d, font, matrix);
                if (playerSetting.isSelected("Предметы в руках")) {
                    drawHands(matrix, player, font, vec4d);
                }

                MutableText text = getTextPlayer(player, friend);
                if (Network.isAresMine()) {
                    RenderSystem.disableDepthTest();
                    float startX = (float) Projection.centerX(vec4d);
                    float startY = (float) (vec4d.y);
                    float width = mc.textRenderer.getWidth(text);
                    float height = mc.textRenderer.fontHeight;
                    float posX = startX - width / 2f;
                    float posY = startY - 11F;

                    Render2D.rectangleWithMask(context.getMatrices().peek().getPositionMatrix(), posX - 2f,
                            posY - 0.75f,
                            width + 4f,
                            height + 1.5f, height / 4f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
        /*            blur.render(ShapeProperties.create(matrix,
                                    posX - 2f,
                                    posY - 0.75f,
                                    width + 4f,
                                    height + 1.5f).quality(5).round(4f).softness(1)
                            .round(height / 4f)
                            .color(ColorAssist.HALF_BLACK)
                            .build());*/
                    context.drawText(mc.textRenderer, text, (int)posX, (int)posY + 1, ColorAssist.getColor(255), false);
                } else {
                    drawText(matrix, text, Projection.centerX(vec4d), vec4d.y - 2, font);
                }
            }
        }

        List<Entity> entities = PlayerInteractionHelper.streamEntities()
                .sorted(Comparator.comparing(ent -> ent instanceof ItemEntity item && item.getStack().getName().getContent().toString().equals("empty")))
                .toList();

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && !(living instanceof PlayerEntity)) {
                boolean isMob = living instanceof MobEntity && !(living instanceof AnimalEntity);
                boolean isAnimal = living instanceof AnimalEntity;

                if ((isMob && entityType.isSelected("Мобов")) || (isAnimal && entityType.isSelected("Животных"))) {
                    Vector4d vec4d = Projection.getVector4D(entity);
                    float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(entity.getBoundingBox().getCenter());
                    if (distance < 1) continue;
                    if (Projection.cantSee(vec4d)) continue;

                    if (playerSetting.isSelected("Предметы")) drawArmor(context, living, vec4d, font, matrix);
                    if (playerSetting.isSelected("Предметы в руках")) {
                        drawHands(matrix, living, font, vec4d);
                    }

                    MutableText text = getTextLiving(living);
                    drawText(matrix, text, Projection.centerX(vec4d), vec4d.y - 2, font);
                }
            } else if (entity instanceof ItemEntity item && entityType.isSelected("Предметы")) {
                Vector4d vec4d = Projection.getVector4D(entity);
                ItemStack stack = item.getStack();
                if (Projection.cantSee(vec4d)) continue;
                Text text = item.getStack().getName();
                if (stack.getCount() > 1) text = text.copy().append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                drawText(matrix, text, Projection.centerX(vec4d), vec4d.y, text.getContent().toString().equals("empty") ? bigFont : font);
            } else if (entity instanceof TntEntity tnt && entityType.isSelected("Динамит")) {
                Vector4d vec4d = Projection.getVector4D(entity);
                if (Projection.cantSee(vec4d)) continue;
                drawText(matrix, tnt.getStyledDisplayName(), Projection.centerX(vec4d), vec4d.y, font);
            }
        }

        matrix.pop();
        RenderSystem.enableDepthTest();
    }



    private void drawText(MatrixStack matrix, Text text, double startX, double startY, FontRenderer font) {
        int paddingX = 3;
        float paddingY = 0.75F;
        float height = font.getFont().getSize() / 1.5F;
        float width = font.getStringWidth(text);
        float posX = (float) (startX - width / 2);
        float posY = (float) startY - height;
        rectangle.render(ShapeProperties.create(matrix, posX - paddingX, posY - paddingY, width + paddingX * 2, height + paddingY * 2)
                .round(1f)
                .outlineColor(new Color(33, 33, 33, 0).getRGB())
                .color(new Color(27,27,30,150).getRGB())
                .build());
        font.drawText(matrix, text, posX, posY + 3);
    }

    private void drawArmor(DrawContext context, LivingEntity entity, Vector4d vec, FontRenderer font, MatrixStack matrix) {
        List<ItemStack> items = new ArrayList<>();
        entity.getEquippedItems().forEach(s -> {if (!s.isEmpty()) items.add(s);});
        
        float posX = (float) (Projection.centerX(vec) - items.size() * 8.5);
        float posY = (float) (vec.y - 13 / 1.5 - 15);
        float offset = -11;
        if (!items.isEmpty()) {
            RenderSystem.disableDepthTest();
            for (ItemStack stack : items) {
                offset += 16;
                Render2D.defaultDrawStack(context, stack, posX + offset, posY, false, Aura.aimMode.get().equals("ХолиВорлд") ? false : true, 0.65F);
                
                
                if (showEnchantments.isValue()) {
                    drawEnchantments(matrix, stack, vec, posX + offset, posY);
                }
            }
        }
    }
    
    private void drawEnchantments(MatrixStack matrix, ItemStack stack, Vector4d vec, float itemX, float itemY) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return;
        
        
        FontRenderer font = Fonts.getSize(11, Fonts.Type.SuisseIntlSemiBold);
        
        
        double textX = itemX + 6; 
        double startY = itemY - 5; 
        double lineHeight = 7; 
        double yOffset = 0;
        
        for (RegistryEntry<Enchantment> enchantEntry : enchantments.getEnchantments()) {
            int level = enchantments.getLevel(enchantEntry);
            String enchantName = enchantEntry.value().getName(enchantEntry, level).getString();
            
            
            String shortName = getShortEnchantmentName(enchantName, level);
            
            
            float textWidth = font.getStringWidth(shortName);
            float posX = (float) (textX - textWidth / 2); 
            Text enchantText = Text.literal(shortName).styled(style -> style.withColor(0xFFFFFF));
            font.drawText(matrix, enchantText, posX, (float) (startY - yOffset));
            
            yOffset += lineHeight;
        }
    }
    
    private String getShortEnchantmentName(String fullName, int level) {
        
        String lowerName = fullName.toLowerCase();
        if (lowerName.contains("защита от снарядов") || lowerName.contains("projectile protection")) {
            return "Зсн" + level; 
        }
        if (lowerName.contains("защита") && !lowerName.contains("от")) {
            return "Зщт" + level; 
        }
        
        
        String[] words = fullName.split("\\s+");
        
        String shortName;
        if (words.length > 1) {
            
            StringBuilder result = new StringBuilder();
            String vowels = "aeiouAEIOUаеёиоуыэюяАЕЁИОУЫЭЮЯ";
            
            for (String word : words) {
                if (result.length() >= 3) break;
                if (word.length() == 0) continue;
                
                
                for (char c : word.toCharArray()) {
                    if (vowels.indexOf(c) == -1 && Character.isLetter(c)) {
                        result.append(Character.toUpperCase(c));
                        break;
                    }
                }
            }
            shortName = result.toString();
        } else {
            
            String withoutSpaces = fullName.replaceAll("\\s+", "");
            StringBuilder consonants = new StringBuilder();
            String vowels = "aeiouAEIOUаеёиоуыэюяАЕЁИОУЫЭЮЯ";
            
            for (char c : withoutSpaces.toCharArray()) {
                if (vowels.indexOf(c) == -1 && Character.isLetter(c) && consonants.length() < 3) {
                    consonants.append(c);
                }
            }
            shortName = consonants.toString();
        }
        
        
        if (shortName.length() > 3) {
            shortName = shortName.substring(0, 3);
        }
        
        
        if (shortName.length() > 0) {
            shortName = shortName.substring(0, 1).toUpperCase() + 
                       (shortName.length() > 1 ? shortName.substring(1).toLowerCase() : "");
        }
        
        
        return shortName + level;
    }
    
    private void drawHands(MatrixStack matrix, LivingEntity entity, FontRenderer font, Vector4d vec) {
        double posY = vec.w;
        for (ItemStack stack : entity.getHandItems()) {
            if (stack.isEmpty()) continue;
            MutableText text = Text.empty().append(stack.getName());
            if (stack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
            posY += font.getStringHeight(text) / 2 + 3;
            drawText(matrix, text, Projection.centerX(vec), posY, font);
        }
    }


    private MutableText getTextPlayer(PlayerEntity player, boolean friend) {
        float health = PlayerInteractionHelper.getHealth(player);
        MutableText text = Text.empty();

        if (friend) text.append("[" + Formatting.GREEN + "F" + Formatting.RESET + "] ");
        if (AntiBot.getInstance().isBot(player)) text.append("[" + Formatting.DARK_RED + "BOT" + Formatting.RESET + "] ");

        text.append(convertDisplayName(player.getDisplayName()));

        
        if (health > 0) {
            text.append(Formatting.RESET + " [" + Formatting.RED + PlayerInteractionHelper.getHealthString(player) + Formatting.RESET + "]");
        }

        return text;
    }

    private MutableText getTextLiving(LivingEntity entity) {
        float health = PlayerInteractionHelper.getHealth(entity);
        MutableText text = Text.empty();
        text.append(entity.getDisplayName());
        if (health > 0) {
            text.append(Formatting.RESET + " [" + Formatting.RED + PlayerInteractionHelper.getHealthString(entity) + Formatting.RESET + "]");
        }
        return text;
    }



  
    private record PrefixInfo(String name, Formatting color) {}

    private static final Map<String, PrefixInfo> DONAT_PREFIXES = new HashMap<>();
    static {
        DONAT_PREFIXES.put("ꔀ", new PrefixInfo("PLAYER", Formatting.GRAY));
        DONAT_PREFIXES.put("ꔄ", new PrefixInfo("HERO", Formatting.BLUE));
        DONAT_PREFIXES.put("ꔈ", new PrefixInfo("TITAN", Formatting.YELLOW));
        DONAT_PREFIXES.put("ꔒ", new PrefixInfo("AVENGER", Formatting.GREEN));
        DONAT_PREFIXES.put("ꔖ", new PrefixInfo("OVERLORD", Formatting.AQUA));
        DONAT_PREFIXES.put("ꔠ", new PrefixInfo("MAGISTER", Formatting.GOLD));
        DONAT_PREFIXES.put("ꔤ", new PrefixInfo("IMPERATOR", Formatting.RED));
        DONAT_PREFIXES.put("ꔨ", new PrefixInfo("DRAGON", Formatting.LIGHT_PURPLE));
        DONAT_PREFIXES.put("ꔲ", new PrefixInfo("BULL", Formatting.DARK_PURPLE));
        DONAT_PREFIXES.put("ꕒ", new PrefixInfo("RABBIT", Formatting.WHITE));
        DONAT_PREFIXES.put("ꔶ", new PrefixInfo("TIGER", Formatting.GOLD));
        DONAT_PREFIXES.put("ꕄ", new PrefixInfo("DRACULA", Formatting.RED));
        DONAT_PREFIXES.put("ꕖ", new PrefixInfo("BUNNY", Formatting.WHITE));
        DONAT_PREFIXES.put("ꕀ", new PrefixInfo("HYDRA", Formatting.DARK_GREEN));
        DONAT_PREFIXES.put("ꕈ", new PrefixInfo("COBRA", Formatting.GREEN));
        DONAT_PREFIXES.put("ꕅ", new PrefixInfo("VAMPIRE", Formatting.RED));
        DONAT_PREFIXES.put("ꔁ", new PrefixInfo("MEDIA", Formatting.BLUE));
        DONAT_PREFIXES.put("ꔅ", new PrefixInfo("YT", Formatting.RED));
        DONAT_PREFIXES.put("ꕠ", new PrefixInfo("D.HELPER", Formatting.GREEN));
        DONAT_PREFIXES.put("ꔉ", new PrefixInfo("HELPER", Formatting.YELLOW));
        DONAT_PREFIXES.put("ꔓ", new PrefixInfo("ML.MODER", Formatting.YELLOW));
        DONAT_PREFIXES.put("ꔗ", new PrefixInfo("MODER", Formatting.YELLOW));
        DONAT_PREFIXES.put("ꔡ", new PrefixInfo("MODER+", Formatting.YELLOW));
        DONAT_PREFIXES.put("ꔥ", new PrefixInfo("ST.MODER", Formatting.YELLOW));
        DONAT_PREFIXES.put("ꔩ", new PrefixInfo("GL.MODER", Formatting.GOLD));
        DONAT_PREFIXES.put("ꔳ", new PrefixInfo("ML.ADMIN", Formatting.RED));
        DONAT_PREFIXES.put("ꔷ", new PrefixInfo("ADMIN", Formatting.DARK_RED));
    }

    private static Text convertDisplayName(Text original) {
        MutableText result = Text.empty();
        processTextComponent(original, result);
        return result;
    }

    private static void processTextComponent(Text component, MutableText result) {
        String content = "";
        if (component.getContent() instanceof net.minecraft.text.PlainTextContent.Literal literal) {
            content = literal.string();
        }

        if (!content.isEmpty()) {
            boolean foundPrefix = false;
            for (Map.Entry<String, PrefixInfo> entry : DONAT_PREFIXES.entrySet()) {
                if (content.contains(entry.getKey())) {
                    String replaced = content.replace(entry.getKey(), entry.getValue().color() + entry.getValue().name());
                    result.append(Text.literal(replaced));
                    foundPrefix = true;
                    break;
                }
            }
            if (!foundPrefix) {
                result.append(Text.literal(content).setStyle(component.getStyle()));
            }
        }

        for (Text sibling : component.getSiblings()) {
            processTextComponent(sibling, result);
        }
    }
    }















