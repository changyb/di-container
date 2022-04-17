package org.cyb.di;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    interface Component {

    }

    public static class ComponentWithDefaultConstructor implements Component {

    }

    @Nested
    public class ComponetConstruction {
        // TODO: instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Context context = new Context();

            Component instance = new Component() {
            };
            context.bind(Component.class, instance);
            assertSame(instance, context.get(Component.class));
        }

        // TODO: abstract class
        // TODO: interface

        @Nested
        public class ConstructorInjection {
            // TODO: No args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                Context context = new Context();
                context.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = context.get(Component.class);
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            // TODO: with dependencies
            // TODO: A -> B -> C
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
