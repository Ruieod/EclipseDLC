package code.essence.utils.features.price;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import java.util.regex.Pattern;

public class PriceParser {
    private final Pattern funTimePricePattern = Pattern.compile("\\$\\s*(\\d+(?:[,\\s]\\d{3})*(?:\\.\\d{2})?)");

    public int getPrice(ItemStack stack) {
        ComponentMap tag = stack.getComponents();
        if (tag == null) return -1;
        String componentString = tag.toString();
        String price = StringUtils.substringBetween(componentString, "literal{ $", "}[style={color=green}]");


        if (price == null || price.isEmpty()) {
            var lore = stack.get(DataComponentTypes.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
                String lastFoundPrice = null;
                for (Text line : lore.lines()) {
                    String text = line.getString();

                    java.util.regex.Matcher matcher = funTimePricePattern.matcher(text);
                    while (matcher.find()) {
                        lastFoundPrice = matcher.group(1);
                    }
                }
                if (lastFoundPrice != null && !lastFoundPrice.isEmpty()) {
                    price = lastFoundPrice;
                }
            }
        }
        
        if (price == null || price.isEmpty()) return -1;
        try {
            price = price.replaceAll("[\\s,]", "");
            return Integer.parseInt(price);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public int getSpookyTimePrice(ItemStack stack, GenericContainerScreen screen) {
        if (screen == null) {
            return -1;
        }

        String title = screen.getTitle().getString();
        if (!title.contains("Аукцион") && !title.contains("Поиск:")) {
            return -1;
        }

        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            for (Text line : lore.lines()) {
                String text = line.getString();
                if (text.contains("Цена:") || text.contains("Price:")) {
                    try {
                        int priceIndex = Math.max(text.indexOf("Цена:"), text.indexOf("Price:"));
                        if (priceIndex != -1) {
                            String priceText = text.substring(priceIndex);
                            String priceStr = priceText.replaceAll("[^0-9.]", "").trim();

                            try {
                                return (int) Double.parseDouble(priceStr);
                            } catch (NumberFormatException e) {
                                try {
                                    return Integer.parseInt(priceStr);
                                } catch (NumberFormatException e2) {
                                    return -1;
                                }
                            }
                        } else {
                            return -1;
                        }
                    } catch (Exception e) {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }
}