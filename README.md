## YARMI (yet-another-RMI)
> YARMI is RMI Framework which provides much more simple way to build server-client application in Java.


### Features
1. Provides high degree of similarity to HTTP Web Service
2. No Java RMI package dependency
3. Provides Service Discovery 


### How-To
#### Build Service (Server)
1. Declare controller    
```java
public interface UserIDPController {

    @Get("/{id}")
    Response<User> getUser(@Path(name = "id") Long userId);

    @Get("/list")
    Response<User> getUsers();

    @Post("/new")
    Response<User> createUser(@Body(name = "user") User user);

} 
```     
2. Implement Controller Stub    
```java
public class UserIDControllerImpl implements UserIDPController {
    @Override
    public Response<User> getUser(Long userId) {
        return null;
    }

    @Override
    public Response<User> getUsers() {
        return null;
    }

    @Override
    public Response<User> createUser(User user) {
        return null;
    }
}
``` 
3. Declare your service with route setting
```java
@Service(name = "test-service",
        params = {"127.0.0.1", "6644"})
public class TestService {

    @Controller(path = "/user", version = 1, module = UserIDControllerImpl.class)
    UserIDPController userIDPService;
}

```   
4. Start your service & advertise
```java
public static class SimpleServer {
    
    public static void main (String[] args) {
        RMIService service = RMIService.create(TestService.class, new LocalServiceAdvertiser());
        service.listen();
    }
}
```

#### Buid Client
1. Discover service & create client
```java
public static class SimpleClient {
    
    public static void main (String[] args) {
        // build target service information
        RMIServiceInfo serviceInfo = RMIServiceInfo.from(TestService.class);
        // start discovery
        LocalServiceDiscovery discovery = new LocalServiceDiscovery();
        discovery.startDiscovery(serviceInfo, discovered -> {
            assert discovered.provide(UserIDPController.class);
            
            // create client side controller for service
            UserIDPController userCtr = RMIClient.create(discovered, TestService.class, UserIDPController.class);
            
            // request to service
            User user = new User();
            user.setName("David");
            
            Response<User> response = userCtr.createUser(user);
            assert response.isSuccessful();
            response = userCtr.getUser(response.getBody().getId());
            assert response.isSuccessful();
            assert user.equals(response.getBody());
            
        });
    }
}
```


### License
> Apache License, Version 2.0