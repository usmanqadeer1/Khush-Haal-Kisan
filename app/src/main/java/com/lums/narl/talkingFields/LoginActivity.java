package com.lums.narl.talkingFields;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lums.narl.talkingFields.Utils.LocaleUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    public static final String user_data = "USER_DATA";
    private Button buttonLogin,sendCode;
    private EditText editPhone, editCode,editName;
    private TextView textSignUp;
    private Spinner languageSpinner;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private String phoneVerificationId;
    private DatabaseReference usersDatabase;
    private String code;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationCallbacks;
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleUtils.loadLocale(this);
        setContentView(R.layout.activity_login);
        setTitle(R.string.app_name);

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        usersDatabase = FirebaseDatabase.getInstance().getReference();

        if(firebaseUser != null){
            editor.putBoolean(getString(R.string.login_status), true);                           // user is already logged in, loginStatus = 1, no need to load data again from firebase database
            editor.apply();
            openMainActivity();
            finish();
        }

        progressBar = new ProgressBar(this);
        editPhone = findViewById(R.id.edit_phone);
        editCode = findViewById(R.id.edit_code);
        editName = findViewById(R.id.edit_name);
        sendCode = findViewById(R.id.send_code);
        buttonLogin = findViewById(R.id.sign_in);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String phoneNumber = preferences.getString("phone_number", null);
        if(phoneNumber!=null){
            editPhone.setText(phoneNumber);
        }
        sendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(LocaleUtils.isNetworkAvailable(getApplicationContext())){
                    editCode.setVisibility(View.VISIBLE);
                    sendCode.setText(getString(R.string.resend_code));
                    buttonLogin.setVisibility(View.VISIBLE);
                    Toast.makeText(LoginActivity.this,getString(R.string.login_wait), Toast.LENGTH_LONG).show();
                    sendCode();
                }
                else{
                    Toast.makeText(getApplicationContext(), getString(R.string.network_not_available),Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String codeRead = editCode.getText().toString();
                if(codeRead != null){
                    verifyCode(codeRead);
                }
            }
        });

    }


    private void sendCode(){
        String phoneNumber = editPhone.getText().toString().trim();
        if(TextUtils.isEmpty(phoneNumber))
            return;
        setUpVerificationCallbacks();
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,60,
                TimeUnit.SECONDS,
                this,
                verificationCallbacks);
    }

    private void setUpVerificationCallbacks(){
        verificationCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(final PhoneAuthCredential phoneAuthCredential) {
                /*String code = phoneAuthCredential.getSmsCode();
                editCode.setText(code);
                if(code != null){
                    verifyCode(code);
                }*/
                code = phoneAuthCredential.getSmsCode();
                editCode.setText(code);

            }

            @Override
            public void onVerificationFailed(FirebaseException e) {

            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(verificationId, forceResendingToken);
                phoneVerificationId = verificationId;
                resendingToken = forceResendingToken;
            }
            @Override
            public void onCodeAutoRetrievalTimeOut(String s) {
                super.onCodeAutoRetrievalTimeOut(s);

            }
        };
    }

    private void verifyCode(String code){
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(phoneVerificationId,code);
        signInwithPhoneAuthCredential(credential);
    }

    private void signInwithPhoneAuthCredential(PhoneAuthCredential credential){
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    firebaseUser = firebaseAuth.getCurrentUser();
                    editor.putBoolean(getString(R.string.login_status), false);          //user have signed in, loginStatus = false so that user can load data from firebase
                    editor.putBoolean("maps_activity_opened",false);
                    editor.putBoolean("fields_activity_opened",false);
                    editor.apply();
                    firebaseUser = task.getResult().getUser();

                    final String userID = firebaseUser.getUid();
                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference(userID);

                    rootRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                openMainActivity();
                                finish();
                            }
                            else{
                                buttonLogin.setVisibility(View.VISIBLE);
                                editName.setVisibility(View.VISIBLE);
                                sendCode.setVisibility(View.GONE);
                                editCode.setVisibility(View.GONE);
                                makeNewUser();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }
            }
        });
    }

    private void makeNewUser(){
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String phone = editPhone.getText().toString().trim();
                final String name = editName.getText().toString().trim();

                buttonLogin.setText("Make Account");
                if(TextUtils.isEmpty(phone)){
                    Toast.makeText(LoginActivity.this, getString(R.string.valid_phone), Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(name)){
                    Toast.makeText(LoginActivity.this, getString(R.string.valid_name), Toast.LENGTH_SHORT).show();
                    return;
                }

                User user = new User(name, phone,null);
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                usersDatabase.child(firebaseUser.getUid())
                        .setValue(user)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    finish();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                }
                                else{
                                    Toast.makeText(getApplicationContext(), "No Database", Toast.LENGTH_SHORT).show();

                                }
                            }
                        });
            }
        });
    }
    private void openMainActivity(){
        Intent openMain = new Intent(LoginActivity.this, MainActivity.class);
        openMain.putExtra(user_data, firebaseUser.getUid());
        startActivity(openMain);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_login, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.language:
                    showChangeLanguageDialog();
                return true;
            default:
                break;
        }
        return false;
    }

    private void showChangeLanguageDialog(){
        final String[] listItems = {"English","Deutsche","اردو"};
        AlertDialog.Builder mBuilder = new  AlertDialog.Builder(LoginActivity.this);
        mBuilder.setTitle(getString(R.string.change_language));
        mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i==0){
                    LocaleUtils.setLocale("es",getApplicationContext());
                    recreate();
                }
                if(i==1){
                    LocaleUtils.setLocale("de",getApplicationContext());
                    recreate();
                }
                if(i==2){
                    LocaleUtils.setLocale("ur",getApplicationContext());
                    recreate();
                }
                dialogInterface.dismiss();

            }
        });

        mBuilder.setCancelable(true);
        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }
}
