package org.cyb.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class ContextConfig {
    private Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    public <T> void bind(Class<T> type, T instance) {
        components.put(new Component(type, null), (ComponentProvider<T>) context -> instance);
    }

    public <T> void bind(Class<T> type, T instance, Annotation... annotations) {
        if (Arrays.stream(annotations).map(Annotation::annotationType)
                .anyMatch(t -> !t.isAnnotationPresent(Qualifier.class) && !t.isAnnotationPresent(Scope.class))) {
            throw new IllegalComponentException();
        }

        for (Annotation qualifier : annotations) {
            components.put(new Component(type, qualifier), context -> instance);
        }
    }

    public <T, U extends T>
    void bind(Class<T> type, Class<U> implementation) {
        bind(type, implementation, implementation.getAnnotations());
    }

    public <T, U extends T>
    void bind(Class<T> type, Class<U> implementation, Annotation... annotations) {
        Map<Class<?>, List<Annotation>> annotationGroups = Arrays.stream(annotations).collect(groupingBy(this::typeOf, toList()));

        if (annotationGroups.containsKey(Illgeal.class)) {
            throw new IllegalComponentException();
        }

        bind(type, annotationGroups.getOrDefault(Qualifier.class, List.of()),
                createScopedProvider(implementation, annotationGroups.getOrDefault(Scope.class, List.of()),
                        new InjectProvider<U>(implementation)));
    }

    private <T> ComponentProvider<?> createScopedProvider(Class<T> implementation, List<Annotation> scopes, ComponentProvider<?> injectionProvider) {
        if (scopes.size() > 1) {
            throw new IllegalComponentException();
        }
        Optional<Annotation> scope = scopes.stream().findFirst()
                .or(() -> scopeFrom(implementation));
        return scope.<ComponentProvider<?>>map(s -> getScopeProvider(s, injectionProvider))
                .orElse(injectionProvider);
    }

    private <T> void bind(Class<T> type, List<Annotation> qualifiers, ComponentProvider<?> provider) {
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    private <T> Optional<Annotation> scopeFrom(Class<T> implementation) {
        return Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();
    }

    private Class<?> typeOf(Annotation annotation) {
        Class<? extends Annotation> type = annotation.annotationType();
        return Stream.of(Qualifier.class, Scope.class).filter(type::isAnnotationPresent).findFirst().orElse(Illgeal.class);
    }

    private @interface Illgeal {

    }


    private ComponentProvider<?> getScopeProvider(Annotation scope, ComponentProvider<?> provider) {
        if (!scopes.containsKey(scope.annotationType())) {
            throw new IllegalComponentException();
        }
        return scopes.get(scope.annotationType()).create(provider);
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scope, ScopeProvider provider) {
        scopes.put(scope, provider);
    }


    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {
            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) {
                        return Optional.empty();
                    }
                    return (Optional<ComponentType>) Optional.ofNullable(getProvider(ref))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getProvider(ref))
                        .map(provider -> (ComponentType)provider.get(this));
            }
        };
    }

    private <ComponentType> ComponentProvider<?> getProvider(ComponentRef<ComponentType> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFoundException(dependency.component(), component);
            }

            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) {
                    throw new CyclicDependenciesFound(visiting);
                }

                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }
}
