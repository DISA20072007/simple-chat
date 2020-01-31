package ru.deniskrd.android.simplechat.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import ru.deniskrd.android.simplechat.R;
import ru.deniskrd.android.simplechat.activities.ChatActivity;
import ru.deniskrd.android.simplechat.constants.AppConstants;
import ru.deniskrd.android.simplechat.constants.IntentConstants;
import ru.deniskrd.android.simplechat.listeners.RecyclerItemClickListener;
import ru.deniskrd.android.simplechat.model.Group;
import ru.deniskrd.android.simplechat.model.User;


/**
 * A simple {@link Fragment} subclass.
 */
public class GroupListFragment extends Fragment {
    public GroupListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView groupList = view.findViewById(R.id.group_list);

        FirebaseRecyclerAdapter<Group, ListItemHolder> listAdapter = new FirebaseRecyclerAdapter<Group, ListItemHolder>(Group.class, R.layout.group_list_item,
                ListItemHolder.class, FirebaseDatabase.getInstance().getReference().child("groups")) {

            @Override
            protected void populateViewHolder(ListItemHolder listItemHolder, Group group, int i) {
                TextView groupName = listItemHolder.item;

                FirebaseUser user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());
                if ( (group.getAdmin().equals(user.getEmail()) || group.getMembers().contains(new User(user.getEmail())))
                        && !group.getName().contains(AppConstants.PRIVATE_GROUP_SUFFIX)) {
                    groupName.setText(group.getName());
                } else {
                    groupName.setVisibility(View.GONE);
                }

                registerForContextMenu((View) groupName.getParent());
            }
        };

        groupList.setAdapter(listAdapter);

        groupList.addOnItemTouchListener(new RecyclerItemClickListener(getContext(), groupList, view1 -> {
            Group group = listAdapter.getItem(groupList.getChildAdapterPosition(view1));

            Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class);
            intent.putExtra(IntentConstants.GROUP_ID.name(), group.getId());
            startActivity(intent);
        }));
    }

    public static class ListItemHolder extends RecyclerView.ViewHolder {

        TextView item;

        public ListItemHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.group_name);
        }
    }
}
