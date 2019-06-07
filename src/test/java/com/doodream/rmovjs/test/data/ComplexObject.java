package com.doodream.rmovjs.test.data;

import com.google.gson.annotations.SerializedName;

import java.util.*;

public class ComplexObject {

    public static class Builder {
        final ComplexObject complexObject = new ComplexObject();
        private Builder() {

        }

        public Builder user(User user) {
            complexObject.user = user;
            return this;
        }

        public Builder addressBook(Map<String, User> addressBook) {
            complexObject.addressBook = addressBook;
            return this;
        }

        public ComplexObject build() {
            return complexObject;
        }

        public Builder objects(List<ComplexObject> complexObjects) {
            complexObject.objects = complexObjects;
            return this;
        }

        public Builder friend(Set<User> users) {
            complexObject.friend = users;
            return this;
        }
    }

    @SerializedName("user")
    private User user;

    @SerializedName("friends")
    private Set<User> friend;

    @SerializedName("others")
    private List<ComplexObject> objects;

    @SerializedName("address")
    private Map<String, User> addressBook;

    public static ComplexObject createTestObject() {
        Map<String, User> addressMap = new HashMap<>();
        addressMap.put("david", User.builder()
                .name("david")
                .age(38)
                .build());

        Set<User> users = new HashSet<>();
        users.add(User.builder()
                .name("jane")
                .age(22)
                .build());

        List<ComplexObject> complexObjects = Arrays.asList(
                ComplexObject.builder()
                        .user(User.builder()
                                .name("david")
                                .age(32)
                                .build())
                        .addressBook(addressMap)
                        .build()
        );


        return ComplexObject.builder()
                .addressBook(addressMap)
                .objects(complexObjects)
                .user(User.builder()
                        .name("james")
                        .age(23)
                        .build())
                .friend(users)
                .build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, addressBook, friend, objects);
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    private static Builder builder() {
        return new Builder();
    }
}
