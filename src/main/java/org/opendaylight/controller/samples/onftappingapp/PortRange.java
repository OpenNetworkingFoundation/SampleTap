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

import com.mongodb.BasicDBObject;

public class PortRange {

        private String objectId = "";
        private short startPort;
        private short endPort;

        public PortRange() {
        }

        public PortRange(String objectId, short startPort, short endPort) {
                this.objectId = objectId;
                setStartPort(startPort);
                setEndPort(endPort);
        }

        public PortRange(String portRangeStr) throws IllegalArgumentException {
                String [] ports = portRangeStr.split("-");
                switch (ports.length) {
                case 1:
                        this.setStartPort(Short.parseShort(ports[0].trim()));
                        this.setEndPort((short) 0);
                        return;

                case 2:
                        this.setStartPort(Short.parseShort(ports[0].trim()));
                        this.setEndPort(Short.parseShort(ports[1].trim()));
                        return;

                default:
                        throw new IllegalArgumentException();
                }
        }

        public String getObjectId(){
                return objectId;
        }

        public short getStartPort() {
                return startPort;
        }

        public void setStartPort(short startPort) {
                this.startPort = startPort;
        }

        public short getEndPort() {
                return endPort;
        }

        public void setEndPort(short endPort) {
                this.endPort = endPort;
        }

        public BasicDBObject getAsDocument() {
                BasicDBObject document = new BasicDBObject();
                document.put("startPort", this.getStartPort());
                document.put("endPort",   this.getEndPort());

                return document;
        }

        public String toString() {
                if (this.getEndPort() > 0)
                        return Integer.toString(this.getStartPort()) + "-" + Integer.toString(this.getEndPort());
                else
                        return Integer.toString(this.getStartPort());
        }
}
