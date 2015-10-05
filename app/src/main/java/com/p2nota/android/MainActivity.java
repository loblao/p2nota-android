package com.p2nota.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.json.JSONObject;

public class MainActivity extends Activity
{
    static final String LOG_TAG = "MainActivity";
    static MainActivity sGlobalHandle;
    private Spinner mSubjectList;
    private ArrayAdapter<String> mSubjectListAdapter;
    private GradesParser mParser;
    private Button mLogoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        sGlobalHandle = this;

        mSubjectList = (Spinner) findViewById(R.id.subject_list);
        mSubjectListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
        mSubjectListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSubjectList.setAdapter(mSubjectListAdapter);

        final MainActivity activity = this;
        mSubjectList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                mParser.selectSubject(mSubjectList.getSelectedItemPosition(), activity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mLogoutButton = (Button) findViewById(R.id.logout_button);
        mLogoutButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LoginActivity.sManager.reset_cookies();
                showLogin();
            }
        });

        showLogin();
    }

    private void showLogin()
    {
        Intent i = new Intent(this, LoginActivity.class);
        startActivityForResult(i, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1 && resultCode == RESULT_OK && data != null)
        {
            Log.i(LOG_TAG, "onActivityResult");
            Log.i(LOG_TAG, data.getStringExtra("grades-data"));

            try
            {
                mParser = new GradesParser(this, new JSONObject(data.getStringExtra("grades-data")));
            }

            catch (Exception e)
            {
                mParser = null;
            }

            try
            {
                mParser.setupUi(this);
            }

            catch (Exception e)
            {
                Log.e(LOG_TAG, "mParser setup error: " + e.toString(), e);
                finish();
            }

            mParser.selectSubject(mSubjectList.getSelectedItemPosition(), this);
        }
    }

    public void addSubject(String subject)
    {
        mSubjectListAdapter.add(subject);
        mSubjectListAdapter.notifyDataSetChanged();
    }

    public void clearSubjects()
    {
        mSubjectListAdapter.clear();
        mSubjectListAdapter.notifyDataSetChanged();
    }
}
