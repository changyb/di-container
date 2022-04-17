package org.cyb.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class Context {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, (Provider<T>) () -> instance);
    }

    public <T, U extends T>
    void bind(Class<T> type, Class<U> implementation) {
        Constructor<U> injectConstructor = getInjectConstructor(implementation);
        providers.put(type, (Provider<T>) () -> {
            try {

                Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                        .map(p -> get(p.getType()).orElseThrow(DependencyNotFoundException::new))
                        .toArray(Object[]::new);
                return (T) (injectConstructor.newInstance(dependencies));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
        List<Constructor<?>> injectConstructors = Arrays.stream(implementation.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());

        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(providers.get(type))
                .map(provider -> (T) provider.get());
    }
}
