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
    PointF mLastPosition = new PointF();
    OnTrackpadMovementListener mListener = null;

    public interface OnTrackpadMovementListener {
        public void onMove(float dx, float dy);
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
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastPosition.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - mLastPosition.x;
                float dy = event.getY() - mLastPosition.y;
                if (mListener != null)
                    mListener.onMove(dx, dy);
                mLastPosition.set(event.getX(), event.getY());
                break;
        }
        return true;
    }
}
