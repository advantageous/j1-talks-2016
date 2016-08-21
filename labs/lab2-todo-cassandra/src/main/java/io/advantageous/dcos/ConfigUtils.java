package io.advantageous.dcos;

import io.advantageous.config.Config;
import io.advantageous.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Convenient utility methods for handling env and config.
 *
 * @author Geoff Chandler
 * @author Rick Hightower
 */
public final class ConfigUtils {

    private static final String DEPLOYMENT_ENVIRONMENT;
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    private static AtomicReference<Config> rootConfig = new AtomicReference<>();

    static {
        final String env = System.getenv("DEPLOYMENT_ENVIRONMENT");
        DEPLOYMENT_ENVIRONMENT = env == null ? "development" : env.toLowerCase();
    }

    private ConfigUtils() {
        throw new IllegalStateException("config utils is not to be instantiated.");
    }

    private static Config loadRootConfig() {
        final List<Config> configs = new ArrayList<>();

        final String resourceName = String.format("%s-config.js", DEPLOYMENT_ENVIRONMENT);
        configs.add(ConfigLoader.load("mesos-docker-utils.js", resourceName));

        /*
        This is the fallback config.  Order here matters.
        If we add this to the array first, your env would not
        override the default.
         */
        try {
            configs.add(ConfigLoader.load("default-config.js"));
        } catch (IllegalArgumentException ignore) {
        }

        final Config config = ConfigLoader.configWithFallbacks(configs.toArray(new Config[configs.size()]));
        if (!rootConfig.compareAndSet(null, config)) {
            logger.warn("Config was set, and and you can't overwrite it. {}", resourceName);
        }
        return config;
    }

    public static Config getConfig(final String basePath) {
        if (rootConfig.get() == null) {
            loadRootConfig();
        }
        return rootConfig.get().getConfig(basePath);
    }

}
