package com.xiaomakj.androidcallpy;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.srplab.www.starcore.StarCoreFactory;
import com.srplab.www.starcore.StarCoreFactoryPath;
import com.srplab.www.starcore.StarObjectClass;
import com.srplab.www.starcore.StarServiceClass;
import com.srplab.www.starcore.StarSrvGroupClass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {
    private String CorePath = "/data/data/" + getPackageName() + "/files";
    public static MainActivity Host;
    public StarSrvGroupClass SrvGroup;
    private TextView result;
    private TextView result2;

    private void copyFile(Activity c, String Name, String desPath) throws IOException {
        File outfile = null;
        if (desPath != null)
            outfile = new File("/data/data/" + getPackageName() + "/files/" + desPath + Name);
        else
            outfile = new File("/data/data/" + getPackageName() + "/files/" + Name);
        //if (!outfile.exists()) {
        outfile.createNewFile();
        FileOutputStream out = new FileOutputStream(outfile);
        byte[] buffer = new byte[1024];
        InputStream in;
        int readLen = 0;
        if (desPath != null)
            in = c.getAssets().open(desPath + Name);
        else
            in = c.getAssets().open(Name);
        while ((readLen = in.read(buffer)) != -1) {
            out.write(buffer, 0, readLen);
        }
        out.flush();
        in.close();
        out.close();
        //}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result = ((TextView) findViewById(R.id.result));
        result2 = ((TextView) findViewById(R.id.result2));
        Host = this;

        File destDir = new File("/data/data/" + getPackageName() + "/files");
        if (!destDir.exists())
            destDir.mkdirs();
        File python2_7_libFile = new File("/data/data/" + getPackageName() + "/files/python3.4.zip");
        if (!python2_7_libFile.exists()) {
            try {
                copyFile(this, "python3.4.zip", null);
            } catch (Exception e) {
            }
        }
        try {
            copyFile(this, "_struct.cpython-34m.so", null);
            copyFile(this, "binascii.cpython-34m.so", null);
            copyFile(this, "time.cpython-34m.so", null);
            copyFile(this, "zlib.cpython-34m.so", null);
        } catch (Exception e) {
            System.out.println(e);
        }
        //----a test file to be read using python, we copy it to files directory
        try {
            copyFile(this, "test.txt", "");
            copyFile(this, "test_calljava.py", "");
            copyFile(this, "show.py", "");
        } catch (Exception e) {
            System.out.println(e);
        } 
        /*----load test.py----*/
        String pystring = null;
        try {
            AssetManager assetManager = getAssets();
            InputStream dataSource = assetManager.open("test.py");
            int size = dataSource.available();
            byte[] buffer = new byte[size];
            dataSource.read(buffer);
            dataSource.close();
            pystring = new String(buffer);
        } catch (IOException e) {
            System.out.println(e);
        }

        try {
            //--load python34 core library first;
            System.load(this.getApplicationInfo().nativeLibraryDir + "/libpython3.4m.so");
        } catch (UnsatisfiedLinkError ex) {
            System.out.println(ex.toString());
        }

        /*----init starcore----*/
        StarCoreFactoryPath.StarCoreCoreLibraryPath = this.getApplicationInfo().nativeLibraryDir;
        StarCoreFactoryPath.StarCoreShareLibraryPath = this.getApplicationInfo().nativeLibraryDir;
        StarCoreFactoryPath.StarCoreOperationPath = "/data/data/" + getPackageName() + "/files";

        StarCoreFactory starcore = StarCoreFactory.GetFactory();
        StarServiceClass Service = starcore._InitSimple("test", "123", 0, 0);
        SrvGroup = (StarSrvGroupClass) Service._Get("_ServiceGroup");
        Service._CheckPassword(false);

		/*----run python code----*/
        SrvGroup._InitRaw("python34", Service);
        final StarObjectClass python = Service._ImportRawContext("python", "", false, "");
        python._Call("import", "sys");

        StarObjectClass pythonSys = python._GetObject("sys");
        StarObjectClass pythonPath = (StarObjectClass) pythonSys._Get("path");
        pythonPath._Call("insert", 0, "/data/data/" + getPackageName() + "/files/python3.4.zip");
        pythonPath._Call("insert", 0, this.getApplicationInfo().nativeLibraryDir);
        pythonPath._Call("insert", 0, "/data/data/" + getPackageName() + "/files");
// 直接执行整个Python文件流
        python._Call("execute", pystring);
        Object testread = python._Call("testread", "/data/data/" + getPackageName() + "/files/test.txt");
        result.setText(testread + "");
//Python调用Java
        python._Set("JavaClass", CallBackClass.class);
        Service._DoFile("python", CorePath + "/test_calljava.py", "");
//Java调用Python
        //调用Python代码
        Service._DoFile("python", CorePath + "/show.py", "");
        result2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object stop = python._Call("stop", CorePath + "/show.py");
                result2.setText(stop + "");
            }
        });
        //result2.setText(testread + "");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
