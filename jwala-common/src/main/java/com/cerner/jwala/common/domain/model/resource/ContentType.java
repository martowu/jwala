package com.cerner.jwala.common.domain.model.resource;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonValue;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * Content type enumeration e.g. application/xml etc...
 * Note: Maybe a better alternative is to use http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/net/MediaType.html.
 *       Anyways this will do for now.
 *
 * Created by JC043760 on 3/31/2016.
 */
@JsonDeserialize(using = ContentTypeStrDeserializer.class)
public enum ContentType {
    XML_UTF_8("text/xml"), PLAIN_TEXT_UTF_8("text/plain"), APPLICATION_BINARY("application/binary"), UNDEFINED;

    public final String contentTypeStr;

    ContentType() {
        this.contentTypeStr = StringUtils.EMPTY;
    }

    ContentType(final String contentTypeStr) {
        this.contentTypeStr = contentTypeStr;
    }

    @JsonValue
    public String getContentTypeStr() {
        return contentTypeStr;
    }

    /**
     * Convert's a String content type to {@link ContentType} enum.
     * @param contentTypeStr the content type in string format e.g. text/plain
     * @return {@link ContentType} if contentTypeStr has a match, null if there's none
     */
    public static ContentType fromContentTypeStr(final String contentTypeStr) {
        for (final ContentType contentType: ContentType.values()) {
            if (contentType.contentTypeStr.equalsIgnoreCase(contentTypeStr)) {
                return contentType;
            }
        }
        return UNDEFINED;
    }
}