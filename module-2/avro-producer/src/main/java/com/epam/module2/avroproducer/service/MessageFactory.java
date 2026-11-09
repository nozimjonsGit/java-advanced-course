package com.epam.module2.avroproducer.service;

import com.epam.module2.avroproducer.schema.MessageSchemas;
import com.epam.module2.avroproducer.schema.SchemaVersion;
import java.time.Clock;
import java.util.UUID;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.springframework.stereotype.Component;

/**
 * Builds messages as generic records, because the producer writes two versions of a schema
 * that share one name, and a generated class can only represent one of them.
 */
@Component
public class MessageFactory {

    static final String SENDER_FIELD = "sender";

    private final MessageSchemas schemas;
    private final Clock clock;

    public MessageFactory(MessageSchemas schemas, Clock clock) {
        this.schemas = schemas;
        this.clock = clock;
    }

    /**
     * @param sender ignored by schemas without a sender field
     */
    public GenericRecord create(SchemaVersion version, String text, String sender) {
        Schema schema = schemas.get(version);
        GenericRecordBuilder builder = new GenericRecordBuilder(schema)
                .set("id", UUID.randomUUID().toString())
                .set("text", text)
                .set("sentAt", clock.millis());
        if (schema.getField(SENDER_FIELD) != null) {
            builder.set(SENDER_FIELD, sender);
        }
        return builder.build();
    }
}
