package dev.nexus.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Prosta, typowana magistrala zdarzen. subscribe() zwraca Runnable do wypisania (brak wyciekow). */
public final class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    public <T> Runnable subscribe(Class<T> type, Consumer<T> listener) {
        List<Consumer<?>> list = listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        list.add(listener);
        return () -> list.remove(listener);
    }

    @SuppressWarnings("unchecked")
    public <T> T post(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Consumer<?> c : list) {
                ((Consumer<T>) c).accept(event);
            }
        }
        return event;
    }
}
