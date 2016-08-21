package io.advantageous.dcos;

import io.advantageous.reakt.promise.Promise;
import io.advantageous.test.DockerTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.*;

@Category(DockerTest.class)
public class TodoRepoTest {

    TodoRepo todoRepo;

    @Before
    public void before() throws Exception {

        todoRepo = new TodoRepo(1, URI.create("cassandra://192.168.99.100:39042"));
        todoRepo.connect().invokeAsBlockingPromise().get();
        Thread.sleep(1000);

    }

    @Test
    public void addTodo() throws Exception {
        final Promise<Boolean> promise = todoRepo.addTodo(new Todo("Rick", "Rick", System.currentTimeMillis()))
                .invokeAsBlockingPromise();
        assertTrue(promise.success());
        assertTrue(promise.get());
    }

    @Test
    public void loadTodos() throws Exception {

        for (int i = 1; i < 10; i++) {
            todoRepo.addTodo(new Todo("Geoff"+i, "Geoff"+i, System.currentTimeMillis()))
                    .invokeAsBlockingPromise().get();
        }

        final List<Todo> todos = todoRepo.loadTodos().invokeAsBlockingPromise().get();
        assertTrue(todos.size()>=10);
        System.out.println(todos);

    }

    @Test
    public void connect() throws Exception {

    }

}