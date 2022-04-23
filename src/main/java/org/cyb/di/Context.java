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
        providers.put(type, new ConstructorInjectProvider<U>(type, injectConstructor));
    }

    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(providers.get(type))
                .map(provider -> (T) provider.get());
    }

    class ConstructorInjectProvider<T> implements Provider<T> {
        private Class<?> componentType;
        private Constructor<T> injectConstructor;

        private boolean constructing = false;

        public ConstructorInjectProvider(Class<?> componentType, Constructor<T> injectConstructor) {
            this.componentType = componentType;
            this.injectConstructor = injectConstructor;
        }

        @Override
        public T get() {
            if (constructing) {
                throw new CyclicDependenciesFound(componentType);
            }
            try {
                constructing = true;
                Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                        .map(p -> Context.this.get(p.getType()).orElseThrow(() -> new DependencyNotFoundException(p.getType(), componentType)))
                        .toArray(Object[]::new);
                return injectConstructor.newInstance(dependencies);
            } catch (CyclicDependenciesFound e) {
                throw new CyclicDependenciesFound(componentType, e);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
        }
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


}
