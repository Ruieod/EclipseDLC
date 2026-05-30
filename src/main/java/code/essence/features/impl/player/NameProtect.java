package code.essence.features.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.TextSetting;
import code.essence.common.repository.friend.FriendUtils;
import code.essence.events.render.TextFactoryEvent;

public class NameProtect extends Module {
    TextSetting nameSetting = new TextSetting("Имя", "").setText("Protected").setMax(16);
    BooleanSetting friendsSetting = new BooleanSetting("Скрывать друзей", "Скрывает никнеймы друзей").setValue(true);

    public NameProtect() {
        super("NameProtect","NameProtect", ModuleCategory.PLAYER);
        setup(nameSetting,friendsSetting);
    }

    @EventHandler
    public void onTextFactory(TextFactoryEvent e) {
        e.replaceText(mc.getSession().getUsername(), nameSetting.getText());
        if (friendsSetting.isValue()) FriendUtils.getFriends().forEach(friend -> e.replaceText(friend.getName(), nameSetting.getText()));
    }
}
