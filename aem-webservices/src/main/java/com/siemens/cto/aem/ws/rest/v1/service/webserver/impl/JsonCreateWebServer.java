package com.siemens.cto.aem.ws.rest.v1.service.webserver.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import com.siemens.cto.aem.common.exception.BadRequestException;
import com.siemens.cto.aem.domain.model.fault.AemFaultType;
import com.siemens.cto.aem.domain.model.group.Group;
import com.siemens.cto.aem.domain.model.id.Identifier;
import com.siemens.cto.aem.domain.model.webserver.CreateWebServerCommand;

@JsonDeserialize(using = JsonCreateWebServer.JsonCreateWebServerDeserializer.class)
public class JsonCreateWebServer {

    private String groupId;
    private String webserverName;
    private Integer portNumber;
    private String hostName;

    public JsonCreateWebServer() {
    }

    public JsonCreateWebServer(final String aGroupId,
                         final String aWebServerName,
                         final String aHostName,
                         final String aPortNumber) {
        groupId = aGroupId;
        webserverName = aWebServerName;
        hostName = aHostName;
        portNumber = Integer.parseInt(aPortNumber);
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(final String aGroupId) {
        groupId = aGroupId;
    }

    public String getWebServerName() {
        return webserverName;
    }

    public void setWebServerName(final String aWebServerName) {
        webserverName = aWebServerName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(final String aHostName) {
        hostName = aHostName;
    }

    public CreateWebServerCommand toCreateWebServerCommand() throws BadRequestException {

        try {
            return new CreateWebServerCommand(new Identifier<Group>(groupId),
                                        webserverName,
                                        hostName,
                                        portNumber);
        } catch (final NumberFormatException nfe) {
            throw new BadRequestException(AemFaultType.INVALID_IDENTIFIER,
                                          nfe.getMessage(),
                                          nfe);
        }
    }

    static class JsonCreateWebServerDeserializer extends JsonDeserializer<JsonCreateWebServer> {

        public JsonCreateWebServerDeserializer() {
        }

        @Override
        public JsonCreateWebServer deserialize(final JsonParser jp,
                                           final DeserializationContext ctxt) throws IOException, JsonProcessingException {

            final ObjectCodec obj = jp.getCodec();
            final JsonNode node = obj.readTree(jp).get(0);

            return new JsonCreateWebServer(node.get("groupId").getValueAsText(),
                                     node.get("webserverName").getTextValue(),
                                     node.get("hostName").getTextValue(),
                                     node.get("portNumber").getValueAsText()
                                     );
        }
    }

	public Integer getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(Integer portNumber) {
		this.portNumber = portNumber;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + ((portNumber == null) ? 0 : portNumber.hashCode());
        result = prime * result + ((webserverName == null) ? 0 : webserverName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonCreateWebServer other = (JsonCreateWebServer) obj;
        if (groupId == null) {
            if (other.groupId != null)
                return false;
        } else if (!groupId.equals(other.groupId))
            return false;
        if (hostName == null) {
            if (other.hostName != null)
                return false;
        } else if (!hostName.equals(other.hostName))
            return false;
        if (portNumber == null) {
            if (other.portNumber != null)
                return false;
        } else if (!portNumber.equals(other.portNumber))
            return false;
        if (webserverName == null) {
            if (other.webserverName != null)
                return false;
        } else if (!webserverName.equals(other.webserverName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "JsonCreateWebServer {groupId=" + groupId + ", webserverName=" + webserverName + ", portNumber="
                + portNumber + ", hostName=" + hostName + "}";
    }
}
