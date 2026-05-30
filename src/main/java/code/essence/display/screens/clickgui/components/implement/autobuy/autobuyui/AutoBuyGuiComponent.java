package code.essence.display.screens.clickgui.components.implement.autobuy.autobuyui;

import code.essence.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import code.essence.display.screens.clickgui.components.implement.autobuy.manager.AutoBuyManager;
import code.essence.display.screens.clickgui.components.implement.autobuy.originalitems.ItemRegistry;
import code.essence.display.screens.clickgui.components.implement.other.StatusRender;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.display.screens.clickgui.MenuScreen;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.Essence;
import code.essence.utils.display.scissor.ScissorAssist;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Setter
@Accessors(chain = true)
public class AutoBuyGuiComponent extends AbstractComponent {
    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private final List<StatusRender> itemStatusRenders = new ArrayList<>();
    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();

    public AutoBuyGuiComponent() {
        updateItemStatusRenders();
    }

    private void updateItemStatusRenders() {
        itemStatusRenders.clear();
        List<AutoBuyableItem> allItems = ItemRegistry.getAllItems();
        for (AutoBuyableItem item : allItems) {
            StatusRender itemStatus = new StatusRender();
            itemStatus.setState(item.isEnabled())
                    .setRunnable(() -> {
                        autoBuyManager.toggleItem(item);
                        updateItemStatusRenders();
                    });
            itemStatusRenders.add(itemStatus);
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (MenuScreen.INSTANCE.getCategory() != ModuleCategory.CUSTOMIZATION) {
            return;
        }
        MatrixStack matrix = context.getMatrices();
        renderAllItems(context, matrix, mouseX, mouseY, delta);
    }

    private void renderAllItems(DrawContext context, MatrixStack matrix, int mouseX, int mouseY, float delta) {
        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
        ScissorAssist scissorManager = Essence.getInstance().getScissorManager();

        float listX = x + 55f;
        float listY = y + 25f;
        float listWidth = width - 43f - 15f;
        float listHeight = height - 48f;

        float contentHeight = calculateContentHeight();
        float maxScrollAmount = Math.max(0f, contentHeight - listHeight - 25f);
        scroll = MathHelper.clamp(scroll, -maxScrollAmount, 0f);
        smoothedScroll = Calculate.interpolate(smoothedScroll, scroll, 0.15f);

        scissorManager.push(positionMatrix, listX, listY + 4, listWidth, listHeight + 18);

        float itemY = listY + 10f + smoothedScroll;
        int globalIndex = 0;

        List<AutoBuyableItem> krushItems = ItemRegistry.getKrush();
        if (!krushItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Крушитель");
            itemY += 20f;

            for (int i = 0; i < krushItems.size(); i++) {
                AutoBuyableItem item = krushItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;

                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += (krushItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> talismanItems = ItemRegistry.getTalismans();
        if (!talismanItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Талисманы");
            itemY += 20f;

            for (int i = 0; i < talismanItems.size(); i++) {
                AutoBuyableItem item = talismanItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;

                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += (talismanItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> sphereItems = ItemRegistry.getSpheres();
        if (!sphereItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Сферы");
            itemY += 20f;

            for (int i = 0; i < sphereItems.size(); i++) {
                AutoBuyableItem item = sphereItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;

                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += (sphereItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> miscItems = ItemRegistry.getMisc();
        if (!miscItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Разное");
            itemY += 20f;

            for (int i = 0; i < miscItems.size(); i++) {
                AutoBuyableItem item = miscItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;

                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += (miscItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> donatorItems = ItemRegistry.getDonator();
        if (!donatorItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Донаторские");
            itemY += 20f;

            for (int i = 0; i < donatorItems.size(); i++) {
                AutoBuyableItem item = donatorItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;

                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += (donatorItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> potionItems = ItemRegistry.getPotions();
        if (!potionItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Зелья");
            itemY += 20f;

            for (int i = 0; i < potionItems.size(); i++) {
                AutoBuyableItem item = potionItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;

                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += (potionItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        scissorManager.pop();
    }

    private float calculateContentHeight() {
        List<AutoBuyableItem> krushItems = ItemRegistry.getKrush();
        List<AutoBuyableItem> talismanItems = ItemRegistry.getTalismans();
        List<AutoBuyableItem> sphereItems = ItemRegistry.getSpheres();
        List<AutoBuyableItem> miscItems = ItemRegistry.getMisc();
        List<AutoBuyableItem> donatorItems = ItemRegistry.getDonator();
        List<AutoBuyableItem> potionItems = ItemRegistry.getPotions();

        float totalHeight = 10f;
        if (!krushItems.isEmpty()) {
            totalHeight += 20f + ((krushItems.size() + 1) / 2) * 35f + 10f;
        }
        if (!talismanItems.isEmpty()) {
            totalHeight += 20f + ((talismanItems.size() + 1) / 2) * 35f + 10f;
        }
        if (!sphereItems.isEmpty()) {
            totalHeight += 20f + ((sphereItems.size() + 1) / 2) * 35f + 10f;
        }
        if (!miscItems.isEmpty()) {
            totalHeight += 20f + ((miscItems.size() + 1) / 2) * 35f + 10f;
        }
        if (!donatorItems.isEmpty()) {
            totalHeight += 20f + ((donatorItems.size() + 1) / 2) * 35f + 10f;
        }
        if (!potionItems.isEmpty()) {
            totalHeight += 20f + ((potionItems.size() + 1) / 2) * 35f + 10f;
        }
        return totalHeight;
    }

    private void renderCategoryHeader(MatrixStack matrix, float x, float y, float width, String categoryName) {
        float textWidth = Fonts.getSize(14, Fonts.Type.SuisseIntlSemiBold).getStringWidth(categoryName);
        float lineWidth = (width - textWidth - 20f) / 2f;

        rectangle.render(ShapeProperties.create(matrix, x, y + 6, lineWidth - 10, 1)
                .color(new Color(54, 54, 56, 255).getRGB())
                .build());

        Fonts.getSize(14, Fonts.Type.SuisseIntlSemiBold).drawString(matrix, categoryName, x + lineWidth, y + 4, ColorAssist.getText(1f));

        rectangle.render(ShapeProperties.create(matrix, x + lineWidth + textWidth + 10f, y + 6, lineWidth - 6, 1)
                .color(new Color(54, 54, 56, 255).getRGB())
                .build());
    }

    private void renderItem(DrawContext context, MatrixStack matrix, AutoBuyableItem item, float itemX, float itemY, int mouseX, int mouseY, float delta, int index) {
        rectangle.render(ShapeProperties.create(matrix, itemX, itemY, 175, 30)
                .round(6)
                .thickness(2)
                .softness(1)
                .outlineColor(new Color(54, 54, 56, 255).getRGB())
                .color(new Color(31, 27, 35, 0).getRGB())
                .build());

        ItemStack itemStack = item.createItemStack();
        Render2D.defaultDrawStack(context, itemStack, itemX + 6, itemY + 6.5f, false, false, 1.0f);

        Fonts.getSize(17, Fonts.Type.SuisseIntlSemiBold).drawString(matrix, item.getDisplayName(), itemX + 30, itemY + 8, ColorAssist.getText(1f));
        Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold).drawString(matrix, "Цена: " + item.getPrice() + "$", itemX + 30, itemY + 19, ColorAssist.getText(0.8f));

        if (index < itemStatusRenders.size()) {
            itemStatusRenders.get(index).position(itemX + 160, itemY + 11.5f).render(context, mouseX, mouseY, delta);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MenuScreen.INSTANCE.getCategory() != ModuleCategory.CUSTOMIZATION || button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        smoothedScroll = Calculate.interpolate(smoothedScroll, scroll, 0.15f);
        float itemY = y + 25f + 10f + smoothedScroll;
        int globalIndex = 0;

        List<AutoBuyableItem> krushItems = ItemRegistry.getKrush();
        if (!krushItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < krushItems.size(); i++) {
                float itemX = x + 55 + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;
                if (globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 11.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(krushItems.get(i));
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += (krushItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> talismanItems = ItemRegistry.getTalismans();
        if (!talismanItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < talismanItems.size(); i++) {
                float itemX = x + 55 + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;
                if (globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 11.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(talismanItems.get(i));
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += (talismanItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> sphereItems = ItemRegistry.getSpheres();
        if (!sphereItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < sphereItems.size(); i++) {
                float itemX = x + 55 + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;
                if (globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 11.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(sphereItems.get(i));
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += (sphereItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> miscItems = ItemRegistry.getMisc();
        if (!miscItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < miscItems.size(); i++) {
                float itemX = x + 55 + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;
                if (globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 11.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(miscItems.get(i));
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += (miscItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> donatorItems = ItemRegistry.getDonator();
        if (!donatorItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < donatorItems.size(); i++) {
                float itemX = x + 55 + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;
                if (globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 11.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(donatorItems.get(i));
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += (donatorItems.size() % 2 != 0) ? 35 : 35;
            itemY += 10f;
        }

        List<AutoBuyableItem> potionItems = ItemRegistry.getPotions();
        if (!potionItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < potionItems.size(); i++) {
                float itemX = x + 55 + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 35;
                if (globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 11.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(potionItems.get(i));
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (MenuScreen.INSTANCE.getCategory() == ModuleCategory.CUSTOMIZATION &&
                Calculate.isHovered(mouseX, mouseY, x + 55, y + 38, width - 43 - 15, height - 48)) {
            scroll += amount * 20;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}