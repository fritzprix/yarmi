package com.doodream.rmovjs.test.data;

import com.doodream.rmovjs.model.ControllerInfo;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class User {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        final User user = new User();

        private Builder() {
        }

        public Builder name(String name) {
            user.name = name;
            return this;
        }

        public Builder age(int age) {
            user.age = age;
            return this;
        }

        public User build() {
            return user;
        }
    }

    @SerializedName("name")
    String name;

    @SerializedName("age")
    int age;

    public void setAge(int age) {
        this.age = age;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(age, name);
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }
}