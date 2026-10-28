package com.notebook.notes.checklist.util;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public class Utils {
        public static Context wrapContext(Context context) {


        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        return context.createConfigurationContext(configuration);
    }

}
