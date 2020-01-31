package ru.deniskrd.android.simplechat.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Objects;

import io.reactivex.disposables.Disposable;
import ru.deniskrd.android.simplechat.R;
import ru.deniskrd.android.simplechat.constants.AppConstants;
import ru.deniskrd.android.simplechat.constants.AuthConstants;
import ru.deniskrd.android.simplechat.constants.IntentConstants;
import ru.deniskrd.android.simplechat.fragments.GroupEditFragment;
import ru.deniskrd.android.simplechat.fragments.GroupListFragment;
import ru.deniskrd.android.simplechat.model.Group;
import ru.deniskrd.android.simplechat.model.User;
import ru.deniskrd.android.simplechat.rest.FirebaseRestService;

public class GroupsActivity extends AppCompatActivity {

    private ProgressBar progressBar;

    private String groupId;
    private Group selectedGroup;

    private Disposable subscription;
    private Disposable innerSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.groups);

        Intent intent = getIntent();
        if (intent.hasExtra(IntentConstants.MEMBER_USERNAME.name())) {
            groupId = intent.getStringExtra(IntentConstants.GROUP_ID.name());
            String memberUserName = intent.getStringExtra(IntentConstants.MEMBER_USERNAME.name());
            addNewMember(memberUserName);
        }

        showGroupFragment(false);

        setContentView(R.layout.activity_groups);

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }

        if (innerSubscription != null && !innerSubscription.isDisposed()) {
            innerSubscription.dispose();
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments.size() == 1 && fragments.get(0) instanceof GroupListFragment) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.add_group_item:
                addNewGroup();
                break;
        }

        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        ViewGroup layout = (ViewGroup) v;

        RecyclerView recyclerView = (RecyclerView) layout.getParent();

        TextView groupNameText = layout.findViewById(R.id.group_name);

        FirebaseRecyclerAdapter<Group, GroupListFragment.ListItemHolder> adapter = (FirebaseRecyclerAdapter<Group, GroupListFragment.ListItemHolder>) recyclerView.getAdapter();

        this.selectedGroup = null;
        for (int i = 0; i < adapter.getItemCount(); i ++) {
            Group group = adapter.getItem(i);
            if (group.getName().equals(groupNameText.getText().toString())) {
                this.selectedGroup = group;
            }
        }

        if (this.selectedGroup != null) {
            MenuInflater inflater = getMenuInflater();
            FirebaseUser user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());
            inflater.inflate(Objects.equals(user.getEmail(), this.selectedGroup.getAdmin()) ? R.menu.group_admin_menu : R.menu.group_user_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        Group group = this.selectedGroup;

        switch (item.getItemId()) {
            case R.id.edit_menu_item:
                this.groupId = group.getId();
                showGroupFragment(true);
                break;
            case R.id.delete_menu_item:
                showAlertDialog(group, true);
                break;
            case R.id.exit_menu_item:
                showAlertDialog(group, false);
                break;
        }

        return true;
    }

    private void showAlertDialog(final Group group, final boolean isDelete) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).setCancelable(true).create();
        alertDialog.setMessage(getResources().getString(isDelete ? R.string.delete_group_warning : R.string.exit_group_warning));

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    if (isDelete) {
                        FirebaseDatabase.getInstance().getReference().child("groups").child(group.getId()).removeValue();
                    } else {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        group.getMembers().remove(new User(user.getEmail()));

                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("groups");
                        databaseReference.child(group.getId()).setValue(group);
                    }

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

    private void addNewGroup() {
        this.groupId = null;
        showGroupFragment(true);
    }

    public void saveGroup(View view) {
        TextView groupNameInput = findViewById(R.id.group_name_input);
        CheckBox privateCheck = findViewById(R.id.private_group_check);
        ListView memberList = findViewById(R.id.member_list);

        String errorMessage = "";

        String groupName = groupNameInput.getText().toString();
        if (groupName.isEmpty()) {
            errorMessage = getResources().getString(R.string.group_name_is_empty);
        } else if (groupName.contains(AppConstants.PRIVATE_GROUP_SUFFIX)) {
            errorMessage = getResources().getString(R.string.private_group_name_forbidden, AppConstants.PRIVATE_GROUP_SUFFIX);
        }

        if (!errorMessage.isEmpty()) {
            groupNameInput.setError(errorMessage);
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Group group = new Group();
        group.setAdmin(currentUser.getEmail());
        group.setName(groupName);
        group.setPrivate(privateCheck.isChecked());

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        View rootView = findViewById(R.id.group_edit_form);
        String groupId;
        if (rootView.getTag() != null) {
            groupId = rootView.getTag().toString();
        } else {
            groupId = databaseReference.child("groups").push().getKey();
        }
        group.setId(groupId);

        for (int i = 0; i < memberList.getCount(); i++) {
            User member = (User) memberList.getItemAtPosition(i);
            if (!group.getMembers().contains(member)) {
                group.getMembers().add(member);
            }
        }

        databaseReference.child("groups").child(groupId).setValue(group);

        this.groupId = null;
        showGroupFragment(false);
    }

    public void addMember(View view) {
        Intent intent = new Intent(this, ContactsActivity.class);
        intent.putExtra(IntentConstants.GROUP_ID.name(), groupId);
        startActivity(intent);
    }

    private void showGroupFragment(boolean isEdit) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.group_fragment_layout, isEdit ? new GroupEditFragment(groupId) : new GroupListFragment());
        if (groupId != null) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    private void addNewMember(String memberName) {
        SharedPreferences sharedPreferences = getSharedPreferences(AuthConstants.AUTH_DATA.name(), Context.MODE_PRIVATE);
        final String token = sharedPreferences.getString(AuthConstants.KEY_AUTH_TOKEN.name(), "");

        progressBar.setVisibility(View.VISIBLE);

        subscription = FirebaseRestService.getInstance().getChatGroup(token, groupId)
                .doAfterSuccess(group ->
                        innerSubscription = FirebaseRestService.getInstance().getChatUser(token, memberName).subscribe(member -> {
                        group.getMembers().add(member);
                        FirebaseDatabase.getInstance().getReference().child("groups").child(groupId).setValue(group);
                })).subscribe(user -> progressBar.setVisibility(View.GONE));
    }

    public void deleteMember(View view) {
        Button button = (Button) view;
        Integer position = (Integer) button.getTag();

        if (position != null) {
            SharedPreferences sharedPreferences = getSharedPreferences(AuthConstants.AUTH_DATA.name(), Context.MODE_PRIVATE);
            final String token = sharedPreferences.getString(AuthConstants.KEY_AUTH_TOKEN.name(), "");

            subscription = FirebaseRestService.getInstance().getChatGroup(token, groupId)
                    .subscribe(group -> {
                        group.getMembers().remove((int) position);

                        FirebaseDatabase.getInstance().getReference().child("groups").child(groupId).setValue(group);
                    });
        }
    }
}
