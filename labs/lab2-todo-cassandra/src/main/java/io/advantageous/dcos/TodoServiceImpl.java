package io.advantageous.dcos;


import io.advantageous.discovery.DiscoveryService;
import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.annotation.RequestMapping;
import io.advantageous.qbit.annotation.RequestMethod;
import io.advantageous.qbit.annotation.RequestParam;
import io.advantageous.qbit.annotation.http.DELETE;
import io.advantageous.qbit.annotation.http.GET;
import io.advantageous.qbit.annotation.http.POST;
import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import static io.advantageous.reakt.promise.Promises.invokablePromise;


/**
 * Default port for admin is 7777.
 * Default port for main endpoint is 8888.
 * <p>
 * <pre>
 * <code>
 *
 *     Access the service:
 *
 *    $ curl http://localhost:8888/v1/...
 *
 *
 *     To see swagger file for this service:
 *
 *    $ curl http://localhost:7777/__admin/meta/
 *
 *     To see health for this service:
 *
 *    $ curl http://localhost:8888/__health -v
 *     Returns "ok" if all registered health systems are healthy.
 *
 *     OR if same port endpoint health is disabled then:
 *
 *    $ curl http://localhost:7777/__admin/ok -v
 *     Returns "true" if all registered health systems are healthy.
 *
 *
 *     A node is a service, service bundle, queue, or server endpoint that is being monitored.
 *
 *     List all service nodes or endpoints
 *
 *    $ curl http://localhost:7777/__admin/all-nodes/
 *
 *
 *      List healthy nodes by name:
 *
 *    $ curl http://localhost:7777/__admin/healthy-nodes/
 *
 *      List complete node information:
 *
 *    $ curl http://localhost:7777/__admin/load-nodes/
 *
 *
 *      Show service stats and metrics
 *
 *    $ curl http://localhost:8888/__stats/instance
 * </code>
 * </pre>
 */
@RequestMapping("/todo-service")
public class TodoServiceImpl implements TodoService {


    private final ServiceManagementBundle mgmt;
    private final DiscoveryService discoveryService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TodoRepo todoRep;


    public TodoServiceImpl(ServiceManagementBundle mgmt, TodoRepo todoRepo) {
        this.mgmt = mgmt;
        this.todoRep = todoRepo;

        /** Send stat count i.am.alive every three seconds.  */
        mgmt.reactor().addRepeatingTask(Duration.ofSeconds(3),
                () -> mgmt.increment("i.am.alive"));

        mgmt.reactor().deferRun(() -> todoRepo.connect()
                .catchError(error -> logger.error("Error connecting to repo", error))
                .then(flag -> logger.error("Connecting to repo {}", flag))
                .invoke());

        logger.info("Creating discovery service");
        discoveryService = DiscoveryService.create(URI.create("marathon://marathon.mesos:8080/"));


        logger.info("Todo service created");
    }


    @Override
    @POST(value = "/todo")
    public Promise<Boolean> addTodo(final Todo todo) {
        logger.debug("Add Todo to list {}", todo);
        return invokablePromise(promise -> {
            /** Send KPI addTodo called every time the addTodo method gets called. */
            mgmt.increment("addTodo.called");
            todoRep.addTodo(todo).invokeWithPromise(promise);
        });
    }


    @Override
    @DELETE(value = "/todo")
    public final Promise<Boolean> removeTodo(final @RequestParam("id") String id) {
        logger.debug("Add Todo from list {}", id);
        return invokablePromise(promise -> {
            /** Send KPI addTodo.removed every time the removeTodo method gets called. */
            mgmt.increment("removeTodo.called");
            //not implemented
            promise.accept(true);
        });
    }


    @Override
    @GET(value = "/todo")
    public final Promise<List<Todo>> listTodos() {
        logger.debug("List todos");
        return invokablePromise(promise -> {
            /** Send KPI addTodo.listTodos every time the listTodos method gets called. */
            mgmt.increment("listTodos.called");
            todoRep.loadTodos().invokeWithPromise(promise);
        });
    }


    @POST(value = "/service")
    public final Promise<List<URI>> listServices(URI uri) {
        logger.debug("List services");
        return discoveryService.lookupService(uri);
    }

}
