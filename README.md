<p align="center"><img src="https://s33.postimg.cc/getb2kc33/LOGO_YARMI_Hzt_500px.png"></p>

yarmi is yet anotehr RMI based on JSON. it's simple yet powerful when developing server & client based distributed application within a network of small scale
 
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/5c9f40d574c64e629af11f284c447bea)](https://www.codacy.com/app/innocentevil0914/yarmi?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=fritzprix/yarmi&amp;utm_campaign=Badge_Grade)

### Features
1. Zero-cost migration to (from) RESTful application 
> Provides conceptual similarity to popular RESTful application framework (like service / controller mapping). 
> and that means not only the migration from / to RESTful implementation is easy
> but also implementing proxy for any RESTful service in heterogeneous network scenario (like typical IoT application) is also well supported  
2. No Java RMI package dependency
> yarmi uses JSON as its ser-deserializer, so any language specific dependency is not required.
> and also intended to be cross-platform to support constrained system (such as Raspberrypi) in which the Java runtime is not available or restricted (NOT SUPPORTED YET)
3. Support various transport  
> yarmi also provides abstraction over transport layer so it can over any kinds of transport like tcp / ip or bluetooth rfcomm.

### How-To

#### Using Maven
1. Add Maven Repository
```xml
<repositories>
    <repository>
        <id>yarmi-core</id>
        <name>yarmi</name>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <url>https://raw.githubusercontent.com/fritzprix/yarmi/releases</url>
    </repository>
</repositories>
```
2. Add dependency 
```xml
<dependencies>
        <dependency>
            <groupId>com.doodream</groupId>
            <artifactId>yarmi-core</artifactId>
            <version>0.0.2</version>
        </dependency>
</dependencies>
```

#### Build Service (Server)
1. Declare controller stub in a very similar way doing with Spring REST Controller
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
3. Declare your service with route configuration
```java
@Service(name = "test-service",
        params = {"6644"})
public class TestService {

    @Controller(path = "/user", version = 1, module = UserIDControllerImpl.class)
    UserIDPController userIDPService;
}

```   
4. Start service & advertise it 
```java
public static class SimpleServer {
    
    public static void main (String[] args) {
        RMIService service = RMIService.create(TestService.class, new SimpleServiceAdvertiser());
        service.listen(true);
        // listen will block, you can change the blocking behaviour with the argument
    }
}
```

#### Build Client
1. Discover service & create client
```java
public static class SimpleClient {
    
    public static void main (String[] args) {
            // build target service information
            
            SimpleServiceDiscovery discovery = new SimpleServiceDiscovery();
            discovery.startDiscovery(TestService.class, new ServiceDiscoveryListener() {
                @Override
                public void onDiscovered(RMIServiceProxy proxy)  {
                    discoveredService.add(proxy);
                }
    
                @Override
                public void onDiscoveryStarted() { 
                    discoveredService = new LinkedList<>();
                }
    
                @Override
                public void onDiscoveryFinished() {
                    // pick RMIServiceProxy and create client
                    if(discoveredService == null) {
                        return;
                    }
                    if(discoveredService.size() > 0) {
                        RMIServiceProxy serviceProxy = discoveredService.get(0);
                        if(!serviceProxy.provide(UserIDPController.class)) {
                            // check given controller is provided from the service
                            return;
                        }
                        try {
                                UserIDPController userCtr = RMIClient.create(serviceProxy, TestService.class, UserIDPController.class);
                                // will be create client-side proxy 
                                // and use it just like simple method call
                                
                                User user = new User();
                                user.setName("David");
        
                                Response<User> response = userCtr.createUser(user);
                                assert response.isSuccessful();
                                user = response.getBody();
                                
                                response = userCtr.getUser(user.getId());
                                assert response.isSuccessful();
                                response.getBody();
                            } catch (IllegalAccessException | InstantiationException | IOException e) {
                                e.printStackTrace();
                            }    
                        }
                        
                    }
            });
            
    }
}
```

### License
> Apache License, Version 2.0
