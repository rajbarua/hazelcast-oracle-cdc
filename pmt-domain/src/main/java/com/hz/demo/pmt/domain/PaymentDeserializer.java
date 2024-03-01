package com.hz.demo.pmt.domain;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PaymentDeserializer implements Deserializer<Payment>{
        
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Desrialiser for Kafka
     */
    @Override
    public Payment deserialize(String topic, byte[] data) {
        //use fasterxml to deserialise
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.readValue(data, Payment.class);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
