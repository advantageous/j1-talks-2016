package io.advantageous.j1.reakt;

public class Todo {

    private final String id;
    private final String name;
    private final String description;
    private final long createdTime;
    private final long updatedTime;

    public Todo(String name, String description, long createdTime, String id, long updatedTime) {
        this.name = name;
        this.description = description;
        this.createdTime = createdTime;
        this.id = id;
        this.updatedTime = updatedTime;
    }

    public Todo(String name, String description, String id, long updatedTime) {
        this.name = name;
        this.description = description;
        this.createdTime = 0L;
        this.id = id;
        this.updatedTime = updatedTime;
    }

    public Todo(Todo todo, long createTime) {
        this.name = todo.name;
        this.description = todo.description;
        this.createdTime = createTime;
        this.id = todo.id;
        this.updatedTime = todo.updatedTime;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getUpdatedTime() { return updatedTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Todo todo = (Todo) o;

        if (updatedTime != todo.updatedTime) return false;
        if (id != null ? !id.equals(todo.id) : todo.id != null) return false;
        if (name != null ? !name.equals(todo.name) : todo.name != null) return false;
        return description != null ? description.equals(todo.description) : todo.description == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (int) (updatedTime ^ (updatedTime >>> 32));
        return result;
    }
}
