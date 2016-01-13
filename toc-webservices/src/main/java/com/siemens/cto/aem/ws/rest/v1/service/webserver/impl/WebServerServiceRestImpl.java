package com.siemens.cto.aem.ws.rest.v1.service.webserver.impl;

import com.siemens.cto.aem.common.domain.model.fault.AemFaultType;
import com.siemens.cto.aem.common.domain.model.group.Group;
import com.siemens.cto.aem.common.domain.model.id.Identifier;
import com.siemens.cto.aem.common.domain.model.resource.ResourceType;
import com.siemens.cto.aem.common.domain.model.state.CurrentState;
import com.siemens.cto.aem.common.domain.model.webserver.WebServer;
import com.siemens.cto.aem.common.domain.model.webserver.WebServerReachableState;
import com.siemens.cto.aem.common.exception.FaultCodeException;
import com.siemens.cto.aem.common.exception.InternalErrorException;
import com.siemens.cto.aem.common.exec.CommandOutput;
import com.siemens.cto.aem.common.properties.ApplicationProperties;
import com.siemens.cto.aem.common.request.jvm.UploadJvmTemplateRequest;
import com.siemens.cto.aem.common.request.webserver.ControlWebServerRequest;
import com.siemens.cto.aem.common.request.webserver.UploadHttpdConfTemplateRequest;
import com.siemens.cto.aem.common.request.webserver.UploadWebServerTemplateRequest;
import com.siemens.cto.aem.exception.CommandFailureException;
import com.siemens.cto.aem.persistence.jpa.service.exception.NonRetrievableResourceTemplateContentException;
import com.siemens.cto.aem.persistence.jpa.service.exception.ResourceTemplateUpdateException;
import com.siemens.cto.aem.service.resource.ResourceService;
import com.siemens.cto.aem.service.state.StateService;
import com.siemens.cto.aem.service.webserver.WebServerCommandService;
import com.siemens.cto.aem.service.webserver.WebServerControlService;
import com.siemens.cto.aem.service.webserver.WebServerService;
import com.siemens.cto.aem.ws.rest.v1.provider.AuthenticatedUser;
import com.siemens.cto.aem.ws.rest.v1.provider.WebServerIdsParameterProvider;
import com.siemens.cto.aem.ws.rest.v1.response.ResponseBuilder;
import com.siemens.cto.aem.ws.rest.v1.service.webserver.WebServerServiceRest;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WebServerServiceRestImpl implements WebServerServiceRest {

    private static final Logger logger = LoggerFactory.getLogger(WebServerServiceRestImpl.class);
    public static final String STP_HTTPD_DATA_DIR = "paths.httpd.conf";

    private final WebServerService webServerService;
    private final WebServerControlService webServerControlService;
    private final WebServerCommandService webServerCommandService;
    private final StateService<WebServer, WebServerReachableState> webServerStateService;
    private final Map<String, ReentrantReadWriteLock> wsWriteLocks;
    private ResourceService resourceService;
    private static WebServerServiceRestImpl instance;

    public WebServerServiceRestImpl(final WebServerService theWebServerService,
                                    final WebServerControlService theWebServerControlService,
                                    final WebServerCommandService theWebServerCommandService,
                                    final StateService<WebServer, WebServerReachableState> theWebServerStateService,
                                    final Map<String, ReentrantReadWriteLock> theWriteLocks,
                                    final ResourceService theResourceService) {
        webServerService = theWebServerService;
        webServerControlService = theWebServerControlService;
        webServerCommandService = theWebServerCommandService;
        webServerStateService = theWebServerStateService;
        wsWriteLocks = theWriteLocks;
        resourceService = theResourceService;
    }

    @Override
    public Response getWebServers(final Identifier<Group> aGroupId) {
        final List<WebServer> webServers;
        if (aGroupId == null) {
            webServers = webServerService.getWebServers();
            return ResponseBuilder.ok(webServers);
        }
        webServers = webServerService.findWebServers(aGroupId);
        return ResponseBuilder.ok(webServers);
    }

    @Override
    public Response getWebServer(final Identifier<WebServer> aWsId) {
        logger.debug("Get WS requested: {}", aWsId);
        return ResponseBuilder.ok(webServerService.getWebServer(aWsId));
    }

    @Override
    public Response createWebServer(final JsonCreateWebServer aWebServerToCreate, final AuthenticatedUser aUser) {
        logger.debug("Create WS requested: {}", aWebServerToCreate);

        final WebServer webServer = webServerService.createWebServer(aWebServerToCreate.toCreateWebServerRequest(),
                aUser.getUser());

        // upload the default resource template for the newly created web server
        uploadWebServerResourceTemplates(aUser, webServer);

        return ResponseBuilder.created(webServer);
    }

    private void uploadWebServerResourceTemplates(AuthenticatedUser aUser, WebServer webServer) {
        for (final ResourceType resourceType : resourceService.getResourceTypes()) {
            if ("webServer".equals(resourceType.getEntityType())) {
                FileInputStream dataInputStream = null;
                try {
                    dataInputStream =
                            new FileInputStream(new File(ApplicationProperties.get("paths.resource-types") + "/"
                                    + resourceType.getTemplateName()));
                    UploadWebServerTemplateRequest uploadWebServerTemplateRequest =
                            new UploadWebServerTemplateRequest(webServer, resourceType.getTemplateName(), dataInputStream) {
                                @Override
                                public String getConfFileName() {
                                    return resourceType.getConfigFileName();
                                }
                            };
                    webServerService.uploadWebServerConfig(uploadWebServerTemplateRequest, aUser.getUser());
                } catch (FileNotFoundException e) {
                    logger.error("Could not find template {} for new webserver {}", resourceType.getConfigFileName(),
                            webServer.getName(), e);
                    throw new InternalErrorException(AemFaultType.WEB_SERVER_HTTPD_CONF_TEMPLATE_NOT_FOUND, "Could not find template "
                            + resourceType.getTemplateName());
                }
            }
        }
    }

    @Override
    public Response updateWebServer(final JsonUpdateWebServer aWebServerToCreate, final AuthenticatedUser aUser) {
        logger.debug("Update WS requested: {}", aWebServerToCreate);
        return ResponseBuilder.ok(webServerService.updateWebServer(aWebServerToCreate.toUpdateWebServerRequest(),
                aUser.getUser()));
    }

    @Override
    public Response removeWebServer(final Identifier<WebServer> aWsId) {
        logger.debug("Delete WS requested: {}", aWsId);
        webServerService.removeWebServer(aWsId);
        return ResponseBuilder.ok();
    }

    @Override
    public Response controlWebServer(final Identifier<WebServer> aWebServerId,
                                     final JsonControlWebServer aWebServerToControl, final AuthenticatedUser aUser) {
        logger.debug("Control Web Server requested: {} {}", aWebServerId, aWebServerToControl);
        final CommandOutput commandOutput = webServerControlService.controlWebServer(
                new ControlWebServerRequest(aWebServerId, aWebServerToControl.toControlOperation()),
                aUser.getUser());
        if (commandOutput.getReturnCode().wasSuccessful()) {
            return ResponseBuilder.ok(commandOutput.getStandardOutput());
        } else {
            throw new InternalErrorException(AemFaultType.CONTROL_OPERATION_UNSUCCESSFUL, commandOutput.getStandardError());
        }
    }

    @Override
    public Response generateConfig(final String aWebServerName, final Boolean withSsl) {
        String httpdConfStr = generateHttpdConfText(aWebServerName, withSsl);
        return Response.ok(httpdConfStr).build();
    }

    @Override
    public Response generateAndDeployConfig(final String aWebServerName) {

        // only one at a time per web server
        if (!wsWriteLocks.containsKey(aWebServerName)) {
            wsWriteLocks.put(aWebServerName, new ReentrantReadWriteLock());
        }

        wsWriteLocks.get(aWebServerName).writeLock().lock();

        try {
            // create the file
            final String httpdDataDir = ApplicationProperties.get(STP_HTTPD_DATA_DIR);
            final File httpdConfFile = createTempHttpdConf(aWebServerName, httpdDataDir);

            // copy the file
            final CommandOutput execData;
            final String httpdUnixPath = httpdConfFile.getAbsolutePath().replace("\\", "/");

            execData = webServerControlService.secureCopyHttpdConf(aWebServerName, httpdUnixPath, httpdDataDir + "/httpd.conf");
            if (execData.getReturnCode().wasSuccessful()) {
                logger.info("Copy of httpd.conf successful: {}", httpdUnixPath);
            } else {
                String standardError =
                        execData.getStandardError().isEmpty() ? execData.getStandardOutput() : execData
                                .getStandardError();
                logger.error("Copy command completed with error trying to copy httpd.conf to {} :: ERROR: {}",
                        aWebServerName, standardError);
                Map<String, String> errorDetails = new HashMap<>();
                errorDetails.put("webServerName", aWebServerName);
                return ResponseBuilder.notOkWithDetails(Response.Status.INTERNAL_SERVER_ERROR, new FaultCodeException(
                        AemFaultType.REMOTE_COMMAND_FAILURE, standardError), errorDetails);
            }
        } catch (CommandFailureException e) {
            logger.error("Failed to copy the httpd.conf to {} :: ERROR: {}", aWebServerName, e);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("webServerName", aWebServerName);
            return ResponseBuilder.notOkWithDetails(Response.Status.INTERNAL_SERVER_ERROR, new FaultCodeException(
                    AemFaultType.REMOTE_COMMAND_FAILURE, "Failed to copy httpd.conf"), errorDetails);
        } finally {
            wsWriteLocks.get(aWebServerName).writeLock().unlock();
        }
        return ResponseBuilder.ok(webServerService.getWebServer(aWebServerName));
    }

    private File createTempHttpdConf(String aWebServerName, String httpdDataDir) {
        PrintWriter out = null;
        final File httpdConfFile =
                new File((httpdDataDir + System.getProperty("file.separator") + aWebServerName + "_httpd."
                        + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".conf").replace("\\", "/"));
        final String httpdConfAbsolutePath = httpdConfFile.getAbsolutePath().replace("\\", "/");
        try {
            out = new PrintWriter(httpdConfAbsolutePath);
            final boolean useSSL = true;
            out.println(generateHttpdConfText(aWebServerName, useSSL));
        } catch (FileNotFoundException e) {
            logger.error("Unable to create temporary file {}", httpdConfAbsolutePath);
            throw new InternalErrorException(AemFaultType.INVALID_PATH, e.getMessage(), e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return httpdConfFile;
    }

    private String generateHttpdConfText(String aWebServerName, Boolean withSsl) {
        return webServerService.generateHttpdConfig(aWebServerName, withSsl);
    }

    @Override
    public Response generateLoadBalancerConfig(final String aWebServerName) {
        return Response.ok(webServerService.generateWorkerProperties(aWebServerName)).build();
    }

    @Override
    public Response getCurrentWebServerStates(final WebServerIdsParameterProvider webServerIdsParameterProvider) {
        logger.debug("Current WebServer states requested : {}", webServerIdsParameterProvider);
        final Set<Identifier<WebServer>> webServerIds = webServerIdsParameterProvider.valueOf();
        final Set<CurrentState<WebServer, WebServerReachableState>> currentWebServerStates;

        if (webServerIds.isEmpty()) {
            currentWebServerStates = webServerStateService.getCurrentStates();
        } else {
            currentWebServerStates = webServerStateService.getCurrentStates(webServerIds);
        }

        return ResponseBuilder.ok(currentWebServerStates);
    }

    @Override
    public Response getHttpdConfig(Identifier<WebServer> aWebServerId) {
        try {
            return Response.ok(webServerCommandService.getHttpdConf(aWebServerId)).build();
        } catch (CommandFailureException cmdFailEx) {
            logger.warn("Request Failure Occurred", cmdFailEx);
            return ResponseBuilder.notOk(Response.Status.INTERNAL_SERVER_ERROR, new FaultCodeException(
                    AemFaultType.REMOTE_COMMAND_FAILURE, cmdFailEx.getMessage()));
        }
    }

    @Override
    public Response getResourceNames(final String wsName) {
        return ResponseBuilder.ok(webServerService.getResourceTemplateNames(wsName));
    }

    @Context
    private MessageContext context;

    /*
     * access point for unit testing
     */
    void setMessageContext(MessageContext aContextForTesting) {
        context = aContextForTesting;
    }

    @Override
    public Response uploadConfigTemplate(String webServerName, AuthenticatedUser aUser, String templateName) {
        logger.debug("Upload Archive requested: {} streaming (no size, count yet)", webServerName);

        // iframe uploads from IE do not understand application/json
        // as a response and will prompt for download. Fix: return
        // text/html
        if (!context.getHttpHeaders().getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE)) {
            context.getHttpServletResponse().setContentType(MediaType.TEXT_HTML);
        }

        WebServer webServer = webServerService.getWebServer(webServerName);
        if (null == webServer) {
            throw new InternalErrorException(AemFaultType.JVM_NOT_FOUND, "Could not find web server with name "
                    + webServerName);
        }

        ServletFileUpload sfu = new ServletFileUpload();
        InputStream data = null;
        try {
            FileItemIterator iter = sfu.getItemIterator(context.getHttpServletRequest());
            FileItemStream file1;

            while (iter.hasNext()) {
                file1 = iter.next();
                try {
                    data = file1.openStream();
                    UploadWebServerTemplateRequest request =
                            new UploadHttpdConfTemplateRequest(webServer, file1.getName(), data);

                    return ResponseBuilder.created(webServerService.uploadWebServerConfig(request, aUser.getUser())); // early
                    // out
                    // on
                    // first
                    // attachment
                } finally {
                    assert data != null;
                    data.close();
                }
            }
            return ResponseBuilder.notOk(Response.Status.NO_CONTENT, new FaultCodeException(
                    AemFaultType.INVALID_WEBSERVER_OPERATION, "No data"));
        } catch (IOException | FileUploadException e) {
            throw new InternalErrorException(AemFaultType.BAD_STREAM, "Error receiving data", e);
        }
    }

    @Override
    public Response getResourceTemplate(final String wsName, final String resourceTemplateName,
                                        final boolean tokensReplaced) {
        return ResponseBuilder.ok(webServerService.getResourceTemplate(wsName, resourceTemplateName, tokensReplaced));
    }

    @Override
    public Response updateResourceTemplate(final String wsName, final String resourceTemplateName, final String content) {

        try {
            return ResponseBuilder.ok(webServerService.updateResourceTemplate(wsName, resourceTemplateName, content));
        } catch (ResourceTemplateUpdateException | NonRetrievableResourceTemplateContentException e) {
            logger.debug("Failed to update resource template {}", resourceTemplateName, e);
            return ResponseBuilder.notOk(Response.Status.INTERNAL_SERVER_ERROR, new FaultCodeException(
                    AemFaultType.PERSISTENCE_ERROR, e.getMessage()));
        }

    }

    @Override
    public Response previewResourceTemplate(final String webServerName, final String groupName, String template) {
        try {
            return ResponseBuilder.ok(webServerService.previewResourceTemplate(webServerName, groupName, template));
        } catch (RuntimeException rte) {
            logger.debug("Error previewing template.", rte);
            return ResponseBuilder.notOk(Response.Status.INTERNAL_SERVER_ERROR, new FaultCodeException(
                    AemFaultType.INVALID_TEMPLATE, rte.getMessage()));
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        instance = this;
    }

    public static WebServerServiceRestImpl get(){
        return instance;
    }
}
