package code.essence.events.chat;

import code.essence.utils.client.managers.event.events.callables.EventCancellable;

public class TabCompleteEvent extends EventCancellable {
    public final String prefix;
    public String[] completions;



    public TabCompleteEvent(String prefix) {
        this.prefix = prefix;
        this.completions = null;
    }
}
