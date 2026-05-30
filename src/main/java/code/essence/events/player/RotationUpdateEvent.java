package code.essence.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import code.essence.utils.client.managers.event.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
