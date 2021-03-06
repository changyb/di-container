package org.cyb.di;

import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, (Provider<T>) () -> instance);
    }

    public <T, U extends T>
    void bind(Class<T> type, Class<U> implementation) {
        providers.put(type, (Provider<T>) () -> {
            try {
                return (T) ((Class<?>) implementation).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T> T get(Class<T> type) {
        return (T)providers.get(type).get();
    }

}
