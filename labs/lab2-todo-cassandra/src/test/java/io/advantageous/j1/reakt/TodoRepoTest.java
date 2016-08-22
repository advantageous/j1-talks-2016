package io.advantageous.j1.reakt;

import io.advantageous.reakt.promise.Promise;
import io.advantageous.test.DockerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.Assert.*;

@Category(DockerTest.class)
public class TodoRepoTest {

    private TodoRepo todoRepo;

    @Before
    public void before() throws Exception {

        todoRepo = new TodoRepo(1, ConfigUtils.getConfig("todo").getConfig("cassandra").getUriList("uris"));
        todoRepo.connect().invokeAsBlockingPromise().get();
        Thread.sleep(1000);

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

    @Test
    public void connect() throws Exception {

    }

}