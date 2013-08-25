/*
 * Copyright (C) 2013 Clark Scheff
 *
 * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scheffsblend.btmouse;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BtMouseActivity extends Activity implements TrackPad.OnTrackpadMovementListener,
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "BtShiznit";
    private static final String UUID_SPP = "00001101-0000-1000-8000-00805f9b34fb";
    private static final int REQUEST_ENABLE_BT = 42;
    private static final char COMMAND_START = '@';

    private UUID mUuid = UUID.fromString(UUID_SPP);

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBtSocket;
    private OutputStream mBtOutputStream;
    private boolean mLeftPressed = false;
    private boolean mRightPressed = false;
    private float mSensitivty = 1f;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mouse_pad);

        TrackPad trackPad = (TrackPad) findViewById(R.id.track_pad);
        trackPad.setOnTrackpadMovementListener(this);

        findViewById(R.id.left_button).setOnTouchListener(mButtonListener);
        findViewById(R.id.right_button).setOnTouchListener(mButtonListener);

        SeekBar seekBar = (SeekBar) findViewById(R.id.sensitivity);
        seekBar.setOnSeekBarChangeListener(this);
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        seekBar.setProgress(prefs.getInt("sensitivity", 50));

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
            finish();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            setupBluetooth();
        }
    }

    View.OnTouchListener mButtonListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (id == R.id.left_button)
                        mLeftPressed = true;
                    else
                        mRightPressed = true;
                    try {
                        sendData((short)0, (short)0, mLeftPressed, mRightPressed, false);
                    } catch (IOException e) {
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (id == R.id.left_button)
                        mLeftPressed = false;
                    else
                        mRightPressed = false;
                    try {
                        sendData((short)0, (short)0, mLeftPressed, mRightPressed, false);
                    } catch (IOException e) {
                    }
                    break;
            }
            return true;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtSocket != null && mBtSocket.isConnected()) {
            if (mBtOutputStream != null) {
                try {
                    mBtOutputStream.close();
                } catch (IOException e) {
                }
                mBtOutputStream = null;
            }
            try {
                mBtSocket.close();
            } catch (IOException e) {
            }
            mBtSocket = null;
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    setupBluetooth();
                } else {
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void setupBluetooth() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.select_dialog_singlechoice);
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "device: " + device.getName());
                adapter.add(device.getName());
            }
            chooseDevice(adapter);
        }
    }

    private void chooseDevice(final ArrayAdapter<String> adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select device");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        if (adapter.getItem(which).equals(device.getName())) {
                            (new ConnectThread(device)).start();
                            break;
                        }
                    }
                }
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onMove(float dx, float dy, boolean scroll) {
        if (!scroll) {
            dx *= mSensitivty;
            dy *= mSensitivty;
        }
        if (mBtSocket != null && mBtSocket.isConnected()) {
            try {
                sendData((short)dx, (short)dy, mLeftPressed, mRightPressed, scroll);
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void onClick() {
        try {
            sendData((short)0, (short)0, true, mRightPressed, false);
            sendData((short)0, (short)0, false, mRightPressed, false);
        } catch (IOException e) {
        }
    }

    private void sendData(short x, short y, boolean leftPressed, boolean rightPressed, boolean scroll)
            throws IOException {
        byte[] data = new byte[6];
        // [0] start byte
        data[0] = COMMAND_START;
        // [1:2] dx - least significant byte first
        data[1] = (byte)(x & 0xFF);
        data[2] = (byte)(x>>8 & 0xFF);
        // [3:4] dy - least significant byte first
        data[3] = (byte)(y & 0xFF);
        data[4] = (byte)(y>>8 & 0xFF);
        // [5] scroll and left/right pressed state packed into last byte
        data[5] = (byte)((scroll ? 4 : 0) | (leftPressed ? 2 : 0) | (rightPressed ? 1 : 0));
        mBtOutputStream.write(data);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mSensitivty = 1f + (progress/100f * 3f);

        // store this value so we can restore it when the application runs again
        getPreferences(MODE_PRIVATE).edit()
        .putInt("sensitivity", progress)
        .commit();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // mUuid is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(mUuid);
            } catch (IOException e) { }
            mBtSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mBtSocket.connect();
                mBtOutputStream = mBtSocket.getOutputStream();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mBtSocket.close();
                    mBtSocket = null;
                } catch (IOException closeException) { }
                return;
            }
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mBtSocket.close();
                mBtSocket = null;
            } catch (IOException e) { }
        }
    }
}
