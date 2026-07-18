package com.openstrata.srs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** OpenStrata SRS service configuration (SPECS §3 / DESIGN §10). */
@ConfigurationProperties(prefix = "openstrata")
public class OpenstrataProperties {

    private Service service = new Service();
    private Features features = new Features();
    private Spi spi = new Spi();

    public static class Service {
        private int port = 8083;
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

    public static class Features {
        private Srs srs = new Srs();
        private SkillTest skillTest = new SkillTest();
        private RuleEngine ruleEngine = new RuleEngine();
        private DevMode devMode = new DevMode();

        public Srs getSrs() { return srs; }
        public void setSrs(Srs v) { this.srs = v; }
        public SkillTest getSkillTest() { return skillTest; }
        public void setSkillTest(SkillTest v) { this.skillTest = v; }
        public RuleEngine getRuleEngine() { return ruleEngine; }
        public void setRuleEngine(RuleEngine v) { this.ruleEngine = v; }
        public DevMode getDevMode() { return devMode; }
        public void setDevMode(DevMode v) { this.devMode = v; }

        public static class Srs {
            private boolean enabled = false;
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
        }

        public static class SkillTest {
            private boolean enabled = true;
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
        }

        public static class RuleEngine {
            private Engine opa = new Engine(true);
            private Engine drools = new Engine(false);
            public Engine getOpa() { return opa; }
            public void setOpa(Engine v) { this.opa = v; }
            public Engine getDrools() { return drools; }
            public void setDrools(Engine v) { this.drools = v; }

            public static class Engine {
                private boolean enabled;
                public Engine() {}
                public Engine(boolean enabled) { this.enabled = enabled; }
                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean v) { this.enabled = v; }
            }
        }

        public static class DevMode {
            private boolean enabled = false;
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
        }
    }

    public static class Spi {
        private Provider auth = new Provider("keycloak");
        private Provider cache = new Provider("redis");
        private ObjectStore objectStore = new ObjectStore();

        public Provider getAuth() { return auth; }
        public void setAuth(Provider v) { this.auth = v; }
        public Provider getCache() { return cache; }
        public void setCache(Provider v) { this.cache = v; }
        public ObjectStore getObjectStore() { return objectStore; }
        public void setObjectStore(ObjectStore v) { this.objectStore = v; }

        public static class Provider {
            private String provider;
            public Provider() {}
            public Provider(String provider) { this.provider = provider; }
            public String getProvider() { return provider; }
            public void setProvider(String v) { this.provider = v; }
        }

        public static class ObjectStore {
            private boolean enabled = false;
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
        }
    }

    public Service getService() { return service; }
    public void setService(Service v) { this.service = v; }
    public Features getFeatures() { return features; }
    public void setFeatures(Features v) { this.features = v; }
    public Spi getSpi() { return spi; }
    public void setSpi(Spi v) { this.spi = v; }
}
