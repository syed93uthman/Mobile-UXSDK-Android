package com.ikramatic.dronemonitor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class JsonHandler {
    public JsonHandler(){}

    public JSONObject createJson(InputStream is){
        try {
            JSONObject json = new JSONObject(loadJSONFromAsset(is));
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String loadJSONFromAsset(InputStream is) {
        String json = null;
        try {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
