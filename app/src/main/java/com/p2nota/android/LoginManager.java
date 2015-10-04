package com.p2nota.android;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;

public class LoginManager
{
    static final String LOG_TAG = "LoginManager";
    static final String USER_AGENT = "p2nota/1.0";
    static final String FILENAME = "username.dat";
    LoginActivity mActivityHandle;
    CookieManager mCookieManager;

    LoginManager()
    {
        mCookieManager = new CookieManager();
        CookieHandler.setDefault(mCookieManager);
    }

    public void reset_cookies()
    {
        mCookieManager = new CookieManager();
        CookieHandler.setDefault(mCookieManager);
    }

    public void doLogin(LoginActivity activity, String username, String password) throws Exception
    {
        Log.i(LOG_TAG, "doLogin: " + username);

        if ((username.length() == 0) || (password.length() == 0))
        {
            activity.loginError(activity.getString(R.string.gui_login_error_credentials));
            return;
        }

        reset_cookies();
        mActivityHandle = activity;

        LoginTask task = new LoginTask();
        task.execute(username, password);
    }

    JSONObject fetchGrades() throws Exception
    {
        return new LoginTask().fetchGrades();
    }

    String getUsername(Context context)
    {
        try
        {
            BufferedInputStream input = new BufferedInputStream(context.openFileInput(FILENAME));
            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            String inputLine = br.readLine();
            if (inputLine != null)
                return inputLine;

            throw new IOException("empty file");
        }

        catch (IOException e)
        {
            return "";
        }
    }

    void saveUsername(Context context, String username)
    {
        try
        {
            FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(username.getBytes());
            fos.close();
        }

        catch (Exception e)
        {
            Log.e(LOG_TAG, "saveUsername failed: " + e.toString(), e);
        }
    }

    private abstract class RequestTask extends AsyncTask<String, Void, JSONObject>
    {
        protected HttpURLConnection doGet(String url) throws Exception
        {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            return conn;
        }

        protected HttpURLConnection doPost(String url, String parameters) throws Exception
        {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(parameters);
            out.flush();
            out.close();
            return conn;
        }

        protected String readResponse(HttpURLConnection conn) throws Exception
        {
            BufferedInputStream input = new BufferedInputStream(conn.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            StringBuilder sb = new StringBuilder();
            String inputLine = "";
            while ((inputLine = br.readLine()) != null)
                sb.append(inputLine);

            return sb.toString();
        }
    }

    private class LoginTask extends RequestTask
    {
        Exception mError = null;

        @Override
        protected JSONObject doInBackground(String... params)
        {
            String username = params[0];
            String password = params[1];

            HttpURLConnection conn;
            JSONObject grades = null;

            try
            {
                conn = doPost(mActivityHandle.getString(R.string.api_login_endpoint),
                              "Usuario=" + username + "&Password=" + password);
                Log.d(LOG_TAG, "login result: " + readResponse(conn));

                conn = doGet(mActivityHandle.getString(R.string.api_student_redirect));
                String studentReplyData = readResponse(conn);
                Log.d(LOG_TAG, "redirect result: " + studentReplyData);

                String tokenPath = "";
                String[] cmds = studentReplyData.split(";");
                for (String cmd : cmds)
                {
                    cmd = cmd.trim();
                    String[] x = cmd.split("=");
                    if (x.length == 2 && x[1].trim().startsWith("'https://student."))
                    {
                        tokenPath = x[1];
                        tokenPath = tokenPath.trim().substring(1, tokenPath.length() - 2);
                        break;
                    }
                }

                Log.d(LOG_TAG, "token path: " + tokenPath);
                conn = doGet(tokenPath);
                Log.d(LOG_TAG, "redirect result: " + readResponse(conn));
                grades = fetchGrades();
            }

            catch (MalformedURLException e)
            {
                Log.w(LOG_TAG, "MalformedURLException: " + e.getMessage(), e);
                mError = new Exception(mActivityHandle.getString(R.string.gui_login_error_credentials));
            }

            catch (Exception e)
            {
                Log.e(LOG_TAG, "LoginManager error: " + e.toString(), e);
                mError = e;
                grades = null;
            }

            return grades;
        }

        @Override
        protected void onPostExecute(JSONObject grades)
        {
            if (grades == null)
            {
                String errString = (mError == null) ?
                                    mActivityHandle.getString(R.string.gui_login_error_connection) :
                                    mError.getMessage();
                mActivityHandle.loginError(errString);
                return;
            }

            mActivityHandle.loginSuccessful(grades);
        }

        public JSONObject fetchGrades() throws Exception
        {
            HttpURLConnection conn = doGet(mActivityHandle.getString(R.string.api_grades_endpoint));
            String data = readResponse(conn);
            return new JSONObject(data);
        }
    }
}
