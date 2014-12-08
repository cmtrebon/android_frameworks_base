/*
 * Copyright 2014 Bassel Bakr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class Flash
{

    private static String sFlashDevice, sFlashMode;
    private static FileWriter sFileWriter;
    private static boolean sOn;

    static {
        setFlashDevice("/sys/devices/virtual/camera/rear/rear_flash");
    }

    // Set flash device
    public static void setFlashDevice(String sFlashDevice)
    {
        Flash.sFlashDevice = sFlashDevice;
        try {
            if (sFileWriter != null) {
                sFileWriter.close();
            }
            sFileWriter = null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            sFileWriter = new FileWriter(sFlashDevice);
        }
        catch (IOException e) {
            /*
             * It's unlikely to get thrown inside this block unless our flash device's group isn't camera (1006)
             * So we need to change its group to camera (1006)
             */

            fixGroup();
            try {
                sFileWriter = new FileWriter(sFlashDevice);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    // Change flash device group to camera (1006)
    public static void fixGroup()
    {
        try {
            Process proc = new ProcessBuilder("su").start();
            PrintWriter stdin = new PrintWriter(proc.getOutputStream());
            stdin.write(String.format("busybox chown 1000:1006 %s\n", sFlashDevice));
            stdin.flush();
            stdin.write("exit\n");
            stdin.flush();
            proc.waitFor();
            stdin.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Are we using auto-flash?
    public static boolean isAuto()
    {
        return getFlashMode().contains("auto");
    }

    // Return current flash state
    public static boolean isOn()
    {
        return sOn;
    }

    // Turn flash on ( the same goes to off() )
    static void on()
    {
        if (isOn()) {
            return;
        }
        try {
            // Write "1" to turn it on
            sFileWriter.write(String.valueOf(1));
            sFileWriter.flush();

            // Let's do this to keep track of flash state
            sOn = true;
        }
        catch (IOException e) {
            fixGroup();
            e.printStackTrace();
        }
    }

    static void off()
    {
        if (!isOn()) {
            return;
        }
        try {
            // Write "0" to turn it on
            sFileWriter.write(String.valueOf(0));
            sFileWriter.flush();

            // Let's do this to keep track of flash state
            sOn = false;
        }
        catch (IOException e) {
            fixGroup();
            e.printStackTrace();
        }
    }

    // Return current flash mode set by the camera or flashlight app
    static String getFlashMode()
    {
        if (sFlashMode == null) {
            return "off";
        }
        return sFlashMode;
    }

    // Keep track of flash mode
    static void changeFlashMode(final String mode)
    {
        sFlashMode = mode;
    }

    // This will be called when an app tries to change flash mode
    static void setFlashMode(final String mode)
    {
        if( (mode == Camera.Parameters.FLASH_MODE_ON) || (mode == Camera.Parameters.FLASH_MODE_TORCH) ) {
		      on();
	      } else {
	      	off();
      	}

        // Let's do this to keep track of current flash mode set by app
        sFlashMode = mode;
    }
}
