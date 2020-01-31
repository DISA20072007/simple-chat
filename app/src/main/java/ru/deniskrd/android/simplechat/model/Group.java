package ru.deniskrd.android.simplechat.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Group {

    private String id;
    private String name;
    private String admin;
    private boolean isPrivate;
    private List<User> members = new ArrayList<>();

    public List<User> getMembers() {
        if (members == null) {
            members = new ArrayList<>();
        }
        return members;
    }
}
