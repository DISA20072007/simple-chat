package ru.deniskrd.android.simplechat.model;

import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Message {

    private String userName;
    private String textMessage;
    private long messageTime;
    private String groupId;

    public Message(String userName, String textMessage, String groupId) {
        this.userName = userName;
        this.textMessage = textMessage;
        this.groupId = groupId;

        this.messageTime = new Date().getTime();
    }
}
