package code.essence.features.module.setting.implement;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import code.essence.features.module.setting.Setting;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class RadioSetting extends Setting {
    private String value;
    @Getter
    private final String[] options;

    public RadioSetting(String name, String description, String[] options) {
        super(name, description);
        this.options = options;
        this.value = options.length > 0 ? options[0] : "";
    }

    public RadioSetting(String name, String description, String[] options, String defaultValue) {
        super(name, description);
        this.options = options;
        this.value = defaultValue;
    }

    public void set(String value) {
        this.value = value;
    }

    public String get() {
        return value;
    }

    public RadioSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}
