package net.doodream.yarmi;


public class Properties {

    private static final java.util.Properties PROPERTIES;
    static {
        PROPERTIES = new java.util.Properties();
        try {
            PROPERTIES.load(Properties.class.getClassLoader().getResourceAsStream("project.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getArtifactId() {
        return PROPERTIES.getProperty("artifactId", "yarmi-core");
    }

    public static String getVersionString() {
        return PROPERTIES.getProperty("version", "0.0.1");
    }

    public static int getDiscoveryTTL(int defaultMulticastTtl) {
        try {
            return Integer.valueOf(PROPERTIES.getProperty("sdp.multicast.ttl", String.valueOf(defaultMulticastTtl)));
        } catch (Exception e) {
            return defaultMulticastTtl;
        }
    }

    public static int getDiscoveryPort(int broadcastPort) {
        try {
            return Integer.valueOf(PROPERTIES.getProperty("sdp.multicast.port", String.valueOf(broadcastPort)));
        } catch (Exception e) {
            return broadcastPort;
        }
    }

    public static String getDiscoveryAddress(String multicastGroupIp) {
        return PROPERTIES.getProperty("sdp.multicast.address", multicastGroupIp);
    }

    public static long getDiscoveryTimeoutInMills() {
        try {
            return Long.valueOf(PROPERTIES.getProperty("sdp.muticast.discovery.timeout", "5000L"));
        } catch (Exception e) {
            return 5000L;
        }
    }

    public static int getRegistryDelegationPort(int defaultServiceRegistryDelegationPort) {
        try {
            return Integer.valueOf(PROPERTIES.getProperty("sdp.muticast.delegation.port", "5000L"));
        } catch (Exception e) {
            return defaultServiceRegistryDelegationPort;
        }
    }
}
