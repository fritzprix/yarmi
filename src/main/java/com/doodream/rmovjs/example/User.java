package com.doodream.rmovjs.example;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class User {
    long id;
    String name;
    int age;
}
