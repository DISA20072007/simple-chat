package ru.deniskrd.android.simplechat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import io.reactivex.disposables.Disposable;
import ru.deniskrd.android.simplechat.R;
import ru.deniskrd.android.simplechat.constants.AuthConstants;
import ru.deniskrd.android.simplechat.model.User;
import ru.deniskrd.android.simplechat.rest.FirebaseRestService;

public class MainActivity extends AppCompatActivity {

    private final static int SIGN_IN_CODE = 1;

    private RelativeLayout mainLayout;

    private Disposable subscription;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SIGN_IN_CODE) {
            if (resultCode == RESULT_OK) {
                saveAuthData();

                Snackbar.make(mainLayout, R.string.authorization_success_message, Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar snackbar = Snackbar.make(mainLayout, R.string.authorization_error_message, Snackbar.LENGTH_LONG);
                snackbar.show();

                mainLayout.postDelayed(() -> startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_CODE), 3000);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_CODE);
        } else {
            saveAuthData();
            Snackbar.make(mainLayout, R.string.authorization_success_message, Snackbar.LENGTH_LONG).show();
        }
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
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.contacts_item:
                startActivity(new Intent(this, ContactsActivity.class));
                break;
            case R.id.groups_item:
                startActivity(new Intent(this, GroupsActivity.class));
                break;
            case R.id.sign_out_item:
                AuthUI.getInstance().signOut(getApplicationContext());
                startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_CODE);
                break;
        }

        return true;
    }

    private void saveAuthData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        currentUser.getIdToken(true).addOnSuccessListener(tokenResult -> {
            String token = tokenResult.getToken();
            getBaseContext().getSharedPreferences(AuthConstants.AUTH_DATA.name(), MODE_PRIVATE)
                    .edit().putString(AuthConstants.KEY_AUTH_TOKEN.name(), token).apply();

            subscription = FirebaseRestService.getInstance().getChatUser(token, currentUser.getEmail())
                    .subscribe(user -> {
                        if (user == null) {
                            FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid())
                                    .setValue(new User(currentUser.getEmail(), currentUser.getDisplayName()));
                        }
                    });
        });
    }
}
