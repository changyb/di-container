package org.cyb.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    Context context;

    @BeforeEach
    public void setUp() {
        context = new Context();
    }

    @Nested
    public class ComponetConstruction {
        // TODO: instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            context.bind(Component.class, instance);
            assertSame(instance, context.get(Component.class).get());
        }

        @Test
        public void should_return_empty_if_component_is_not_found () {
            Optional<Component> component = context.get(Component.class);
            assertTrue(component.isEmpty());
        }

        // TODO: abstract class
        // TODO: interface

        @Nested
        public class ConstructorInjection {
            // TODO: No args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = context.get(Component.class).get();
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            // TODO: with dependencies
            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Component component = context.get(Component.class).get();
                assertNotNull(component);
                assertSame(dependency, ((ComponentWithInjectConstructor)component).getDependency());
            }

            // TODO: A -> B -> C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "indirect dependency");

                Component instance = context.get(Component.class).get();
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor)instance).getDependency();
                assertNotNull(dependency);

                assertEquals("indirect dependency", ((DependencyWithInjectConstructor)dependency).getDependency());

            }

            //TODO: multi inject constructors
            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                   context.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            @Test
            public void should_throw_exception_if_no_inject_constructor_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            //TODO: no default constructor and inject constructor
            @Test
            public void should_throw_exception_if_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> {
                    context.get(Component.class).get();
                });
                assertEquals(Dependency.class, dependencyNotFoundException.getDependency());
                assertEquals(Component.class, dependencyNotFoundException.getComponent());
            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    context.get(Component.class).get();
                });

                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }

            //TODO: dependencies not exist

            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependOnComponent.class);

                CyclicDependenciesFound exception = assertThrows(CyclicDependenciesFound.class, () -> context.get(Component.class));

                Set<Class<?>> classes = Sets.newSet(exception.getComponents());

                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }

            @Test
            public void should_throw_exception_if_transitive_cyclicdependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                context.bind(AnotherDependency.class, AnotherDependencyDependedComponent.class);

                CyclicDependenciesFound exception = assertThrows(CyclicDependenciesFound.class, () -> context.get(Component.class));
                List<Class<?>> classes = Arrays.asList(exception.getComponents());

                assertEquals(3, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
                assertTrue(classes.contains(AnotherDependency.class));
            }

        }

        @Nested
        public class FiledInjection {

        }

        @Nested
        public class MethodInjection {

        }
    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }
}

interface Component {
}

interface Dependency {
}

interface AnotherDependency {

}

class ComponentWithDefaultConstructor implements Component {
}

class ComponentWithInjectConstructor implements Component {
    public Dependency getDependency() {
        return dependency;
    }

    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }
}

class ComponentWithMultiInjectConstructors implements Component {
    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {

    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}

class DependencyWithInjectConstructor implements Dependency {
    private String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class DependencyDependOnComponent implements Dependency {
    private Component component;

    @Inject
    public DependencyDependOnComponent(Component component) {
        this.component = component;
    }
}

class AnotherDependencyDependedComponent implements AnotherDependency {
    private Component component;

    @Inject
    public AnotherDependencyDependedComponent(Component component) {
        this.component = component;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency {
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}
