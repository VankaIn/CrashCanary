package com.github.vankain.crashcanary_library.log;


import com.github.vankain.crashcanary_library.CrashCanaryContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;

/**
 * 写文件线程
 * <p>
 * Created by markzhai on 2015/9/25.
 */
public class LogWriter {

    private static final Object SAVE_DELETE_LOCK = new Object();
    private static final SimpleDateFormat FILE_NAME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final long OBSOLETE_DURATION = 2 * 24 * 3600 * 1000;

    public static String saveLooperLog(String str) {
        String path;
        synchronized (SAVE_DELETE_LOCK) {
            path = saveLogToSDCard("crash", str);
        }
        return path;
    }

    public static String saveLogcatLog(String str) {
        String path;
        synchronized (SAVE_DELETE_LOCK) {
            path = saveLogToSDCard("logcat", str);
        }
        return path;
    }

    /**
     * 清除所有过期的文件，see {@code OBSOLETE_DURATION}
     */
    public static void cleanOldFiles() {
        CrashCanaryContext.get().getWriteLogFileThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                File[] f = CrashCanaryInternals.getLogFiles();
                if (f != null && f.length > 0) {
                    synchronized (SAVE_DELETE_LOCK) {
                        for (File aF : f) {
                            if (now - aF.lastModified() > OBSOLETE_DURATION) {
                                aF.delete();
                            }
                        }
                    }
                }
            }
        });
    }

    public static void deleteLogFiles() {
        synchronized (SAVE_DELETE_LOCK) {
            try {
                File[] files = CrashCanaryInternals.getLogFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private static String saveLogToSDCard(String logFileName, String str) {
        String path = "";
        BufferedWriter writer = null;
        try {
            File file = CrashCanaryInternals.detectedLeakDirectory();
            long time = System.currentTimeMillis();
            path = file.getAbsolutePath() + "/" + logFileName + "-" + FILE_NAME_FORMATTER.format(time) + ".txt";
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path, true), "UTF-8");

            writer = new BufferedWriter(out);
            writer.write("\r\n**********************\r\n");
            writer.write(TIME_FORMATTER.format(time) + "(write log time)");
            writer.write("\r\n");
            writer.write("\r\n");
            writer.write(str);
            writer.write("\r\n");
            writer.flush();
            writer.close();
            writer = null;
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                    writer = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return path;
    }

    public static File generateTempZipFile(String filename) {
        return new File(CrashCanaryInternals.getPath() + "/" + filename + ".log.zip");
    }
}
