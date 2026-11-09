package com.epam.module2.avroconsumer.service;

import com.epam.module2.avro.Message;

/**
 * @param schemaId      ID of the schema the message was written with, from bytes 1 to 4 of the payload
 * @param schemaVersion version of that schema under the topic's subject in Schema Registry
 * @param message       the message, resolved to the consumer's reader schema
 */
public record DecodedMessage(int schemaId, int schemaVersion, Message message) {
}
