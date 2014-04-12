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

public class PortDescription {
        private String objectId = "";
        private String name = "";
        private String macAddress = "";
        private int portNumber;

        public PortDescription(){
        }

        public PortDescription(String name, String macAddr, int portNum) {
                this.setName(name);
                this.setMacAddress(macAddr);
                this.setPortNumber(portNum);
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

        public String getMacAddress() {
                return macAddress;
        }

        public void setMacAddress(String macAddress) {
                this.macAddress = macAddress;
        }

        public int getPortNumber() {
                return portNumber;
        }

        public void setPortNumber(int portNumber) {
                this.portNumber = portNumber;
        }

}
