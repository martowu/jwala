package com.siemens.cto.aem.common.properties;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.siemens.cto.aem.common.AemConstants;
import com.siemens.cto.aem.common.ApplicationException;

public class ApplicationProperties {

    private volatile Properties properties;

    private static volatile ApplicationProperties SELF;

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationProperties.class);

    public static ApplicationProperties getInstance() {
        if (SELF == null) {
            synchronized (ApplicationProperties.class) {
                if (SELF == null) {
                    SELF = new ApplicationProperties();
                }
            }
        }

        return SELF;
    }

    public static Properties getProperties() {
        final Properties copy = new Properties();
        copy.putAll(getInstance().properties);
        return copy;
    }

    public static void reload() {
        getInstance().init();
    }

    public static String get(String key) {
        return getProperties().getProperty(key);
    }

    public static Integer getAsInteger(String key) {
        return Integer.parseInt(getProperties().getProperty(key));
    }

    public static Boolean getAsBoolean(String key) {
        return Boolean.parseBoolean(getProperties().getProperty(key));
    }

    public static int size() {
        return getProperties().size();
    }

    private ApplicationProperties() {
        properties = new Properties();
        init();
    }

    private void init() {
        String propertiesFile = System.getProperty(AemConstants.PROPERTIES_ROOT_PATH) + "/" + AemConstants.PROPERTIES_FILE_NAME;
        Properties tempProperties = new Properties();
        try {
            tempProperties.load(new FileReader(new File(propertiesFile)));
        } catch (IOException e) {
            throw new ApplicationException("Failed to load properties file " + propertiesFile, e);
        }
        properties = tempProperties;
        LOG.info("Properties loaded from path " + propertiesFile);
    }
}
