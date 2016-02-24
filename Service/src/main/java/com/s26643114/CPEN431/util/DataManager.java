package com.s26643114.CPEN431.util;

import java.io.File;
import java.io.IOException;

public class DataManager {
    public DataManager(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists())
            file.createNewFile();
    }


}
