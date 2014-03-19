package org.motechproject.mtraining.domain;

import org.codehaus.jackson.annotate.JsonProperty;
import org.ektorp.support.TypeDiscriminator;

import java.util.UUID;

/**
 * Couch document object representing a Message.
 * Message
 *   + name    : Message name
 *   + description : message description
 *   + contentId   : UUID that for a message (different from the _id generated by couch)
 *   + version     : message version (a message can have multiple versions, different versions of the message will have same contentId)
 *   + externalContentId  : Id that points to an external file or resource that is associated with the message.For eg. an audio file that is played to the enrollee
 *   + createdBy    : Author of the message
 *   + createdOn    : Date on which message was created
 */
@TypeDiscriminator("doc.type === 'Message'")
public class Message extends Content {

    @JsonProperty
    private String name;

    @JsonProperty
    private String description;

    Message() {
    }

    public Message(boolean isActive, String name, String description, String externalId, String createdBy) {
        super(isActive, externalId, createdBy);
        this.name = name;
        this.description = description;
    }

    public Message(UUID contentId, Integer version, boolean isActive, String name, String description, String externalId, String createdBy) {
        super(contentId, version, isActive, externalId, createdBy);
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
