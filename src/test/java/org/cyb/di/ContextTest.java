package org.cyb.di;

import jakarta.inject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.IntStream;

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
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            assertSame(instance, config.getContext().get(ComponentRef.of(TestComponent.class)).get());
        }

        @Test
        public void should_return_empty_if_component_is_not_found() {
            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();

            Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() {}).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();

            assertFalse(context.get(new ComponentRef<List<TestComponent>>(){}).isPresent());
        }

        @Nested
        public class WithQualifier {
            @Test
            public void should_bind_instance_with_multi_qualifier() {
                TestComponent instance = new TestComponent() {
                };

                config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"),
                        new SkywalkerLiteral());
                Context context = config.getContext();;
                TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
                TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get();

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
                        new SkywalkerLiteral()
                );

                Context context = config.getContext();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor chosenOne = context.get(ComponentRef.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor skywalker = context.get(ComponentRef.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new SkywalkerLiteral())).get();
                assertSame(dependency, chosenOne.dependency);
                assertSame(dependency, skywalker.dependency);
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent instance = new TestComponent() {
                };
                assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, instance, new TestLiteral()));
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class, () -> config.bind(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new TestLiteral()));
            }
        }
    }

    @Nested
    public class WithScope {
        //TODO default scope should not be singleton
        static class NotSingleton {
        }

        @Test
        public void should_not_be_singleton_scope_by_default() {
            config.bind(NotSingleton.class, NotSingleton.class);
            Context context = config.getContext();
            assertNotSame(context.get(ComponentRef.of(NotSingleton.class)).get(),
                    context.get(ComponentRef.of(NotSingleton.class)).get());
        }

        //TODO bind component as singleton scoped
        @Test
        public void should_bind_component_as_singleton_scoped() {
            config.bind(NotSingleton.class, NotSingleton.class, new SingletonLiteral());
            Context context = config.getContext();
            assertSame(context.get(ComponentRef.of(NotSingleton.class)).get(),
                    context.get(ComponentRef.of(NotSingleton.class)).get());

        }

        //TODO get scope from component class
        @Singleton
        static class SingletonAnnotated implements Dependency {
        }

        @Test
        public void should_retrieve_scope_annotation_from_component() {
            config.bind(Dependency.class, SingletonAnnotated.class);
            Context context = config.getContext();
            assertSame(context.get(ComponentRef.of(Dependency.class)).get(),
                    context.get(ComponentRef.of(Dependency.class)).get());
        }

        @Nested
        public class WithQualifier {
            @Test
            public void should_not_be_singleton_scope_by_default() {
                config.bind(NotSingleton.class, NotSingleton.class, new SkywalkerLiteral());
                Context context = config.getContext();
                assertNotSame(context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())).get(),
                        context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())).get());
            }

            //TODO bind component with qualifiers as singleton scoped
            @Test
            public void should_bind_component_as_singleton_scoped() {
                config.bind(NotSingleton.class, NotSingleton.class, new SingletonLiteral(), new SkywalkerLiteral());
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())).get(),
                        context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())).get());

            }

            @Test
            public void should_retrieve_scope_annotation_from_component() {
                config.bind(SingletonAnnotated.class, SingletonAnnotated.class, new SkywalkerLiteral());
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(SingletonAnnotated.class, new SkywalkerLiteral())).get(),
                        context.get(ComponentRef.of(SingletonAnnotated.class, new SkywalkerLiteral())).get());
            }
        }
    }

    @Nested
    public class DependencyCheck {
        static class TestComponentWithInjectConstructor implements TestComponent {
            public Dependency getDependency() {
                return dependency;
            }

            private Dependency dependency;

            @Inject
            public TestComponentWithInjectConstructor(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        static class DependencyDependOnComponent implements Dependency {
            private TestComponent component;

            @Inject
            public DependencyDependOnComponent(TestComponent component) {
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
            private TestComponent component;

            @Inject
            public AnotherDependencyDependedComponent(TestComponent component) {
                this.component = component;
            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        @Test
        public void should_throw_exception_if_provider_dependency_not_found() {
            config.bind(TestComponent.class, MissingDependencyProviderConstructor.class);
            DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });
            assertEquals(Dependency.class, dependencyNotFoundException.getDependency().type());
        }

        @Test
        public void should_throw_exception_if_dependency_not_found() {
            config.bind(TestComponent.class, TestComponentWithInjectConstructor.class);
            DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });
            assertEquals(Dependency.class, dependencyNotFoundException.getDependency().type());
            assertEquals(TestComponent.class, dependencyNotFoundException.getComponent().type());
        }

        @Test
        public void should_throw_exception_if_cyclic_dependencies_found() {
            config.bind(TestComponent.class, TestComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependOnComponent.class);

            CyclicDependenciesFound exception = assertThrows(CyclicDependenciesFound.class, () -> config.getContext());

            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
        }

        @Test
        public void should_throw_exception_if_transitive_cyclicdependencies() {
            config.bind(TestComponent.class, TestComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependedComponent.class);

            CyclicDependenciesFound exception = assertThrows(CyclicDependenciesFound.class, () -> config.getContext());
            List<Class<?>> classes = Arrays.asList(exception.getComponents());

            assertEquals(3, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
            assertTrue(classes.contains(AnotherDependency.class));
        }

        @Test
        public void should_bind_component_as_customized_scope() {
            config.scope(Pooled.class, PooledProvider::new);
            config.bind(WithScope.NotSingleton.class, WithScope.NotSingleton.class, new PooledLiteral());
            Context context = config.getContext();
            List<WithScope.NotSingleton> instances = IntStream.range(0, 5).mapToObj(i -> context.get(ComponentRef.of(WithScope.NotSingleton.class)).get()).toList();
            assertEquals(PooledProvider.MAX, new HashSet<>(instances).size());
        }

        @Test
        public void should_throw_exception_if_multi_scope_provided() {
            assertThrows(IllegalComponentException.class, () -> config.bind(WithScope.NotSingleton.class, WithScope.NotSingleton.class, new SingletonLiteral(), new PooledLiteral()));
        }

        @Singleton @Pooled
        static class MultiScopeAnnotated {
        }

        @Test
        public void should_throw_exception_if_multi_scope_annotated() {
            assertThrows(IllegalComponentException.class, () -> config.bind(MultiScopeAnnotated.class, MultiScopeAnnotated.class));
        }

        @Test
        public void should_throw_exception_if_scope_undefined() {
            assertThrows(IllegalComponentException.class, () -> config.bind(WithScope.NotSingleton.class, WithScope.NotSingleton.class, new PooledLiteral()));
        }

        @Nested
        public class WithQualifier {

            @Test
            public void should_throw_exception_if_dependency_with_qualifier_not_found() {
                config.bind(Dependency.class, new Dependency() {
                });
                config.bind(InjectConstructor.class, InjectConstructor.class, new NamedLiteral("Owner"));

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertEquals(new Component(InjectConstructor.class, new NamedLiteral("Owner")), exception.getComponent());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());
            }

            static class InjectConstructor {
                @Inject
                public InjectConstructor(@SkyWalker Dependency dependency) {

                }
            }

            static class SkywalkerDependency implements Dependency {
                @Inject
                public SkywalkerDependency(@jakarta.inject.Named("ChosenOne") Dependency dependency) {

                }
            }

            static class NotCyclicDependency implements Dependency {
                @Inject
                public NotCyclicDependency(@SkyWalker Dependency dependency) {

                }
            }

            @Test
            public void should_not_throw_cyclic_exception_if_component_with_same_type_taged_with_different_qualifier() {
                Dependency instance = new Dependency() {
                };
                config.bind(Dependency.class, instance, new NamedLiteral("ChosenOne"));
                config.bind(Dependency.class, SkywalkerDependency.class, new SkywalkerLiteral());
                config.bind(Dependency.class, NotCyclicDependency.class);

                try {
                    config.getContext();
                } catch (CyclicDependenciesFound ex) {
                    fail();
                }
            }

        }
    }
}

record NamedLiteral(String value) implements Named {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Named.class;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Named named) {
            return Objects.equals(value, named.value());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return "value".hashCode() * 127 ^ value.hashCode();
    }
}

@java.lang.annotation.Documented
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
@jakarta.inject.Qualifier
@interface SkyWalker {
}

record SkywalkerLiteral() implements SkyWalker {
    @Override
    public Class<? extends Annotation> annotationType() {
        return SkyWalker.class;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof SkyWalker;
    }
}

record TestLiteral() implements Test {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}

record SingletonLiteral() implements Singleton {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Singleton.class;
    }
}

@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
@interface Pooled {}

record PooledLiteral() implements Pooled {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Pooled.class;
    }
}

class PooledProvider<T> implements ComponentProvider<T> {
    static int MAX = 2;
    private List<T> pool = new ArrayList<>();
    private ComponentProvider<T> provider;
    private int current;

    public PooledProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (pool.size() < MAX) {
             pool.add(provider.get(context));
        }
        return pool.get(current++ % MAX);
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}
