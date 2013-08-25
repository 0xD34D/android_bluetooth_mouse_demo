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

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Simple view that provides changes in x and y movement to simulate a
 * touchpad pointing device.
 *
 * @author Clark Scheff
 */
public class TrackPad extends View implements View.OnTouchListener {
    private static long CLICK_TIME_MS = 300;

    PointF mLastPosition = new PointF();
    OnTrackpadMovementListener mListener = null;
    private int mPointerCount = 0;
    private long mLastDownTime = System.currentTimeMillis();

    public interface OnTrackpadMovementListener {
        public void onMove(float dx, float dy, boolean scroll);
        public void onClick();
    }

    public TrackPad(Context context) {
        this(context, null);
    }

    public TrackPad(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrackPad(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnTouchListener(this);
    }

    public void setOnTrackpadMovementListener(OnTrackpadMovementListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mPointerCount = event.getPointerCount();
        long time = System.currentTimeMillis();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                if (mPointerCount < 2) {
                    mLastPosition.set(event.getX(), event.getY());
                    mLastDownTime = System.currentTimeMillis();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = 0f;
                float dy = 0f;
                if (mPointerCount < 2) {
                    dx = event.getX() - mLastPosition.x;
                    dy = event.getY() - mLastPosition.y;
                    mLastPosition.set(event.getX(), event.getY());
                } else {
                    dx = event.getX(0) - mLastPosition.x;
                    dy = event.getY(0) - mLastPosition.y;
                    mLastPosition.set(event.getX(0), event.getY(0));
                }
                if (mListener != null)
                    mListener.onMove(dx, dy, mPointerCount > 1);
                break;
            case MotionEvent.ACTION_UP:
                if (mPointerCount < 2 && (time - mLastDownTime) <= CLICK_TIME_MS) {
                    if (mListener != null)
                        mListener.onClick();
                }
                break;
        }
        return true;
    }
}
