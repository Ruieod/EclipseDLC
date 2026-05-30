package code.essence.utils.input;

import java.util.ArrayList;
import java.util.List;

public class InputManager {
    private static final List<InputComponent> activeInputs = new ArrayList<>();
    
    public interface InputComponent {
        void setTyping(boolean typing);
        boolean isTyping();
    }
    
    public static void activateInput(InputComponent input) {
        for (InputComponent activeInput : activeInputs) {
            if (activeInput != input && activeInput.isTyping()) {
                activeInput.setTyping(false);
            }
        }

        input.setTyping(true);
        

        if (!activeInputs.contains(input)) {
            activeInputs.add(input);
        }
    }
    
    public static void deactivateInput(InputComponent input) {
        input.setTyping(false);
        activeInputs.remove(input);
    }
    
    public static void deactivateAllInputs() {
        for (InputComponent input : activeInputs) {
            input.setTyping(false);
        }
        activeInputs.clear();
    }
    
    public static void clearAllInputs() {
        for (InputComponent input : activeInputs) {
            input.setTyping(false);
            if (input instanceof ClearableInput clearable) {
                clearable.clearText();
            }
        }
        activeInputs.clear();
    }
    
    public static boolean hasActiveInputs() {
        return activeInputs.stream().anyMatch(InputComponent::isTyping);
    }
    
    public interface ClearableInput {
        void clearText();
    }
}












