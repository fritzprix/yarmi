<p align="center"><img src="https://s33.postimg.cc/getb2kc33/LOGO_YARMI_Hzt_500px.png"></p>

RMI (Remote Method Invocation) framework for auto configuring micro service architecture  
 
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/5c9f40d574c64e629af11f284c447bea)](https://www.codacy.com/app/innocentevil0914/yarmi?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=fritzprix/yarmi&amp;utm_campaign=Badge_Grade)
![Travis Badge](https://travis-ci.com/fritzprix/yarmi.svg?branch=master)
### Features
1. Zero configuration for service integration based on RMI and Service Discovery
1. Simple APIs
>  discover and request service with just a few API calls
2. Support large blob as method parameter or response
> yarmi supports blob exchange between client and server by default with BlobSession which exposes familiar read / write APIs    



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
            <groupId>net.doodream</groupId>
            <artifactId>yarmi-core</artifactId>
            <version>0.1.1</version>
        </dependency>
            <dependency>
                <groupId>net.doodream.yarmi</groupId>
                <artifactId>sdp-mdns</artifactId>
                <version>0.1.1</version>
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
    implementation 'net.doodream:yarmi-core:0.1.1'
    implementation 'net.doodream.yarmi:sdp-mdns:0.1.1'
...
}
```

#### Build Service (Server)
1. Declare controller stubs with RMIExpose annotation 
```java
public interface TestController {

    @RMIExpose
    Response<String> echo(String message);
} 
```     
2. Implement Controller 
```java
public class TestControllerImpl implements TestController {


    @Override
    public Response<String> echo(String message) {
        return Response.success(message);
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

    @Controller(path = "/test", version = 1, module = TestControllerImpl.class)
    TestController controller;
}

```   
4. Start service & advertise it 
```java
public static class SimpleServer {
    
    public static void main (String[] args) {
        RMIService service = RMIService.create(TestService.class);
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
        final ServiceDiscovery discovery = MDnsServiceDiscovery.create();
        discovery.start(TestService.class, new ServiceDiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                // discovery started
            }

            @Override
            public void onServiceDiscovered(RMIServiceInfo service) {
                // new service discovered
                Object client = RMIClient.create(servce, TestService.class, new Class[] {
                        TestController.class
                });
                // cast client proxy into interface of interest
                TestController controller = (TestController) client;
                // and use it 
                Response<String> response = controller.echo("Hello");
                if(response.isSucessful()) {
                    // successfully RMI handled and response received successfully
                    System.out.println(response.getBody());
                }
            }

            @Override
            public void onDiscoveryFinished(int i, Throwable throwable) {
                // service discovery finished
            }
        });
    }
}
```

### License
> Apache License, Version 2.0
