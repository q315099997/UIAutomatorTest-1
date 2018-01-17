package unibg.uiautomatortest;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.util.DisplayMetrics;
import android.util.Log;

import unibg.accessibilitytestgenerator.ATGImageUtilities;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class Test4 {

    private static final String PACKAGE_NAME
            = "unibg.testapp1";
    private static final int LAUNCH_TIMEOUT = 5000;
    private UiDevice mDevice;
    private UiObject2 targetView = null;


    @Before
    public void startMainActivityFromHomeScreen() {

        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Start from the home screen
        mDevice.pressHome();

        // Wait for launcher
        final String launcherPackage = mDevice.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)),
                LAUNCH_TIMEOUT);

        // Launch the app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(PACKAGE_NAME);
        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)),
                LAUNCH_TIMEOUT);


         


        File dir = new File(Environment.getExternalStorageDirectory() + "/UIAccessibilityTests/");
        dir.mkdirs();
        File file = new File(dir, "screenshot.png");
        mDevice.takeScreenshot(file);

        List<UiObject2> list = mDevice.findObjects(By.clazz("android.widget.Button"));


       for (UiObject2 obj : list) {

            boolean text = obj.getText()==null;
            boolean resourceId = obj.getResourceName()!=null && obj.getResourceName().equals("unibg.testapp1:id/button5");
            boolean packageName = obj.getApplicationPackage()!=null && obj.getApplicationPackage().equals("unibg.testapp1");
            boolean contentDesc = obj.getContentDescription()==null;
            boolean bounds = obj.getVisibleBounds()!=null && obj.getVisibleBounds().toString().equals("Rect(733, 1458 - 964, 1584)");

            if (text && resourceId && packageName && contentDesc && bounds) {
                targetView = obj;
            }


        }

    }


    @Test
    public void testContrast() {
        Rect bounds = targetView.getVisibleBounds();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/UIAccessibilityTests/screenshot.png", options);
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(Environment.getExternalStorageDirectory() + "/UIAccessibilityTests/cropped.png");
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        double contrastRatio = ATGImageUtilities.contrastRatio(Environment.getExternalStorageDirectory() + "/UIAccessibilityTests/screenshot.png", targetView.getVisibleBounds());
        Log.d("CONTRASTO", Double.toString(contrastRatio));
        File file = new File(Environment.getExternalStorageDirectory() + "/UIAccessibilityTests/screenshot.png");
        file.delete();
		assertFalse(contrastRatio < 3);
    }


}