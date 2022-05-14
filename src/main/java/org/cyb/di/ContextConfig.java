package org.cyb.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class ContextConfig {
    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, (ComponentProvider<T>) context -> instance);
    }

    public <T, U extends T>
    void bind(Class<T> type, Class<U> implementation) {
        providers.put(type, new InjectProvider<>(implementation));;
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {
            private <T> Optional<T> getComponent(Class<T> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(provider -> (T) provider.get(this));
            }

            private Optional getContainer(ParameterizedType type) {
                if (type.getRawType() != Provider.class) {
                    return Optional.empty();
                }
                return Optional.ofNullable(providers.get(getComponentType(type)))
                        .map(provider -> (Provider<Object>) () -> provider.get(this));
            }

            @Override
            public Optional get(Type type) {
                if (isContainerType(type)) {
                    return getContainer((ParameterizedType) type);
                }
                return getComponent((Class<?>) type);

            }
        };
    }

    private Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType)type).getActualTypeArguments()[0];
    }

    private boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (isContainerType(dependency)) {
                checkContainerTypeDependency(component, dependency);
            } else {
                checkComponentDependency(component, visiting, (Class<?>) dependency);
            }
        }
    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        Class<?> type = getComponentType(dependency);
        if (!providers.containsKey(type)) {
            throw new DependencyNotFoundException(type, component);
        }
    }

    private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) {
            throw new DependencyNotFoundException(dependency, component);
        }

        if (visiting.contains(dependency)) {
            throw new CyclicDependenciesFound(visiting);
        }

        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        T get(Context context);
        default List<Type> getDependencies() {return List.of();}
    }


}
