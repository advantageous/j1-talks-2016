package io.advantageous.j1.reakt.repo;

import io.advantageous.config.Config;
import io.advantageous.j1.reakt.ConfigUtils;
import io.advantageous.j1.reakt.Todo;
import io.advantageous.qbit.admin.ManagedServiceBuilder;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.test.DockerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;
import java.util.List;

import static io.advantageous.j1.reakt.Main.createManagedServiceBuilder;
import static io.advantageous.j1.reakt.Main.createTodoRepo;
import static org.junit.Assert.*;

@Category(DockerTest.class)
public class TodoRepoTest {


    private final Config config = ConfigUtils.getConfig("todo");

    private TodoRepo todoRepo;
    private  ManagedServiceBuilder managedServiceBuilder;

    @Before
    public void before() throws Exception {
        managedServiceBuilder = createManagedServiceBuilder(config);
        todoRepo = createTodoRepo(config, managedServiceBuilder, true);
        todoRepo.connect().invokeAsBlockingPromise(Duration.ofSeconds(20)).get();
        System.out.println("Connected");
    }

    @After
    public void after() throws Exception {
        todoRepo.close();
    }

    @Test
    public void addTodo() throws Exception {
        final Promise<Boolean> promise = todoRepo.addTodo(new Todo("Rick", "Rick", System.currentTimeMillis(),"abc"))
                .invokeAsBlockingPromise();
        assertTrue(promise.success());
        assertTrue(promise.get());
    }

    @Test
    public void loadTodos() throws Exception {

        for (int i = 1; i < 10; i++) {
            todoRepo.addTodo(new Todo("Geoff"+i, "Geoff"+i, System.currentTimeMillis(), "xyz"))
                    .invokeAsBlockingPromise().get();
        }

        final List<Todo> todos = todoRepo.loadTodos().invokeAsBlockingPromise().get();
        assertTrue(todos.size()>=10);
        System.out.println(todos);
    }

}