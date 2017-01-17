package com.cerner.jwala.common.properties;

/**
 * Created by Steven Ger on 12/21/16.
 */
public enum PropertyKeys {

    REMOTE_JAWALA_DATA_DIR("remote.jwala.data.dir"),
    APACHE_HTTPD_FILE_NAME("jwala.apache.httpd.zip.name"),
    REMOTE_PATHS_APACHE_HTTPD("remote.paths.apache.httpd"),
    REMOTE_PATHS_HTTPD_ROOT_DIR_NAME("remote.paths.httpd.root.dir.name"),
    SCRIPTS_PATH("commands.scripts-path"),
    REMOTE_TOMCAT_DIR_NAME("remote.tomcat.dir.name"),
    REMOTE_PATH_INSTANCES_DIR("remote.paths.instances"),
    REMOTE_PATHS_DEPLOY_DIR("remote.paths.deploy.dir"),
    REMOTE_SCRIPT_DIR("remote.commands.user-scripts"),
    REMOTE_JAVA_HOME("remote.jwala.java.home"),
    REMOTE_JWALA_JAVA_ROOT_DIR_NAME("remote.jwala.java.root.dir.name"),
    REMOTE_PATHS_TOMCAT_ROOT_CORE("remote.paths.tomcat.root.core"),
    REMOTE_PATHS_TOMCAT_CORE("remote.paths.tomcat.core"),
    LOCAL_JWALA_BINARY_DIR("jwala.binary.dir"),
    JMAP_DUMP_LIVE_ENABLED("jmap.dump.live.enabled");

    private String propertyName;

    PropertyKeys(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}