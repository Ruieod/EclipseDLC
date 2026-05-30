package code.essence.utils.display.render.util;

import java.util.Stack;

/**
 * @author nikitavodolaz
 * @since 08.02.2026
 */

public class CyclicStack<T> extends Stack<T> {
    int index = 0;

    public T next() {
        index = (index + 1) % size();
        return get(index);
    }
}
