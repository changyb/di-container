package org.cyb.di;

import java.util.List;

interface ComponentProvider<T> {
    T get(Context context);

    default List<ComponentRef<?>> getDependencies() {
        return List.of();
    }

}
