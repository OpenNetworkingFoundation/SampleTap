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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;

public class RewriteParams {

        private String objectId = "";
        private String name = "";
        private long ipAddress;
        private int vlanId;
        private String macAddr = "";

        // Values to match (enabled by a bit in the bitMask attribute)
        List<EntryField> entryFieldList = new ArrayList<EntryField>();

        private static final Logger logger = Logger.getLogger(TappingApp.class);

        public RewriteParams() {
        }

        public RewriteParams(BasicDBObject objDoc) throws UnknownHostException {
                this.setName(objDoc.getString("name"));
                this.setIPAddress((String) objDoc.get("IPAddress"));
                this.setMacAddr((String) objDoc.getString("MACAddress"));
                this.setVlanId((int) objDoc.getInt("VLAN_ID"));
        }

        public String getObjectId() {
                return objectId;
        }

        public void setObjectId(String objectId) {
                this.objectId = objectId;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public long getIpAddr() {
                return ipAddress;
        }

        public void setIpAddr(long ipAddress) {
                this.ipAddress = ipAddress;
        }

        public void setIPAddress(String dottedString) throws UnknownHostException {
                this.ipAddress = pack(InetAddress.getByName(dottedString).getAddress());
        }

        public int getVlanId() {
                return vlanId;
        }

        public void setVlanId(int vlanId) {
                this.vlanId = vlanId;
        }

        public String getMacAddr() {
                return macAddr;
        }

        public void setMacAddr(String macAddr) {
                this.macAddr = macAddr;
        }

        public List<EntryField> getFields() {

                 List<EntryField> cloneList = new ArrayList<EntryField>(entryFieldList.size());

                 Iterator<EntryField> it = entryFieldList.iterator();

                 while(it.hasNext()){
                         EntryField field = it.next();
                         cloneList.add(field);
                 }

                 return cloneList;
        }

        // Construct the entry fields from a list of EntryField key/value pairs
        public void setFields(List<EntryField> entryFields)
        {
                Iterator<EntryField> it = entryFields.iterator();

                while (it.hasNext()) {
                        EntryField entryField = it.next();
                        logger.info(entryField.toString());

                        // Copy the match criteria into the list and set the bit mask to indicate it is part of the match
                        switch(entryField.getType()) {
                        case EntryField.UNDEFINED:
                                // Ignored
                                break;

                        case EntryField.IP_ADDR:
                        case EntryField.MAC_ADDR:
                        case EntryField.VLAN_ID:
                                entryFieldList.add(entryField);
                                break;

                        default:
                                logger.warn("Unhandled Entry Field Type");
                                break;
                        }
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

        public String getIPAddressStr() {
                try {
                        return InetAddress.getByAddress(unpack(this.ipAddress)).getHostAddress();
                } catch (UnknownHostException e) {
                        return "Unknown";
                }
        }

        public BasicDBObject getAsDocument() {
                BasicDBObject document = new BasicDBObject();

                document.put("name",          this.getName());
                document.put("IPAddress",     this.getIPAddressStr());
                document.put("VLAN_ID",       this.getVlanId());
                document.put("MACAddress",    this.getMacAddr());

                return document;
        }
}
