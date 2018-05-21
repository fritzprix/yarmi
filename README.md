## yarmi (yet-another-RMI)
> yarmi is yet anotehr RMI based on JSON. it's simple yet powerful when developing server & client based distributed application within a narrow range of network 

### Features
1. Zero-cost migration to (from) RESTful application 
> Provides conceptual similarity to popular RESTful application framework (like service / controller mapping). 
> and that means not only the migration from / to RESTful implementation is easy
> but also implementing proxy for any RESTful service in heterogeneous network scenario (like typical IoT application) is also well supported  
2. No Java RMI package dependency
> yarmi uses JSON as its ser-deserializer, so any language specific dependency is not required.
> and also intended to be cross-platform to support constrained system (such as Raspberrypi) in which the Java runtime is not available or restricted (NOT SUPPORTED YET)
3. Support various network technology  
> yarmi also provides service discovery module which support various network from TCP/IP to its counter part in Bluetooth (like RFCOMM)

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

    private HashMap<Long, User> userTable = new HashMap<>();

    @Override
    public Response<User> getUser(Long userId) {
        User user = userTable.get(userId);
        if(user == null) {
            return null;
        }
        return Response.success(user, User.class);
    }

    @Override
    public Response<User> getUsers() {
        return null;
    }

    @Override
    public Response<User> createUser(User user) {
        int id = user.hashCode();
        userTable.put((long) id, user);
        user.id = (long) id;
        return Response.success(user, User.class);
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
4. Start & advertise your service 
```java
public static class SimpleServer {
    
    public static void main (String[] args) {
        RMIService service = RMIService.create(TestService.class, new SimpleServiceAdvertiser());
        service.listen();
    }
}
```

#### Build Client
1. Discover service & create client
```java
public static class SimpleClient {
    
    public static void main (String[] args) {
            // build target service information
            List<RMIServiceProxy> discoveredService = new LinkedList<>();
            RMIServiceInfo serviceInfo = RMIServiceInfo.from(TestService.class);
            
            SimpleServiceDiscovery discovery = new SimpleServiceDiscovery();
            discovery.startDiscovery(serviceInfo, new ServiceDiscoveryListener() {
                @Override
                public void onDiscovered(RMIServiceProxy proxy)  {
                    discoveredService.add(proxy);
                }
    
                @Override
                public void onDiscoveryStarted() { }
    
                @Override
                public void onDiscoveryFinished() {
                    // discovery finished 
                    // pick RMIServiceProxy and create client
                    if(discoveredService.size() > 0) {
                        RMIServiceProxy serviceProxy = discoveredService.get(0);
                        assert serviceProxy.provide(UserIDPController.class);
                        try {
                            UserIDPController userCtr = RMIClient.create(serviceProxy, TestService.class, UserIDPController.class);
                            
                            User user = new User();
                            user.setName("David");
    
                            Response<User> response = userCtr.createUser(user);
                            assert response.isSuccessful();
                            response = userCtr.getUser(1L);
                            assert !response.isSuccessful();
                            assert user.equals(response.getBody());
                        } catch (IllegalAccessException | InstantiationException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 2L, TimeUnit.SECONDS);
            
    }
}
```


### License
> Apache License, Version 2.0