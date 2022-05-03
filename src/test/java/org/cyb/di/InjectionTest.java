package org.cyb.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class InjectionTest {
    private Dependency dependency = mock(Dependency.class);

    private Context context = mock(Context.class);

    @BeforeEach
    public void setUp() {
        when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {
            static class DefaultConstructor {
            }

            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                DefaultConstructor instance = new ConstructorInjectProvider<>(DefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            static class InjectConstructor {
                Dependency dependency;

                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_constructor() {
                InjectConstructor component = new ConstructorInjectProvider<>(InjectConstructor.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                ConstructorInjectProvider<InjectConstructor> provider = new ConstructorInjectProvider<>(InjectConstructor.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }
        }

        @Nested
        class IllegelInjectConstructor {
            abstract class AbstractComponent implements Component {
                @Inject
                public AbstractComponent() {
                }
            }

            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectProvider<>(AbstractComponent.class));
            }

            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectProvider<>(AbstractComponent.class));
            }

            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    new ConstructorInjectProvider<>(ComponentWithMultiInjectConstructors.class);
                });
            }

            @Test
            public void should_throw_exception_if_no_inject_constructor_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    new ConstructorInjectProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }
        }

    }

    @Nested
    public class FiledInjection {

        @Nested
        class Injection {
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }

            @Test
            public void should_inject_dependency_via_field() {
                ComponentWithFieldInjection component = new ConstructorInjectProvider<>(ComponentWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                SubclassWithFieldInjection component = new ConstructorInjectProvider<>(SubclassWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);

            }

            @Test
            public void should_include_dependency_from_field_dependency() {
                ConstructorInjectProvider<ComponentWithFieldInjection> provider = new ConstructorInjectProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }
        }

        @Nested
        class IllegalInjectFields {
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectProvider<>(FinalInjectField.class));
            }
        }
    }

    @Nested
    public class MethodInjection {

        @Nested
        class Injection {
            static class InjectMethodWithNoDependency {
                boolean called;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                InjectMethodWithNoDependency component = new ConstructorInjectProvider<>(InjectMethodWithNoDependency.class).get(context);
                assertTrue(component.called);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_method() {
                new ConstructorInjectProvider<>(InjectMethodWithDependency.class).get(context);
                InjectMethodWithDependency component = new ConstructorInjectProvider<>(InjectMethodWithDependency.class).get(context);
                assertSame(dependency, component.dependency);
            }

            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                void install() {
                    superCalled++;
                }
            }

            static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            public void should_inject_dependencies_via_inject_method_from_superclass() {
                SubclassWithInjectMethod component = new ConstructorInjectProvider<>(SubclassWithInjectMethod.class).get(context);
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }

            static class SubclassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
                @Inject
                @Override
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_only_call_once_if_subclass_override_inject_method_with_inject() {
                SubclassOverrideSuperClassWithInject component = new ConstructorInjectProvider<>(SubclassOverrideSuperClassWithInject.class).get(context);

                assertEquals(1, component.superCalled);
            }

            static class SubclassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_override_with_no_inject() {
                SubclassOverrideSuperClassWithNoInject component = new ConstructorInjectProvider<>(SubclassOverrideSuperClassWithNoInject.class).get(context);

                assertEquals(0, component.superCalled);
            }

            @Test
            public void should_include_dependencies_from_inject_method() {
                ConstructorInjectProvider<InjectMethodWithDependency> provider = new ConstructorInjectProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

        }

        @Nested
        class IllegalInjectMethods {
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectProvider<>(InjectMethodWithTypeParameter.class));
            }
        }

    }
}
