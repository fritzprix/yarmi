<p align="center"><img src="https://s33.postimg.cc/getb2kc33/LOGO_YARMI_Hzt_500px.png"></p>

yarmi is yet-another remote method invocation framework for simple distributed service architecture which provides service discovery mechanism out of the box.
 
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/5c9f40d574c64e629af11f284c447bea)](https://www.codacy.com/app/innocentevil0914/yarmi?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=fritzprix/yarmi&amp;utm_campaign=Badge_Grade)

### Features
1. Simple APIs
>  discover and request service with just a few API calls
2. Support large blob as method parameter or response
> yarmi supports blob exchange between client and server by default with BlobSession which exposes familiar read / write APIs    
3. Provide service discovery out-of-the-box
> service discovery is provided out of the box which is not dependent on any other lookup service.
4. Extensible design
> yarmi core itself is agnostic to network / messaging / discovery / negotiation implementation.



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
            <version>0.0.7</version>
        </dependency>
</dependencies>
```


#### Using Gradle
1. Add Repository
```groovy
allprojects {
    repositories {
        ...
        maven {
            url 'https://raw.githubusercontent.com/fritzprix/yarmi/releases'
        }
        maven {
            url 'https://raw.githubusercontent.com/fritzprix/yarmi/snapshots'
        }
        ...
    }
}
```
2. Add Dependency
```groovy
dependencies {
...
    implementation 'com.doodream:yarmi-core:0.0.7'
    annotationProcessor 'org.projectlombok:lombok:1.16.18'
...
}
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
    
    @Post("/new/thumbnail")
    Response<Long> postThumbnail(@Body(name = "th_nail") BlobSession blob, Long userId);

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
    
    @Override
    public Response<Long> postThumbnail(BlobSession blob, Long userId) {
        // example save blob as file
        byte[] rb = new byte[1024];
        int rsz;
        try {
            Session session = blob.open();
            FileOutputStream fos = new FileOutputStream("thumbnail_" + userId);
            while((rsz = session.read(rb,0, rb.length)) > 0) {
                fos.write(rb, 0, rsz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fos.close();
        }
        return Response.success(userId);    
    }
}  
``` 
3. Declare your service with route configuration
```java
@Service(name = "test-service",
         provider = "com.example",
         params = {
            @AdapterParam(key=TcpServiceAdapter.PARAM_PORT, value = "6644")
         })
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
                public void onDiscovered(RMIServiceInfo info)  {
                    discoveredService.add(RMIServiceInfo.toServiceProxy(info));
                }
    
                @Override
                public void onDiscoveryStarted() { 
                    discoveredService = new LinkedList<>();
                }
    
                @Override
                public void onDiscoveryFinished() {
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
                                
                                // example : upload file as thumbnail images
                                FileInputStream fis = new FileInputStream("./thumbnail.jpg");
                                byte[] buffer = new byte[2048];
                                BlobSession session = new BlobSession(ses -> {
                                    int rsz;
                                    try {
                                        while((rsz = fis.read(buffer)) > 0) {
                                            ses.write(buffer, rsz);
                                        }
                                        ses.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                                Response<Long> thumbResponse = controller.postThumbnail(session, 1L);
                                assert thumbResponse.isSuccessful();
                                
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
