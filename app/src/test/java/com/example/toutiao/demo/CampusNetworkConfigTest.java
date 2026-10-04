package com.example.toutiao.demo;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
public class CampusNetworkConfigTest {
    @Test public void acceptsEmulatorLanAndHttpsOrigins() {
        assertEquals("http://10.0.2.2:5000", CampusNetworkConfig.normalize(" http://10.0.2.2:5000/ "));
        assertEquals("http://192.168.1.8:5000", CampusNetworkConfig.normalize("192.168.1.8:5000"));
        assertEquals("https://news.example.com", CampusNetworkConfig.normalize("https://news.example.com/"));
    }
    @Test public void rejectsPathsCredentialsAndInvalidPorts() {
        for (String value : new String[]{"", "ftp://192.168.1.8", "http://a:70000", "http://a:0",
                "http://user:password@a", "http://a/api/news", "http://a?x=1", "http://a#x", "hello world"}) {
            try {CampusNetworkConfig.normalize(value);fail("Accepted " + value);}
            catch (IllegalArgumentException expected) { }
        }
    }
}
