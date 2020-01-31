package ru.deniskrd.android.simplechat.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import io.reactivex.disposables.Disposable;
import ru.deniskrd.android.simplechat.R;
import ru.deniskrd.android.simplechat.constants.AuthConstants;
import ru.deniskrd.android.simplechat.model.User;
import ru.deniskrd.android.simplechat.rest.FirebaseRestService;

import static android.util.Patterns.EMAIL_ADDRESS;

public class AddContactFragmentDialog extends DialogFragment {

    private String errorMessage;

    private Disposable subscription;
    private Disposable innerSubscription;

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }

        if (innerSubscription != null && !innerSubscription.isDisposed()) {
            innerSubscription.dispose();
        }
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Activity activity = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final LayoutInflater layoutInflater = activity.getLayoutInflater();

        final View rootView = layoutInflater.inflate(R.layout.add_contact_dialog, null);
        builder.setView(rootView);

        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    dialog.cancel();
                    break;
            }
        };

        builder.setPositiveButton(R.string.ok, onClickListener).setNegativeButton(R.string.cancel, onClickListener);

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();

        final AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog != null) {
            View rootView = alertDialog.findViewById(R.id.add_contact_layout);

            Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);

            ProgressBar progressBar = getActivity().findViewById(R.id.progress_bar);

            positiveButton.setOnClickListener(v -> {
                progressBar.setVisibility(View.VISIBLE);

                addUserContact(rootView);

                if (errorMessage == null) {
                    alertDialog.dismiss();
                }
            });
        }
    }

    private void addUserContact(final View view) {
        EditText editText = view.findViewById(R.id.email_input);
        String email = editText.getText().toString();

        errorMessage = null;

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(AuthConstants.AUTH_DATA.name(), Context.MODE_PRIVATE);
        final String token = sharedPreferences.getString(AuthConstants.KEY_AUTH_TOKEN.name(), "");
        if (token.isEmpty()) {
            errorMessage = getContext().getResources().getString(R.string.token_is_empty);
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        subscription = FirebaseRestService.getInstance().getChatUser(token, firebaseUser.getEmail())
                .doAfterSuccess(user -> {
                    if (!EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = getContext().getResources().getString(R.string.invalid_email);
                    } else if (Objects.equals(email, firebaseUser.getEmail())) {
                        errorMessage = getContext().getResources().getString(R.string.duplicate_contact_username_error);
                    } else if (user.getContacts().contains(new User(email))) {
                        errorMessage = getContext().getResources().getString(R.string.exist_contact_username_error);
                    } else {
                        final AlertDialog alertDialog = (AlertDialog) getDialog();
                        alertDialog.dismiss();

                        innerSubscription = FirebaseRestService.getInstance().getChatUser(token, email)
                                .subscribe(contact -> {
                                    if (contact == null) {
                                        errorMessage = getContext().getResources().getString(R.string.contact_not_registered);
                                    } else {
                                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("users");

                                        user.getContacts().add(contact);
                                        databaseReference.child(firebaseUser.getUid()).setValue(user);
                                    }
                                });

                    }
                }).subscribe(user -> {
                    ProgressBar progressBar = getActivity().findViewById(R.id.progress_bar);
                    progressBar.setVisibility(View.GONE);
                });

        if (errorMessage != null)
            editText.setError(errorMessage);
    }
}
