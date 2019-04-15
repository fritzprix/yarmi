package com.doodream.rmovjs.test.service;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplexObject {

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
}
