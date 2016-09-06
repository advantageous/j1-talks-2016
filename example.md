```java
package org.advantageous.recommendation;

import io.advantageous.reakt.Expected;
import io.advantageous.reakt.StreamResult;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.reactor.Reactor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

import static io.advantageous.reakt.Expected.expectedNullable;

@Service
public class RecommendationService {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, List<Promise<List<Recommendation>>>> outstandingCallMap = new ConcurrentHashMap<>();
    private final List<RecommendationService> engines = new CopyOnWriteArrayList<>();
    private final List<String> userIds = new ArrayList<>();

    private final UserStoreService userStoreService = new UserStoreService();
    private final Reactor reactor = Reactor.reactor();

    {
        engines.add(new RecommendationService());
    }

    @Init
    private void init() {

        initUserStream();

        reactor.addRepeatingTask(Duration.ofMillis(50), () -> {
            if (userIds.size() > 0) {
                userStoreService.loadUsers(Collections.unmodifiableList(userIds));
                userIds.clear();
            }
        });
    }

    private void loadUserFromStoreService(final String userId) {
        userIds.add(userId);
        if (userIds.size() > 100) {
            userStoreService.loadUsers(Collections.unmodifiableList(userIds));
            userIds.clear();
        }
    }

    private void initUserStream() {

        userStoreService.userStream(userList -> {
            if (userList.complete()) {
                initUserStream();
            } else if (userList.failure()) {
                //Log & Recover
            } else if (userList.success()) {
                reactor.deferRun(() -> {
                    handleListOfUserFromStream(userList);
                });
            }
        });

    }

    private void handleListOfUserFromStream(StreamResult<List<User>> userList) {
        userList.get().stream().forEach(user -> {
            expectedNullable(outstandingCallMap.get(user.getId()))
                    .ifPresent(recommendationPromises ->
                            recommendationPromises.forEach(recommendationPromise ->
                                    pickEngine(user.getId()).recommend(user)
                                            .thenSafe(recommendationPromise::resolve)
                                            .catchError(recommendationPromise::reject)
                                            .invoke()
                            )
                    )
                    .ifAbsent(() -> {
                        //Log not found when expected
                    });
        });
    }


    private Expected<User> getUser(final String userId) {
        return Expected.of(new User(userId));
    }

    private RecommendationEngine pickEngine(final String userId) {
        return new RecommendationEngine();
    }

    @ServiceCall
    public Promise<List<Recommendation>> recommend(final String userId) {
        return invokablePromise(returnPromise ->
                getUser(userId)
                        .ifPresent(user ->
                                pickEngine(userId).recommend(user)
                                        .thenSafe(returnPromise::resolve)
                                        .catchError(returnPromise::reject)
                                        .invoke())
                        .ifAbsent(() -> {
                            loadUserFromStoreService(userId);
                            addOutstandingCall(userId, returnPromise);
                        })
        );
    }

    private void addOutstandingCall(String userId, Promise<List<Recommendation>> returnPromise) {
        Expected.ofNullable(outstandingCallMap.get(userId))
                .ifPresent(promises -> promises.add(returnPromise))
                .ifAbsent(() -> {
                    final List<Promise<List<Recommendation>>> list = new ArrayList<>();
                    list.add(returnPromise);
                    outstandingCallMap.put(userId, list);
                });
    }

}

```


```java
@Service
public class RecommendationService {

    ...
    private final UserStoreService userStoreService = ...;
    private final Reactor reactor = ...;

    @ServiceCall
    public Promise<List<Recommendation>> recommend(final String userId) {
        return invokablePromise(returnPromise ->
                getUser(userId)
                        .ifPresent(user ->
                                pickEngine(userId).recommend(user)
                                        .thenSafe(returnPromise::resolve)
                                        .catchError(returnPromise::reject)
                                        .invoke())
                        .ifAbsent(() -> {
                            loadUserFromStoreService(userId);
                            addOutstandingCall(userId, returnPromise);
                        })
        );
    }

}

```



```java
@Service
public class RecommendationService {

    private final Map<String, User> users = ...;
    private final Map<String, List<Promise<List<Recommendation>>>> outstandingCalls = ...;
    private final List<RecommendationService> engines = ...;
    private final List<String> userIdsToLoad = ...;
    private final UserStoreService userStoreService = ...;
    private final Reactor reactor = ...;

    @Init
    private void init() {

        initUserStream();
        //
        reactor.addRepeatingTask(Duration.ofMillis(50), () -> {
            if (userIds.size() > 0) {
                userStoreService.loadUsers(Collections.unmodifiableList(userIdsToLoad));
                userIdsToLoad.clear();
            }
        });
    }

    private void loadUserFromStoreService(final String userId) {
        userIdsToLoad.add(userId);
        if (userIdsToLoad.size() > 100) {
            userStoreService.loadUsers(Collections.unmodifiableList(userIds));
            userIdsToLoad.clear();
        }
    }

    //Work with user stream from service store
    private void initUserStream() {

        userStoreService.userStream(userList -> {
            if (userList.complete()) {
                initUserStream();
            } else if (userList.failure()) {
                //Log & Recover
            } else if (userList.success()) {
                reactor.deferRun(() -> {
                    handleListOfUserFromStream(userListResult);
                });
            }
        });
    }

    private void handleListOfUserFromStream(StreamResult<List<User>> userListResult) {
        userListResult.get().stream().forEach(user -> {
            users.put(user.getId(), user);
            expectedNullable(outstandingCalls.get(user.getId()))
                    .ifPresent(recommendationPromises ->
                            recommendationPromises.forEach(recommendationPromise ->
                                    pickEngine(user.getId()).recommend(user)
                                            .thenSafe(recommendationPromise::resolve)
                                            .catchError(recommendationPromise::reject)
                                            .invoke()
                            )
                    )
                    .ifAbsent(() -> {
                        //Log not found when expected
                    });
        });
    }


    private Expected<User> getUser(final String userId) { return expectedNullable(users.get(userId); }
...
    private void addOutstandingCall(String userId, Promise<List<Recommendation>> returnPromise) {
        expectedNullable(outstandingCallMap.get(userId))
                .ifPresent(promises -> promises.add(returnPromise))
                .ifAbsent(() -> {
                    final List<Promise<List<Recommendation>>> list = new ArrayList<>();
                    list.add(returnPromise);
                    outstandingCallMap.put(userId, list);
                });
    }

}

```
