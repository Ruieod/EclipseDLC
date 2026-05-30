package code.essence.utils.client.managers.event.events.callables;

import lombok.Setter;
import code.essence.utils.client.managers.event.events.Cancellable;
import code.essence.utils.client.managers.event.events.Event;

public abstract class EventCancellable implements Event, Cancellable {

    @Setter
    private boolean cancelled;

    protected EventCancellable() {
    }

    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    

    @Override
    public void cancel() {
        cancelled = true;
    }
}