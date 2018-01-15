package unibg.uiautomatortest.unused;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class CrawlerWithGraph {

    private static String PACKAGE_NAME;
    private static final int LAUNCH_TIMEOUT = 5000;
    private UiDevice mDevice;
    private List<String> classes = new ArrayList<>();
    int i = 0;
    StringBuilder stringBuilder = new StringBuilder();

    @Before
    public void startMainActivityFromHomeScreen() throws FileNotFoundException {


        // Checked widgets
        classes.add("android.widget.TextView");
        classes.add("android.widget.ImageView");
        classes.add("android.widget.ImageButton");
        classes.add("android.widget.Button");
        classes.add("android.widget.CheckedTextView");


        try {
            PACKAGE_NAME = readApkName();
        } catch (IOException e) {
            Log.e("NO PACKAGE NAME","SELECT PACKAGE FROM MAIN APP");
            e.printStackTrace();
            System.exit(1);
        }

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



    }


    @Test
    public void test(){
        populateEditText();
        List<UiObject2> lista = mDevice.findObjects(By.pkg(PACKAGE_NAME));
        WindowStatus status0 = new WindowStatus(lista);

        ListGraph.addVisitedStatus(status0);
        ListGraph.status0 = status0;

        // TODO: CONTROLLA ECCEZIONI
        try {
            crawl(status0);
        } catch (IOException e) {
            e.printStackTrace();
        }


        stringBuilder.append("\n\n");
        for (int i : ListGraph.getPaths().keySet()) {
            stringBuilder.append("CAMMINO PER STATUS " + i + "\n");
            for (Transition t : ListGraph.getPath(i)) {
                stringBuilder.append(t.toString() + "\n");
            }
        }


        try {
            File dir = new File(Environment.getExternalStorageDirectory() + "/UIAccessibilityTests/logs/");
            dir.mkdirs();
            File file = new File(dir, "log.txt");
            FileOutputStream fos = null;
            fos = new FileOutputStream(file);
            byte[] data = stringBuilder.toString().getBytes();
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void crawl(WindowStatus status) throws IOException {

        stringBuilder.append("CRAWL WINDOW " + Integer.toString(status.getNumber()) + "\n");
        populateEditText();

        for (Node node : status.getNodes()) {

            if (classes.contains(node.getClassName())) {
                TestCaseGenerator.generateTestCase(PACKAGE_NAME, node, "Testerino" + i, status.getNumber());
                stringBuilder.append("TEST DI " + node.toString() + "\n");
                i++;
            }


            if (node.isClickable() || node.isCheckable()) {
                status.getTransitions().add(new Transition(UIActions.CLICK, node));
                stringBuilder.append("AGGIUNTA TRANSIZIONE " + node.toString() + "\n");
            }


           /* if (node.isScrollable()) {
                status.getTransitions().add(new Transition(UIActions.LONG_CLICK, node));
            }


            if (node.isLong_clickable()) {
                status.getTransitions().add(new Transition(UIActions.SCROLL, node));
            }*/

        }

        for (Transition transition : status.getTransitions()) {
            Node node = transition.getNode();
            stringBuilder.append("TRANSIZIONE status: " + status.getNumber() + " " + transition.toString());
            switch (transition.getAction()) {

                case CLICK:
                    stringBuilder.append("CLICK " + node.toString());
                    mDevice.click(node.getBounds().centerX(), node.getBounds().centerY());
                    mDevice.waitForWindowUpdate(PACKAGE_NAME, LAUNCH_TIMEOUT);
                    populateEditText();
                    List<UiObject2> afterList = mDevice.findObjects(By.pkg(PACKAGE_NAME));
                    WindowStatus statusAfter = new WindowStatus(afterList);
                    if (!ListGraph.getVisitedStatuses().contains(statusAfter)) {

                        ListGraph.addVisitedStatus(statusAfter);
                        stringBuilder.append("NUOVO STATUS NUMERO " + statusAfter.getNumber());
                        takeScreenshot(statusAfter);
                        List<Transition> transitionList = new ArrayList<>();

                        if (status.getNumber() != 0)
                            transitionList.addAll(ListGraph.getPath(status.getNumber()));

                        transitionList.add(transition);
                        ListGraph.addPath(statusAfter.getNumber(), transitionList);
                        crawl(statusAfter);
                    } else {
                        stringBuilder.append("STATUS GIA' ESAMINATO");
                    }
                    break;

            }
            if (!currentStatus().equals(status)) {

                comeBackToStatus(status);
            } else {
                stringBuilder.append("STATUS CORRENTE CORRETTO");
            }

        }

        //closeAndOpenApp();

    }

    private String readApkName() throws IOException {

        File dir = Environment.getExternalStorageDirectory();

        //Get the text file
        File file = new File(dir + "/UIAutomatorTest/apk/", "apk");

        //Read text from file
        StringBuilder text = new StringBuilder();


        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        while ((line = br.readLine()) != null) {
            text.append(line);
        }
        br.close();


        Log.d("APK NAME", text.toString());

        return text.toString();
    }



    private void takeScreenshot(WindowStatus status) {
        File dir = new File(Environment.getExternalStorageDirectory() + "/UIAccessibilityTests/screenshots/");
        dir.mkdirs();
        File file = new File(dir, "status" + status.getNumber() + ".png");
        mDevice.takeScreenshot(file);
    }

    private WindowStatus currentStatus() {
        return new WindowStatus(mDevice.findObjects(By.pkg(PACKAGE_NAME)));
    }

    private void populateEditText() {
        List<UiObject2> editTextViews = mDevice.findObjects(By.clazz("android.widget.EditText"));

        if (editTextViews.size() != 0) {
            for (UiObject2 obj : editTextViews) {
                obj.setText("test");
            }
        }
    }

    private void comeBackToStatus(WindowStatus status) {

        if (!currentStatus().equals(ListGraph.status0)) {
            closeAndOpenApp();
        }

        populateEditText();
        List<Transition> transitions = ListGraph.getPath(status.getNumber());

        if (transitions != null) {
            for (Transition t : transitions) {
                Node node = t.getNode();
                switch (t.getAction()) {

                    case CLICK:
                        mDevice.click(node.getBounds().centerX(), node.getBounds().centerY());
                        mDevice.waitForWindowUpdate(PACKAGE_NAME, LAUNCH_TIMEOUT);
                        populateEditText();
                        break;
                }
            }
        }


    }

    private void closeAndOpenApp() {

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


    }


}