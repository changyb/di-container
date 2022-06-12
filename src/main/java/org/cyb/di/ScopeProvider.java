package org.cyb.di;

interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
