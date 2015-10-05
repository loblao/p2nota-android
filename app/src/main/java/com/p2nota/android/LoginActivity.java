package com.p2nota.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONObject;

public class LoginActivity extends Activity
{
    static final String LOG_TAG = "LoginActivity";
    static LoginManager sManager = new LoginManager();
    private EditText mUsernameEntry;
    private EditText mPasswordEntry;
    private Button mLoginButton;
    private TextView mStatusLabel;
    private String mUsername;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        mUsernameEntry = (EditText) findViewById(R.id.username);
        mUsernameEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.username || id == EditorInfo.IME_NULL) {
                    mPasswordEntry.requestFocus();
                    return true;
                }
                return false;
            }
        });

        mPasswordEntry = (EditText) findViewById(R.id.password);
        mPasswordEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent)
            {
                if (id == R.id.password || id == EditorInfo.IME_NULL)
                {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mStatusLabel = (TextView) findViewById(R.id.label_status);
        mStatusLabel.setText("");

        mUsernameEntry.setText(sManager.getUsername(this));
    }

    private void attemptLogin()
    {
        mUsernameEntry.setEnabled(false);
        mPasswordEntry.setEnabled(false);
        mLoginButton.setEnabled(false);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage(getString(R.string.gui_label_login_progress));
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        try
        {
            mUsername = mUsernameEntry.getText().toString();
            sManager.doLogin(this, mUsername,
                             mPasswordEntry.getText().toString());
        }

        catch (Exception e)
        {
            Log.e(LOG_TAG, "doLogin error: " + e.toString(), e);
            loginError(e.getMessage());
        }
    }

    public void loginError(String issue)
    {
        mProgressDialog.dismiss();
        mStatusLabel.setTextColor(Color.RED);
        mStatusLabel.setText(issue);

        mUsernameEntry.setEnabled(true);
        mPasswordEntry.setEnabled(true);
        mLoginButton.setEnabled(true);
    }

    public void loginSuccessful(JSONObject grades)
    {
        mProgressDialog.dismiss();
        GradesParser parser = new GradesParser(this, grades);
        Log.i(LOG_TAG, "loginSuccessful");
        Log.i(LOG_TAG, parser.success() ? "SUCCESS": "FAILURE");

        if (!parser.success())
        {
            Log.i(LOG_TAG, grades.toString());
            try
            {
                Log.w(LOG_TAG, "mParser issue: " + parser.issue());
                loginError(parser.issue());
            }

            catch (Exception e)
            {
                Log.e(LOG_TAG, "parser issue error: " + e.toString(), e);
                loginError(e.getMessage());
            }

            return;
        }

        // Save it
        sManager.saveUsername(this, mUsername);

        // Return to MainActivity and pass our data
        Intent result = new Intent();
        result.putExtra("grades-data", grades.toString());
        setResult(RESULT_OK, result);
        finish();
    }
}
