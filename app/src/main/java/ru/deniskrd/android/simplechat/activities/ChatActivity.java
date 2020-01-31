package ru.deniskrd.android.simplechat.activities;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import ru.deniskrd.android.simplechat.R;
import ru.deniskrd.android.simplechat.constants.IntentConstants;
import ru.deniskrd.android.simplechat.model.Message;

public class ChatActivity extends AppCompatActivity {

    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.chat);

        setContentView(R.layout.activity_chat);

        this.groupId = getIntent().getExtras().getString(IntentConstants.GROUP_ID.name());

        RelativeLayout mainLayout = findViewById(R.id.activity_chat);
        EmojiconEditText editText = findViewById(R.id.message_edit_text);
        ImageView emojiButton = findViewById(R.id.smile_button);

        EmojIconActions emojIconActions = new EmojIconActions(getApplicationContext(), mainLayout, editText, emojiButton);
        emojIconActions.ShowEmojIcon();

        displayAllMessages();
    }

    public void sendMessage(View view) {
        EditText messageText = findViewById(R.id.message_edit_text);
        if (messageText.getText().toString().isEmpty()) {
            return;
        }

        FirebaseDatabase.getInstance().getReference().child("messages").push().setValue(
                new Message(FirebaseAuth.getInstance().getCurrentUser().getEmail(), messageText.getText().toString(), this.groupId));
        messageText.setText("");

        displayAllMessages();
    }

    private void displayAllMessages() {
        FirebaseListAdapter<Message> adapter = new FirebaseListAdapter<Message>(this, Message.class, R.layout.message_list_item,
                FirebaseDatabase.getInstance().getReference().child("messages").orderByChild("groupId").equalTo(groupId)) {
            @Override
            protected void populateView(View v, Message model, int position) {
                TextView messageUser = v.findViewById(R.id.message_user);
                TextView messageTime = v.findViewById(R.id.message_time);
                TextView messageText = v.findViewById(R.id.message_text);

                messageUser.setText(model.getUserName());
                messageText.setText(String.valueOf(model.getTextMessage()));
                messageTime.setText(DateFormat.format("dd-MM-yyyy HH:mm:ss", model.getMessageTime()));
            }
        };

        ListView listView = findViewById(R.id.list_of_messages);
        listView.setAdapter(adapter);
    }
}
