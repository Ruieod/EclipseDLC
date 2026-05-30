package code.essence.features.impl.player.autoMine.region.utils;

import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.Optional;
import java.util.Stack;

/**
 * @author nikitavodolaz
 * @since 08.02.2026
 */

public class RandStack<T> extends Stack<T> {
    public @Nullable T random() {
        return get(new SecureRandom().nextInt(0, size() - 1));
    }

    public Optional<T> fromComparator(Comparator<? super T> comparable) {
        return stream().min(comparable);
    }
}
