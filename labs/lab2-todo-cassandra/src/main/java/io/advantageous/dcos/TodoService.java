package io.advantageous.dcos;

import io.advantageous.reakt.promise.Promise;

import java.util.List;

public interface TodoService {
    Promise<Boolean> addTodo(Todo todo);
    Promise<Boolean> removeTodo(String id);
    Promise<List<Todo>> listTodos();
}