package ru.deniskrd.android.simplechat.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ru.deniskrd.android.simplechat.R;
import ru.deniskrd.android.simplechat.model.Group;
import ru.deniskrd.android.simplechat.model.User;

/**
 * A simple {@link Fragment} subclass.
 */
public class GroupEditFragment extends Fragment {

    private String groupId;

    public GroupEditFragment() {
        super();
    }

    public GroupEditFragment(String groupId) {
        super();

        this.groupId = groupId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (groupId == null) return;

        final View rootView = view.findViewById(R.id.group_edit_form);
        rootView.setTag(groupId);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("groups").child(groupId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Group group = dataSnapshot.getValue(Group.class);

                EditText groupNameInput = rootView.findViewById(R.id.group_name_input);
                groupNameInput.setText(group.getName());

                CheckBox privateGroupCheck = rootView.findViewById(R.id.private_group_check);
                privateGroupCheck.setChecked(group.isPrivate());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        ListView memberList = view.findViewById(R.id.member_list);

        FirebaseListAdapter<User> memberListAdapter = new FirebaseListAdapter<User>(getActivity(), User.class, R.layout.member_list_item,
                                                                                    databaseReference.child("members")) {
            @Override
            protected void populateView(View v, User member, int position) {
                TextView memberUsername = v.findViewById(R.id.member_username);
                memberUsername.setText(member.getDisplayName());

                Button deleteButton = v.findViewById(R.id.delete_member_button);
                deleteButton.setTag(position);
            }
        };

        memberList.setAdapter(memberListAdapter);
    }
}
