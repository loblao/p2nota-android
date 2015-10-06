package com.p2nota.android;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import org.json.*;

import java.util.ArrayList;
import java.util.List;

public class GradesParser
{
    static final String LOG_TAG = "GradesParser";
    static final int GRADE_UNSET = -1;
    static final int GRADE_NO_SUCH_TEST = -2;

    // TO DO: use strings.xml
    static String GUI_ERROR_CREDENTIALS;
    JSONObject mObj;
    Context mContext;
    List<FrontData> mFrontData;

    GradesParser(Context context, JSONObject obj)
    {
        mObj = obj;
        mContext = context;
        mFrontData = new ArrayList<>();
        GUI_ERROR_CREDENTIALS = mContext.getString(R.string.gui_login_error_credentials);
    }

    boolean success()
    {
        try
        {
            return mObj.getBoolean("Sucesso");
        }

        catch (Exception e)
        {
            return false;
        }
    }

    String issue() throws Exception
    {
        if (!mObj.getBoolean("Sucesso"))
            return GUI_ERROR_CREDENTIALS;

        return "OK";
    }

    String getName() throws Exception
    {
        return mObj.getJSONObject("Dados").getJSONObject("Pessoa").getString("Nome");
    }

    String getEnrollment() throws Exception
    {
        return mObj.getJSONObject("Dados").getJSONObject("DadosPagina").getJSONArray("Matriculas").getJSONObject(0).getString("Nome");
    }

    void setupUi(MainActivity activity) throws Exception
    {
        TextView labelName = (TextView) activity.findViewById(R.id.label_name);
        TextView labelEnrollment = (TextView) activity.findViewById(R.id.label_enrollment);

        labelName.setText(getName());
        labelEnrollment.setText(getEnrollment());

        Log.i(LOG_TAG, getName());
        Log.i(LOG_TAG, getEnrollment());

        JSONArray subjects = mObj.getJSONObject("Dados").getJSONObject("DadosPagina").getJSONArray("Materias");
        for (int i = 0; i < subjects.length(); ++i)
        {
            JSONObject subject = subjects.getJSONObject(i);

            JSONArray fronts;

            try
            {
                fronts = subject.getJSONArray("Frentes");
            }

            catch (JSONException e)
            {
                fronts = null;
            }

            if ((fronts != null) && (fronts.length() > 0))
            {
                for (int j = 0; j < fronts.length(); ++j)
                {
                    handleFront(fronts.getJSONObject(j), activity);
                }
            }

            else
            {
                handleFront(subject, activity);
            }
        }

        if (mFrontData.size() == 0)
            throw new EmptySubjectList();
    }

    void handleFront(JSONObject subject, MainActivity activity) throws Exception
    {
        // Check if this is a valid subject/front:
        // MUST match these conditions:
        // 1 - A test called "Prova 1"
        // 2 - A test called "Prova 2"
        // 3 - NO "Prova 3"
        // 4 - NO "Redação"
        // 5 - NO "Relatório"

        float[] grades_p1 = {GRADE_NO_SUCH_TEST, GRADE_UNSET, GRADE_UNSET, GRADE_UNSET};
        float[] grades_p2 = {GRADE_NO_SUCH_TEST, GRADE_UNSET, GRADE_UNSET, GRADE_UNSET};
        float[] grades_final = {GRADE_UNSET, GRADE_UNSET, GRADE_UNSET, GRADE_UNSET};
        float[] bonus = {0, 0, 0, 0};
        boolean has_invalid_test = false;

        JSONArray grades = subject.getJSONArray("NotasBimestre");
        for (int bimester = 0; bimester < 4; ++bimester)
        {
            if (bimester >= grades.length())
                break;

            grades_final[bimester] = extractFloat(grades.getJSONObject(bimester).getString("Nota"), GRADE_UNSET);
            JSONArray tests = grades.getJSONObject(bimester).getJSONArray("Provas");
            if (tests.length() > 0)
            {
                for (int i = 0; i < tests.length(); ++i)
                {
                    JSONObject test = tests.getJSONObject(i);
                    String name = test.getString("Nome");

                    float grade = extractFloat(test.getString("Nota"), GRADE_UNSET);

                    if (name.contains("Bônus"))
                    {
                        bonus[bimester] = grade;
                    }

                    else if (name.contains("Prova 1"))
                    {
                        grades_p1[bimester] = grade;
                    }

                    else if (name.contains("Prova 2"))
                    {
                        grades_p2[bimester] = grade;
                    }

                    else if (name.contains("Prova 3") || name.contains("Redação") || name.contains("Relatório"))
                    {
                        has_invalid_test = true;
                        break;
                    }
                }

                if (has_invalid_test)
                    break;
            }
        }

        if (has_invalid_test)
            return;

        if (grades_p1[0] == GRADE_NO_SUCH_TEST || grades_p2[0] == GRADE_NO_SUCH_TEST)
            return;

        String name = subject.getString("Nome");
        activity.addSubject(name);

        FrontData dt = new FrontData();
        dt.name = name;
        dt.grades_p1 = grades_p1;
        dt.grades_p2 = grades_p2;
        dt.grades_final = grades_final;
        dt.bonus = bonus;

        mFrontData.add(dt);
    }

    public int selectSubject(int index, MainActivity activity)
    {
        if (index >= mFrontData.size() || index < 0)
            index = 0;

        FrontData dt = mFrontData.get(index);

        TextView label_b1 = (TextView) activity.findViewById(R.id.label_b1);
        TextView label_b2 = (TextView) activity.findViewById(R.id.label_b2);
        TextView label_b3 = (TextView) activity.findViewById(R.id.label_b3);
        TextView label_b4 = (TextView) activity.findViewById(R.id.label_b4);

        TextView[] labels = {label_b1, label_b2, label_b3, label_b4};

        for (int bimester = 0; bimester < 4; ++bimester)
        {
            float p1 = dt.grades_p1[bimester];
            float p2 = dt.grades_p2[bimester];
            float bonus = dt.bonus[bimester];

            TextView label = labels[bimester];
            int color = 0;

            if (p1 == GRADE_UNSET)
            {
                label.setText(activity.getString(R.string.gui_data_no_p1));
                continue;
            }

            String bonusData = String.format(activity.getString(R.string.gui_label_bonus), bonus);
            String htmlData = "<p>";
            htmlData += String.format(activity.getString(R.string.gui_label_p1), p1) + "<br/>";

            if (p2 != GRADE_UNSET)
            {
                htmlData += String.format(activity.getString(R.string.gui_label_p2), p2) + "<br/>";
                htmlData += bonusData;
                float total = dt.grades_final[bimester];
                float delta = total - 6;
                color = (delta >= 0) ? Color.GREEN : Color.RED;
                htmlData += "<br/>$";

                String status = (delta >= 0) ? activity.getString(R.string.gui_data_status_approved) :
                        String.format(activity.getString(R.string.gui_data_status_rec), Math.abs(delta),
                                      getPlural(Math.abs(delta)));

                htmlData += String.format(activity.getString(R.string.gui_data_status), status);
            }

            else
            {
                float missing = 12 - p1 - bonus * 2;
                // Calc how many questions you need
                htmlData += String.format(activity.getString(R.string.gui_label_p2_needed),
                        missing, getPlural(missing)) + "<br/>";

                if (missing > 10)
                    htmlData += activity.getString(R.string.gui_data_too_bad);

                else
                {
                    int lastMin = 0, gotFirstGroup = 0;
                    List<String> temp = new ArrayList<>();
                    for (int j = 3; j <= 24; ++j)
                    {
                        double value = 10.0 / j;
                        int qmin = (int) Math.ceil(missing / value);
                        if (qmin > lastMin || j == 24)
                        {
                            if (gotFirstGroup > 0)
                            {
                                String numsStr;
                                if (temp.size() > 1)
                                {
                                    String last = temp.remove(temp.size() - 1);
                                    temp.add("ou");
                                    temp.add(last);
                                    numsStr = join(temp, ", ").replace(", ou,", " ou");
                                }

                                else
                                    numsStr = temp.get(0);

                                htmlData += (gotFirstGroup > 1) ? "<br/>" : "";
                                htmlData += String.format(activity.getString(R.string.gui_data_questions),
                                                          numsStr, getPlural2(lastMin), lastMin);
                                temp.clear();
                            }

                            ++gotFirstGroup;
                        }

                        lastMin = qmin;
                        temp.add(String.valueOf(j));
                    }
                }

                htmlData += "<br/>" + bonusData;
            }

            htmlData += "</p>";
            applyColor(label, Html.fromHtml(htmlData).toString(), color);
        }

        return index;
    }

    // Helpers
    private String getPlural(float value)
    {
        return (value < 2) ? "" : "s";
    }

    private String getPlural2(float value)
    {
        return (value < 2) ? "ão" : "ões";
    }

    private void applyColor(TextView view, String htmlData, int color)
    {
        view.setText(htmlData.replace("$", ""), TextView.BufferType.SPANNABLE);
        Spannable str = (Spannable) view.getText();
        int i = htmlData.indexOf("$");
        if (i > -1)
            str.setSpan(new ForegroundColorSpan(color), i, htmlData.replace("$", "").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private String join(List<String> list, String conjunction)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : list)
        {
            if (first)
                first = false;
            else
                sb.append(conjunction);
            sb.append(item);
        }
        return sb.toString();
    }

    private float extractFloat(String value, float defaultValue)
    {
        try
        {
            return Float.parseFloat(value.replace(",", "."));
        }

        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }

    private class FrontData
    {
        public String name;
        public float[] grades_p1;
        public float[] grades_p2;
        public float[] grades_final;
        public float[] bonus;
    }

    public class EmptySubjectList extends Exception
    {

    }
}
