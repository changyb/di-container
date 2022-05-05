package org.cyb.di;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

class InjectProvider<T> implements ContextConfig.ComponentProvider<T> {
    private Constructor<T> injectConstructor;
    private List<Field> injectFields;
    private List<Method> injectMethods;

    public InjectProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }

        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);

        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }

        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
                        .filter(m -> isOverrideByInjectMethod(methods, m))
                        .filter(method -> isOverrideByNoInjectMethod(component, method))
                        .toList());
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        return traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList());
    }

    private static <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getDeclaredConstructors()).toList();

        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> getDefaultConstructor(implementation));
    }

    @Override
    public T get(Context context) {
        try {
            T instance = this.injectConstructor.newInstance(toDependencies(context, this.injectConstructor));
            for (Field field : injectFields) {
                field.set(instance, toDependency(context, field));
            }
            for (Method method: injectMethods) {
                method.invoke(instance, toDependencies(context, method));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(Stream.concat(stream(injectConstructor.getParameterTypes()),
                injectFields.stream().map(Field::getType)),
                injectMethods.stream().flatMap(m -> stream(m.getParameterTypes()))
                ).toList();
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> function) {
        List<T> members = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            members.addAll(function.apply(members, current));
            current = current.getSuperclass();
        }
        return members;
    }

    private static <T> Constructor<T> getDefaultConstructor(Class<T> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameters())
                .map(p -> context.get(p.getType()).get())
                .toArray(Object[]::new);
    }

    private static Object toDependency(Context context, Field field) {
        return context.get(field.getType()).get();
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().endsWith(m.getName())
                && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(c -> c.isAnnotationPresent(Inject.class));
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method method) {
        return stream(component.getDeclaredMethods()).filter(m -> !m.isAnnotationPresent(Inject.class)).noneMatch(o -> isOverride(method, o));
    }

    private static boolean isOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }
}
