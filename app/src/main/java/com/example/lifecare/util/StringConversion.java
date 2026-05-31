package com.example.lifecare.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StringConversion {

    public String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String line;

        while (true) {
            line = reader.readLine();
            if (line == null)
                break;

            builder.append(line);
        }

        reader.close();
        return builder.toString();

    }
}