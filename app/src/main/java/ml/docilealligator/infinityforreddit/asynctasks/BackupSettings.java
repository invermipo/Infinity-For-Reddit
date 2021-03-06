package ml.docilealligator.infinityforreddit.asynctasks;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;

import androidx.documentfile.provider.DocumentFile;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;

import ml.docilealligator.infinityforreddit.BuildConfig;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.utils.CustomThemeSharedPreferencesUtils;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;

public class BackupSettings {
    public static void backupSettings(Context context, Executor executor, Handler handler,
                                      ContentResolver contentResolver, Uri destinationDirUri,
                                      SharedPreferences defaultSharedPreferences,
                                      SharedPreferences lightThemeSharedPreferences,
                                      SharedPreferences darkThemeSharedPreferences,
                                      SharedPreferences amoledThemeSharedPreferences,
                                      SharedPreferences sortTypeSharedPreferences,
                                      SharedPreferences postLayoutSharedPreferences,
                                      SharedPreferences postFeedScrolledPositionSharedPreferences,
                                      SharedPreferences mainActivityTabsSharedPreferences,
                                      SharedPreferences nsfwAndSpoilerSharedPreferencs,
                                      SharedPreferences bottomAppBarSharedPreferences,
                                      SharedPreferences postHistorySharedPreferences,
                                      BackupSettingsListener backupSettingsListener) {
        executor.execute(() -> {
            boolean res = saveSharedPreferencesToFile(context, defaultSharedPreferences,
                    SharedPreferencesUtils.DEFAULT_PREFERENCES_FILE);
            boolean res1 = saveSharedPreferencesToFile(context, lightThemeSharedPreferences,
                    CustomThemeSharedPreferencesUtils.LIGHT_THEME_SHARED_PREFERENCES_FILE);
            boolean res2 = saveSharedPreferencesToFile(context, darkThemeSharedPreferences,
                    CustomThemeSharedPreferencesUtils.DARK_THEME_SHARED_PREFERENCES_FILE);
            boolean res3 = saveSharedPreferencesToFile(context, amoledThemeSharedPreferences,
                    CustomThemeSharedPreferencesUtils.AMOLED_THEME_SHARED_PREFERENCES_FILE);
            boolean res4 = saveSharedPreferencesToFile(context, sortTypeSharedPreferences,
                    SharedPreferencesUtils.SORT_TYPE_SHARED_PREFERENCES_FILE);
            boolean res5 = saveSharedPreferencesToFile(context, postLayoutSharedPreferences,
                    SharedPreferencesUtils.POST_LAYOUT_SHARED_PREFERENCES_FILE);
            boolean res6 = saveSharedPreferencesToFile(context, postFeedScrolledPositionSharedPreferences,
                    SharedPreferencesUtils.FRONT_PAGE_SCROLLED_POSITION_SHARED_PREFERENCES_FILE);
            boolean res7 = saveSharedPreferencesToFile(context, mainActivityTabsSharedPreferences,
                    SharedPreferencesUtils.MAIN_PAGE_TABS_SHARED_PREFERENCES_FILE);
            boolean res8 = saveSharedPreferencesToFile(context, nsfwAndSpoilerSharedPreferencs,
                    SharedPreferencesUtils.NSFW_AND_SPOILER_SHARED_PREFERENCES_FILE);
            boolean res9 = saveSharedPreferencesToFile(context, bottomAppBarSharedPreferences,
                    SharedPreferencesUtils.BOTTOM_APP_BAR_SHARED_PREFERENCES_FILE);
            boolean res10 = saveSharedPreferencesToFile(context, postHistorySharedPreferences,
                    SharedPreferencesUtils.POST_HISTORY_SHARED_PREFERENCES_FILE);

            boolean zipRes = zipAndMoveToDestinationDir(context, contentResolver, destinationDirUri);

            try {
                FileUtils.deleteDirectory(new File(context.getExternalCacheDir() + "/Backup/"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            handler.post(() -> {
                boolean finalResult = res && res1 && res2 && res3 && res4 && res5 && res6 && res7 && res8 && res9 && res10 && zipRes;
                if (finalResult) {
                    backupSettingsListener.success();
                } else {
                    if (!zipRes) {
                        backupSettingsListener.failed(context.getText(R.string.create_zip_in_destination_directory_failed).toString());
                    } else {
                        backupSettingsListener.failed(context.getText(R.string.backup_some_settings_failed).toString());
                    }
                }
            });
        });
    }

    private static boolean saveSharedPreferencesToFile(Context context, SharedPreferences sharedPreferences,
                                                       String fileName) {

        boolean result = false;
        ObjectOutputStream output = null;
        String backupDir = context.getExternalCacheDir() + "/Backup/" + BuildConfig.VERSION_NAME;
        if (!new File(backupDir).exists()) {
            new File(backupDir).mkdirs();
        } else {
            File backupDirFile = new File(backupDir);
            backupDirFile.delete();
            backupDirFile.mkdirs();
        }

        try {
            output = new ObjectOutputStream(new FileOutputStream(new File(backupDir + "/" + fileName + ".txt")));
            output.writeObject(sharedPreferences.getAll());

            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    private static boolean zipAndMoveToDestinationDir(Context context, ContentResolver contentResolver, Uri destinationDirUri) {
        OutputStream outputStream = null;
        boolean result = false;
        try {
            String time = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(System.currentTimeMillis()));
            String fileName = "Infinity_For_Reddit_Settings_Backup_v" + BuildConfig.VERSION_NAME + "-" + time + ".zip";
            String filePath = context.getExternalCacheDir() + "/Backup/" + fileName;
            ZipFile zip = new ZipFile(filePath, "123321".toCharArray());
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(EncryptionMethod.AES);
            zip.addFolder(new File(context.getExternalCacheDir() + "/Backup/" + BuildConfig.VERSION_NAME + "/"), zipParameters);

            DocumentFile dir = DocumentFile.fromTreeUri(context, destinationDirUri);
            if (dir == null) {
                return false;
            }
            DocumentFile checkForDuplicate = dir.findFile(fileName);
            if (checkForDuplicate != null) {
                checkForDuplicate.delete();
            }
            DocumentFile destinationFile = dir.createFile("application/zip", fileName);
            if (destinationFile == null) {
                return false;
            }

            outputStream = contentResolver.openOutputStream(destinationFile.getUri());
            if (outputStream == null) {
                return false;
            }

            byte[] fileReader = new byte[1024];

            FileInputStream inputStream = new FileInputStream(filePath);
            while (true) {
                int read = inputStream.read(fileReader);

                if (read == -1) {
                    break;
                }

                outputStream.write(fileReader, 0, read);
            }
            result = true;
        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public interface BackupSettingsListener {
        void success();
        void failed(String errorMessage);
    }
}
