/* ONF SampleTap Software License

Copyright ©2014 Open Networking Foundation

This ONF SampleTap software is licensed under the Apache License, 
Version 2.0 (the "License"); you may not use this file except in 
compliance with the License. You may obtain a copy of the original
license at http://www.apache.org/licenses/LICENSE-2.0 and also in
the main directory of the source distribution.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.

End of ONF SampleTap Software License

*/


package org.opendaylight.controller.samples.onftappingapp;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;

public class AddressDesc {

        // Connect to the ODL slf4j logger
        protected static final Logger logger = LoggerFactory.getLogger(TappingApp.class);

        private String objectId = "";
        private long IPAddress;
        private long IPMask;
        private boolean usesDottedMask = false;

        public AddressDesc() {
                this.setIPAddress(0);
                this.setIPMask(0, false);
        }

        public AddressDesc(String value) {
                super();

                if (value.indexOf('/') == -1) {
                        this.setIPAddress(value);
                }
                else  {
                        String [] tokens = value.split("/");
                        if (tokens.length != 2)
                                throw new IllegalArgumentException();

                        this.setIPAddress(tokens[0]);
                        this.setIPMask(tokens[1]);
                }
        }

        public String getObjectId(){
                return objectId;
        }

        public void setObjectId(String objId){
                this.objectId = objId;
        }

        public long getIPAddress() {
                return IPAddress;
        }

        public void setIPAddress(long iPAddress) {
                IPAddress = iPAddress;
        }

        public long getIPMask() {
                return IPMask;
        }

        public void setIPMask(long iPMask, boolean usesDottedMask) {
                this.IPMask = iPMask;
                this.usesDottedMask = usesDottedMask;
        }

        private boolean usesDottedMask() {
                return this.usesDottedMask;
        }

        public void setIPAddress(String dottedString) throws IllegalArgumentException {
                try {
                        this.IPAddress = pack(InetAddress.getByName(dottedString).getAddress());
                } catch (UnknownHostException e) {
                        logger.error("Can't resolve host name " + dottedString);
                        throw new IllegalArgumentException();
                }
        }

        public String getIPAddressStr() {
                try {
                        return InetAddress.getByAddress(unpack(this.IPAddress)).getHostAddress();
                } catch (UnknownHostException e) {
                        return "Unknown";
                }
        }

        public void setIPMask(String maskStr) throws  IllegalArgumentException {
                // Does it look like a well formed dotted string?
                int dots = 0;
                for (int i = 0; i < maskStr.length(); i++ ) {
                        if (maskStr.charAt(i) == '.') dots++;
                }
                if (dots == 3) {
                        try {
                                this.setIPMask(pack(InetAddress.getByName(maskStr).getAddress()), true);
                        } catch (UnknownHostException e) {
                        throw new IllegalArgumentException();
                        }
                }
                else {
                        this.setIPMask(Integer.parseInt(maskStr), false);
                }
        }

        public String getIPMaskStr() {
                if (this.usesDottedMask()) {
                        try {
                                return InetAddress.getByAddress(unpack(this.IPMask)).getHostAddress();
                        } catch (UnknownHostException e) {
                                return "Unknown";
                        }
                }
                else {
                        return (Long.toString(this.getIPMask()));
                }
        }

        private long pack(byte[] bytes) {
                  int val = 0;
                  for (int i = 0; i < bytes.length; i++) {
                    val <<= 8;
                    val |= bytes[i] & 0xff;
                  }
                  return val;
        }

        private byte[] unpack(long bytes) {
            return new byte[] {
                    (byte)((bytes >>> 24) & 0xff),
                    (byte)((bytes >>> 16) & 0xff),
                    (byte)((bytes >>>  8) & 0xff),
                    (byte)((bytes       ) & 0xff)
                  };
        }

        public BasicDBObject getAsDocument() {
                BasicDBObject document = new BasicDBObject();
                document.put("IPAddress", this.getIPAddressStr());
                document.put("IPMask",    this.getIPMaskStr());

                return document;
        }

        public String toString() {
                if (this.getIPMask() == 0)
                        return (this.getIPAddressStr());
                else {
                        return (this.getIPAddressStr() + "/" + this.getIPMaskStr());
                }
        }
}
