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
import java.util.List;

import org.apache.log4j.Logger;
import org.opendaylight.controller.sal.reader.NodeDescription;

public class OPFSwitch {

        boolean assigned = false;
        // private String objectId = "";
        private long   ipAddress = 0;

        private String description = "";
        private String MACAddress = "";
        private String MFRDescription = "";
        private String HardwareDesc = "";
        private String SoftwareDesc = "";
        private String SerialNumber = "";
        private String DataPathDesc = "";

        private List<PortDescription> portDescList = new ArrayList<PortDescription>();

        private static final Logger logger = Logger.getLogger(TappingApp.class);

        public OPFSwitch() {
                this.assigned = false;
        }

        // Copy constructor
        public OPFSwitch(OPFSwitch sw) {
                super();

                // this.objectId = sw.getObjectId().toString();

                setDescription(sw.getDescription());
                setMACAddress(sw.getMACAddress());
                setMFRDescription(sw.getMFRDescription());
                setHardwareDesc(sw.getHardwareDesc());
                setSoftwareDesc(sw.getSoftwareDesc());
                setSerialNumber(sw.getSerialNumber());
                setDataPathDesc(sw.getDataPathDesc());
                setPortDescriptionList(sw.getPortDescriptionList());
        }

        public OPFSwitch(NodeDescription nodeDesc, String macAddr) {
             super();

             setDescription(nodeDesc.getDescription());
             setMACAddress(macAddr);
             setMFRDescription(nodeDesc.getManufacturer());
             setHardwareDesc(nodeDesc.getHardware());
             setSoftwareDesc(nodeDesc.getSoftware());
             setSerialNumber(nodeDesc.getSerialNumber());
             setDataPathDesc("Unknown");
        }

        public void setAssigned(boolean assigned) {
                this.assigned = assigned;
        }

        public boolean getAssigned() {
                return this.assigned;
        }

        private void setPortDescriptionList(List<PortDescription> portDescriptionList) {
                portDescList = portDescriptionList;
        }

        public List<PortDescription> getPortDescriptionList() {
                return portDescList;
        }

        public void setPortDescList(List<PortDescription> portDescriptionList) {
                this.portDescList = portDescriptionList;
        }

        public String getDescription() {
                return description;
        }

        public void setDescription(String description) {
                this.description = description;
        }

        public long getIPAddress() {
                return ipAddress;
        }

        public void setIPAddress(long ipAddress) {
                this.ipAddress = ipAddress;
        }

        public String getIPAddressStr() {
                try {
                        return InetAddress.getByAddress(unpack(this.ipAddress)).getHostAddress();
                } catch (UnknownHostException e) {
                        return "Unknown";
                }
        }

        public void setIPAddress(String dottedString) {
                try {
                        this.ipAddress = pack(InetAddress.getByName(dottedString).getAddress());
                } catch (UnknownHostException e) {
                        logger.warn("SwitchEntry: Can't resolve hostname " + dottedString);
                }
        }

        public String getMACAddress() {
                return MACAddress;
        }

        public void setMACAddress(String mACAddress) {
                MACAddress = mACAddress;
        }

        public String getMFRDescription() {
                return MFRDescription;
        }

        public void setMFRDescription(String mFRDescription) {
                MFRDescription = mFRDescription;
        }

        public String getHardwareDesc() {
                return HardwareDesc;
        }

        public void setHardwareDesc(String hardwareDesc) {
                HardwareDesc = hardwareDesc;
        }

        public String getSoftwareDesc() {
                return SoftwareDesc;
        }

        public void setSoftwareDesc(String softwareDesc) {
                SoftwareDesc = softwareDesc;
        }

        public String getSerialNumber() {
                return SerialNumber;
        }

        public void setSerialNumber(String serialNumber) {
                SerialNumber = serialNumber;
        }

        public String getDataPathDesc() {
                return DataPathDesc;
        }

        public void setDataPathDesc(String dataPathDesc) {
                DataPathDesc = dataPathDesc;
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

        public boolean isEqual(OPFSwitch opfSwitch) {
                return this.getDataPathDesc() == opfSwitch.getDataPathDesc();
        }

        public String toString() {
                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("OPFSwitch: ");
                stringBuilder.append(" IP Address: " + this.getIPAddressStr());
                stringBuilder.append(" Description: " + this.getDescription());
                stringBuilder.append(" Data path description: " + this.getDataPathDesc());
                stringBuilder.append(" Serial Number: " + this.getSerialNumber());
                stringBuilder.append(" MAC Address: " + this.getMACAddress());
                stringBuilder.append(" Manufacturer description: " + this.getMFRDescription());
                stringBuilder.append(" Hardware description: " + this.getHardwareDesc());
                stringBuilder.append(" Software description: " + this.getSoftwareDesc());
                stringBuilder.append((this.getAssigned() == true ? " is assigned" : " is not assigned"));

                String finalString = stringBuilder.toString();

                return finalString;
        }
}
