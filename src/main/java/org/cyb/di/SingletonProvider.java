package org.cyb.di;

import java.util.List;

class SingletonProvider<T> implements ContextConfig.ComponentProvider<T> {
    private T singleton;
    private ContextConfig.ComponentProvider<T> provider;

    public SingletonProvider(ContextConfig.ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (singleton == null) {
            singleton = provider.get(context);
        }
        return singleton;
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}
