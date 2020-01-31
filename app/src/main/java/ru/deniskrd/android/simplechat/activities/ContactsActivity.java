package ru.deniskrd.android.simplechat.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import io.reactivex.disposables.Disposable;
import ru.deniskrd.android.simplechat.R;
import ru.deniskrd.android.simplechat.constants.AppConstants;
import ru.deniskrd.android.simplechat.constants.AuthConstants;
import ru.deniskrd.android.simplechat.constants.IntentConstants;
import ru.deniskrd.android.simplechat.dialogs.AddContactFragmentDialog;
import ru.deniskrd.android.simplechat.model.Group;
import ru.deniskrd.android.simplechat.model.User;
import ru.deniskrd.android.simplechat.rest.FirebaseRestService;

public class ContactsActivity extends AppCompatActivity {

    private ProgressBar progressBar;

    private String groupId;

    private Disposable subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        Intent intent = getIntent();
        if (intent.hasExtra(IntentConstants.GROUP_ID.name())) {
            this.groupId = intent.getStringExtra(IntentConstants.GROUP_ID.name());
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.contacts);

        fillContactList();

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.add_contact:
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                DialogFragment fragment = new AddContactFragmentDialog();
                fragment.show(fragmentTransaction, null);
                break;
        }

        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_user_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.delete_menu_item:
                showDeleteAlertDialog(adapterContextMenuInfo.position);
                break;
        }

        return true;
    }

    private void fillContactList() {
        FirebaseUser firebaseUser = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());

        FirebaseRecyclerAdapter<User, ListItemHolder> listAdapter = new FirebaseRecyclerAdapter<User, ListItemHolder>(User.class, R.layout.contact_list_item,
                ListItemHolder.class, FirebaseDatabase.getInstance().getReference().child("users").child(firebaseUser.getUid()).child("contacts")) {

            @Override
            protected void populateViewHolder(ListItemHolder listItemHolder, User user, int i) {
                listItemHolder.item.setText(user.getDisplayName());
            }
        };

        RecyclerView contactList = findViewById(R.id.contact_list);
        contactList.setAdapter(listAdapter);

        registerForContextMenu(contactList);

        contactList.setOnClickListener(view -> {
            User contact = listAdapter.getItem(contactList.getChildAdapterPosition(view));

            if (groupId != null) {
                Intent intent = new Intent(this, GroupsActivity.class);
                intent.putExtra(IntentConstants.MEMBER_USERNAME.name(), contact.getUserName());
                intent.putExtra(IntentConstants.GROUP_ID.name(), groupId);
                startActivity(intent);
            } else {
                showPrivateChat(contact);
            }
        });
    }

    private void showDeleteAlertDialog(final int contactIndex) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).setCancelable(true).create();
        alertDialog.setMessage(getResources().getString(R.string.delete_contact_warning));

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    final String token = getSharedPreferences(AuthConstants.AUTH_DATA.name(), Context.MODE_PRIVATE).getString(AuthConstants.KEY_AUTH_TOKEN.name(), "");

                    FirebaseUser firebaseUser = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());

                    deleteContact(token, firebaseUser.getEmail(), contactIndex);

                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    dialog.cancel();
                    break;
            }
        };

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.yes), dialogClickListener);
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.no), dialogClickListener);

        alertDialog.show();
    }

    private void deleteContact(String token, String userName, int contractIndex) {
        subscription = FirebaseRestService.getInstance().getChatUser(token, userName)
                .subscribe(user -> {
                    progressBar.setVisibility(View.VISIBLE);

                    user.getContacts().remove(contractIndex);

                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("users");

                    FirebaseUser firebaseUser = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());
                    databaseReference.child(firebaseUser.getUid()).setValue(user);

                    progressBar.setVisibility(View.GONE);
                });
    }

    private void showPrivateChat(User member) {
        SharedPreferences sharedPreferences = getSharedPreferences(AuthConstants.AUTH_DATA.name(), Context.MODE_PRIVATE);
        final String token = sharedPreferences.getString(AuthConstants.KEY_AUTH_TOKEN.name(), "");

        String privateGroupName = member.getUserName() + AppConstants.PRIVATE_GROUP_SUFFIX;

        subscription = FirebaseRestService.getInstance().getChatGroup(token, privateGroupName)
                .subscribe(group -> {
                    if (group == null) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                        group = new Group();
                        group.setName(privateGroupName);
                        group.setPrivate(true);
                        group.setAdmin(currentUser.getEmail());

                        group.getMembers().add(member);

                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("groups");

                        groupId = databaseReference.push().getKey();
                        group.setId(groupId);

                        databaseReference.child(groupId).setValue(group);
                    } else {
                        groupId = group.getId();
                    }
                });

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(IntentConstants.GROUP_ID.name(), groupId);
        startActivity(intent);

        groupId = null;
    }

    private static class ListItemHolder extends RecyclerView.ViewHolder {

        TextView item;

        public ListItemHolder(@NonNull View itemView) {
            super(itemView);

            item = itemView.findViewById(R.id.contact_name);
        }
    }
}
