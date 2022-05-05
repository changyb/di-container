package org.cyb.di;

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
            @Override
            public <T> Optional<T> get(Class<T> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(provider -> (T) provider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
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
    }

    interface ComponentProvider<T> {
        T get(Context context);
        default List<Class<?>> getDependencies() {
            return List.of();
        }
    }


}
