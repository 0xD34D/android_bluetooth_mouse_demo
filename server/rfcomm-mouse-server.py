#
# Copyright (C) 2013 Clark Scheff
#
# Licensed under the GNU GPLv2 license
#
# The text of the license can be found in the LICENSE file
# or at https://www.gnu.org/licenses/gpl-2.0.txt
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
# file: rfcomm-mouse-server.py
# auth: Clark Scheff
# desc: a server using RFCOMM sockets and PyMouse to control the mouse
#       using a bluetooth client
#
# dependencies:
#       PyBluez - https://code.google.com/p/pybluez/
#       PyMouse - https://github.com/pepijndevos/PyMouse
#       XLib (Linux only) - http://python-xlib.sourceforge.net/

from bluetooth import *
from struct import *
from pymouse import PyMouse

# the data from the client should start with this
START_BYTE = '@'

# run for ever until Control-C is pressed or catastrophic failure occurs
while True:
    server_sock=BluetoothSocket( RFCOMM )
    server_sock.bind(("",PORT_ANY))
    server_sock.listen(1)

    port = server_sock.getsockname()[1]

    # use the well-known SPP UUID
    uuid = "00001101-0000-1000-8000-00805f9b34fb"

    advertise_service( server_sock, "BtMouseServer",
                       service_id = uuid,
                       service_classes = [ uuid, SERIAL_PORT_CLASS ],
                       profiles = [ SERIAL_PORT_PROFILE ] 
                     )
                   
    print "Waiting for connection on RFCOMM channel %d" % port
    client_sock, client_info = server_sock.accept()
    print "Accepted connection from ", client_info

    try:
        leftPressed = False
        rightPressed = False
        m = PyMouse()
        while True:
            data = client_sock.recv(6)
            if len(data) == 0: break
            if data[0] == START_BYTE:
                position = m.position()
                movement = unpack('hh', data[1:5])
                left = ord(data[5]) & 2 != 0
                right = ord(data[5]) & 1 != 0
                if leftPressed != left:
                    leftPressed = left
                    if left:
                        m.press(position[0], position[1], 1)
                    else:
                        m.release(position[0], position[1], 1)
                if rightPressed != right:
                    rightPressed = right
                    if right:
                        m.press(position[0], position[1], 2)
                    else:
                        m.release(position[0], position[1], 2)
                if movement[0] != 0 or movement[1] != 0:
                    m.move(position[0] + movement[0], position[1] + movement[1])
    except IOError:
        pass

    print "disconnected"

    client_sock.close()
    server_sock.close()

print "all done"
