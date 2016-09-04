package io.advantageous.j1.reakt.repo;

import io.advantageous.j1.reakt.Todo;
import io.advantageous.reakt.Expected;
import io.advantageous.reakt.promise.Promise;

import java.util.List;

public interface TodoRepo {
    Promise<Expected<Todo>> loadTodo(String id);
    Promise<Boolean> addTodo(Todo todo);
    Promise<List<Todo>> loadTodos();
    Promise<Boolean> connect();

    void close();
}
