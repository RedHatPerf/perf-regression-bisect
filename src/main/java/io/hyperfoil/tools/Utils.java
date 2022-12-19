package io.hyperfoil.tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class Utils {

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileChannel source = new RandomAccessFile(sourceFile,"rw").getChannel();
             FileChannel destination = new RandomAccessFile(destFile,"rw").getChannel();) {

            long position = 0;
            long count    = source.size();

            source.transferTo(position, count, destination);
        }
    }
}
