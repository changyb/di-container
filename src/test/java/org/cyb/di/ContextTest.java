package org.cyb.di;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Nested
public class ContextTest {
    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    class TypeBinding {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            assertSame(instance, config.getContext().get(Context.Ref.of(Component.class)).get());
        }

        @Test
        public void should_return_empty_if_component_is_not_found() {
            Optional<Component> component = config.getContext().get(Context.Ref.of(Component.class));
            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();

            Provider<Component> provider = context.get(new Context.Ref<Provider<Component>>() {}).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();

            assertFalse(context.get(new Context.Ref<List<Component>>(){}).isPresent());
        }

        @Nested
        public class WithQualifier {
            //TODO binding component with qualifier
            @Test
            public void should_bind_instance_with_qualifier() {
                Component instance = new Component() {
                };
                config.bind(Component.class, instance, new NamedLiteral("ChosenOne"));
                Context context = config.getContext();

                Component chosenOne = context.get(Context.Ref.of(Component.class, new NamedLiteral("ChosenOne"))).get();
                assertSame(instance, chosenOne);

            }

            @Test
            public void should_bind_component_with_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                        InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                        new NamedLiteral("ChosenOne"));

                Context context = config.getContext();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor chosenOne = context.get(Context.Ref.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
                assertSame(dependency, chosenOne.dependency);
            }
            //TODO binding component with multi qualifiers
            @Test
            public void should_bind_instance_with_multi_qualifier() {
                Component instance = new Component() {
                };

                config.bind(Component.class, instance, new NamedLiteral("ChosenOne"),
                        new NamedLiteral("Skywalker"));
                Context context = config.getContext();;
                Component chosenOne = context.get(Context.Ref.of(Component.class, new NamedLiteral("ChosenOne"))).get();
                Component skywalker = context.get(Context.Ref.of(Component.class, new NamedLiteral("Skywalker"))).get();

                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);
            }

            @Test
            public void should_bind_component_with_multi_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                        InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                        new NamedLiteral("ChosenOne"),
                        new NamedLiteral("Skywalker")
                );

                Context context = config.getContext();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor chosenOne = context.get(Context.Ref.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor skywalker = context.get(Context.Ref.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new NamedLiteral("Skywalker"))).get();
                assertSame(dependency, chosenOne.dependency);
                assertSame(dependency, skywalker.dependency);
            }

            //TODO throw illegal component if illegal qualifier
        }
    }

    @Nested
    public class DependencyCheck {
        static class ComponentWithInjectConstructor implements Component {
            public Dependency getDependency() {
                return dependency;
            }

            private Dependency dependency;

            @Inject
            public ComponentWithInjectConstructor(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        static class DependencyDependOnComponent implements Dependency {
            private Component component;

            @Inject
            public DependencyDependOnComponent(Component component) {
                this.component = component;
            }
        }

        static class DependencyDependedOnAnotherDependency implements Dependency {
            private AnotherDependency anotherDependency;

            @Inject
            public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
                this.anotherDependency = anotherDependency;
            }
        }

        static class AnotherDependencyDependedComponent implements AnotherDependency {
            private Component component;

            @Inject
            public AnotherDependencyDependedComponent(Component component) {
                this.component = component;
            }
        }

        static class MissingDependencyProviderConstructor implements Component {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        @Test
        public void should_throw_exception_if_provider_dependency_not_found() {
            config.bind(Component.class, MissingDependencyProviderConstructor.class);
            DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });
            assertEquals(Dependency.class, dependencyNotFoundException.getDependency());
        }

        @Test
        public void should_throw_exception_if_dependency_not_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });
            assertEquals(Dependency.class, dependencyNotFoundException.getDependency());
            assertEquals(Component.class, dependencyNotFoundException.getComponent());
        }

        @Test
        public void should_throw_exception_if_cyclic_dependencies_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependOnComponent.class);

            CyclicDependenciesFound exception = assertThrows(CyclicDependenciesFound.class, () -> config.getContext());

            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(Component.class));
            assertTrue(classes.contains(Dependency.class));
        }

        @Test
        public void should_throw_exception_if_transitive_cyclicdependencies() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependedComponent.class);

            CyclicDependenciesFound exception = assertThrows(CyclicDependenciesFound.class, () -> config.getContext());
            List<Class<?>> classes = Arrays.asList(exception.getComponents());

            assertEquals(3, classes.size());
            assertTrue(classes.contains(Component.class));
            assertTrue(classes.contains(Dependency.class));
            assertTrue(classes.contains(AnotherDependency.class));
        }
    }
}

record NamedLiteral(String value) implements Named {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Named.class;
    }
}
