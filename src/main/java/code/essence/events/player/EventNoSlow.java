package code.essence.events.player;

import code.essence.utils.client.managers.event.events.Event;
import code.essence.utils.client.managers.event.events.callables.EventCancellable;
import lombok.*;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Setter
public class EventNoSlow extends EventCancellable {
    public boolean start;
}