package code.essence.utils.client.managers.event.events.callables;

import code.essence.utils.client.managers.event.events.Event;
import code.essence.utils.client.managers.event.events.Typed;

public abstract class EventTyped implements Event, Typed {

    private final byte type;

    
    protected EventTyped(byte eventType) {
        type = eventType;
    }

    
    @Override
    public byte getType() {
        return type;
    }

}