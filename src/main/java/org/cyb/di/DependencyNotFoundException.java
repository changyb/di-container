package org.cyb.di;

public class DependencyNotFoundException extends RuntimeException{
    private Class<?> dependency;

    public DependencyNotFoundException(Class<?> dependency, Class<?> component) {
        this.dependency = dependency;
        this.component = component;
    }

    private Class<?> component;

    public DependencyNotFoundException(Class<?> dependency) {
        this.dependency = dependency;
    }

    public Class<?> getDependency() {
        return dependency;
    }

    public Class<?> getComponent() {
        return component;
    }
}
