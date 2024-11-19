package com.hz.demo.cdc.domain;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer implements Serializable {
    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;

    public Customer() {
    }

    public Customer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

}
