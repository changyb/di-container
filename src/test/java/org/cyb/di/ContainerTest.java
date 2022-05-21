package org.cyb.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

public class ContainerTest {

    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }
}

interface TestComponent {
}

interface Dependency {
}

interface AnotherDependency {

}



class TestComponentWithMultiInjectConstructors implements TestComponent {
    @Inject
    public TestComponentWithMultiInjectConstructors(String name, Double value) {
    }

    @Inject
    public TestComponentWithMultiInjectConstructors(String name) {
    }
}

class TestComponentWithNoInjectConstructorNorDefaultConstructor implements TestComponent {

    public TestComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}




