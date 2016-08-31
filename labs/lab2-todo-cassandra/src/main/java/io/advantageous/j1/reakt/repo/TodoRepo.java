package io.advantageous.j1.reakt.repo;

import io.advantageous.j1.reakt.Todo;
import io.advantageous.reakt.promise.Promise;

import java.util.List;

/**
 * Created by rick on 8/30/16.
 */
public interface TodoRepo {
    Promise<Boolean> addTodo(Todo todo);

    Promise<List<Todo>> loadTodos();

    Promise<Boolean> connect();

    void close();
}
