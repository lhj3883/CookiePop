package com.cookiefeeder.cookiepop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;


public class RegistrationActivity extends AppCompatActivity implements View.OnClickListener
{
    /** Declaration of Registration Activity class fields **/
    private final int SIGN_UP_SUCCESS = 0;
    private final int SIGN_UP_ALREADY_EXIST = 1;
    private final int SIGN_UP_FAIL = 2;

    private TextView tv_agreement1, tv_agreement2, tv_agreement3;
    private EditText et_id, et_password, et_password_confirm, et_name, et_birthday;
    private CheckBox cb_agreement1, cb_agreement2, cb_agreement3;
    private Button btn_sendAuthCode, btn_registration;

    private NetworkService networkService;
    private boolean onNetworkServiceBound;
    private boolean isAuthenticated;

    /* bind service connection */
    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            NetworkService.NetworkServiceBinder binder = (NetworkService.NetworkServiceBinder) service;
            networkService = binder.getService();
            onNetworkServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            networkService = null;
            onNetworkServiceBound= false;
        }
    };

    /* Sign up result Broadcast Receiver (Activity <- Service) */
    private BroadcastReceiver signUpResultReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int result = intent.getIntExtra("result", SIGN_UP_FAIL);
            Log.d("signUp", "sign up result : " + result);
            switch(result)
            {
                case SIGN_UP_SUCCESS:
                    Toast.makeText(getApplicationContext(), "회원가입에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                    Intent mIntent = new Intent(getApplication(), LoginActivity.class);
                    startActivity(mIntent);
                    finish();
                    break;
                case SIGN_UP_ALREADY_EXIST:
                    Toast.makeText(getApplicationContext(), "이미 존재하는 계정입니다.", Toast.LENGTH_SHORT).show();
                    break;
                case SIGN_UP_FAIL:
                    Toast.makeText(getApplicationContext(), "회원가입에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    /* server's auth code Broadcast Receiver (Activity <- Service) */
    private BroadcastReceiver authCodeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String authCode = intent.getStringExtra("code");
            Intent mIntent = new Intent(getApplication(), AuthenticationActivity.class);

            mIntent.putExtra("authCode", authCode);
            mIntent.putExtra("ActivityNum", AuthenticationActivity.REGISTRATION_ACTIVITY);
            mIntent.putExtra("id", et_id.getText().toString());
            startActivity(mIntent);
            finish();
        }
    };


    /** Override Methods **/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        initData();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(signUpResultReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(authCodeReceiver);
        unbindService(mConnection);
    }

    /** Initialize class fields **/
    private void initData()
    {
        tv_agreement1 = findViewById(R.id.tv_agreement1);
        tv_agreement2 = findViewById(R.id.tv_agreement2);
        tv_agreement3 = findViewById(R.id.tv_agreement3);

        et_id = findViewById(R.id.et_registration_id);
        et_password = findViewById(R.id.et_registration_password);
        et_password_confirm = findViewById(R.id.et_registration_pwconfirm);
        et_name = findViewById(R.id.et_registration_name);
        et_birthday = findViewById(R.id.et_registration_birthday);

        cb_agreement1 = findViewById(R.id.cb_agreement1);
        cb_agreement2 = findViewById(R.id.cb_agreement2);
        cb_agreement3 = findViewById(R.id.cb_agreement3);

        btn_sendAuthCode = findViewById(R.id.btn_requestAuthCode);
        btn_registration = findViewById(R.id.btn_registration);

        tv_agreement1.setOnClickListener(this);
        tv_agreement2.setOnClickListener(this);
        tv_agreement3.setOnClickListener(this);
        btn_sendAuthCode.setOnClickListener(this);
        btn_registration.setOnClickListener(this);

        Intent intent = new Intent(this, NetworkService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(signUpResultReceiver, new IntentFilter("signUpResult"));
        LocalBroadcastManager.getInstance(this).registerReceiver(authCodeReceiver, new IntentFilter("authCode"));

        isAuthenticated = getIntent().getBooleanExtra("authResult", false);
        et_id.setText(getIntent().getStringExtra("id"));
        if(isAuthenticated) {
            et_id.setEnabled(false);
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onClick(View v)
    {
        Intent intent = new Intent(getApplication(), TermsActivity.class);
        switch(v.getId())
        {
            case R.id.btn_requestAuthCode:
                requestAuthCode(v);
                break;
            case R.id.btn_registration:
                signUp(v);
                break;
            case R.id.tv_agreement1:
                intent.putExtra("agreementNum", TermsActivity.TERMS_AGREEMENT1);
                startActivity(intent);
                break;
            case R.id.tv_agreement2:
                intent.putExtra("agreementNum", TermsActivity.TERMS_AGREEMENT2);
                startActivity(intent);
                break;
            case R.id.tv_agreement3:
                intent.putExtra("agreementNum", TermsActivity.TERMS_AGREEMENT3);
                startActivity(intent);
                break;
        }
    }

    /** Make Authentication code **/
    private void requestAuthCode(View v)
    {
        String id = et_id.getText().toString();

        Snackbar snackbar = Snackbar.make(v, "", Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(Color.parseColor("#AED581"));

        if(isAuthenticated)
        {
            snackbar.setText("인증 완료된 이메일주소 입니다.");
            snackbar.show();
        }
        else if(id.equals(""))
        {
            snackbar.setText("아이디를 입력해주세요.");
            snackbar.show();
        }
        else
        {
            if(onNetworkServiceBound)
            {
                JSONObject jsonObject = new JSONObject();
                try
                {
                    jsonObject.put("userID", id);
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
                networkService.requestAuthCode(jsonObject);
            }
        }
    }

    /** Sign Up method (Send Data Activity -> Service) **/
    private void signUp(View v)
    {
        String id = et_id.getText().toString();
        String pw = et_password.getText().toString();
        String pw_confirm = et_password_confirm.getText().toString();
        String name = et_name.getText().toString();
        String birthday = et_birthday.getText().toString();

        Snackbar snackbar = Snackbar.make(v, "", Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(Color.parseColor("#AED581"));

        if(id.equals(""))
        {
            snackbar.setText("아이디를 입력해주세요.");
            snackbar.show();
        }
        else if(id.indexOf("@") == -1 || id.indexOf(".") == -1)
        {
            snackbar.setText("이메일형식으로 적어주세요");
            snackbar.show();
        }
        else if(pw.equals("") || pw_confirm.equals(""))
        {
            snackbar.setText("비밀번호를 입력해주세요.");
            snackbar.show();
        }
        else if(name.equals(""))
        {
            snackbar.setText("이름을 입력해주세요.");
            snackbar.show();
        }
        else if(birthday.equals(""))
        {
            snackbar.setText("생년월일을 입력해주세요.");
            snackbar.show();
        }
        else if(!pw.equals(pw_confirm))
        {
            snackbar.setText("비밀번호를 확인해주세요.");
            snackbar.show();
        }
        else if(!isAuthenticated)
        {
            snackbar.setText("이메일 인증을 해주세요.");
            snackbar.show();
        }
        else if(!cb_agreement1.isChecked() || !cb_agreement2.isChecked())
        {
            snackbar.setText("필수 약관에 모두 동의해주세요.");
            snackbar.show();
        }
        else
        {
            if(onNetworkServiceBound)
            {
                String pw_hash = new Crypto().hashing(pw, "sha256");
                JSONObject jsonObject = new JSONObject();
                try
                {
                    jsonObject.put("userID", id);
                    jsonObject.put("userPW", pw_hash);
                    jsonObject.put("userName", name);
                    jsonObject.put("userBirthday", birthday);
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
                networkService.signUp(jsonObject);
            }
        }
    }
}
