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

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class SwitchAndPort {

        private String name = "";
        private ObjectId switchId;
        private int switchPort = 0;

        public SwitchAndPort() {
        }

        public SwitchAndPort(String name, String switchEntryId, int port) {
                super();

                setName(name);
                setSwitchId(switchEntryId);
                setSwitchPort(port);
        }

        // Protected Copy constructor
        protected SwitchAndPort(SwitchAndPort snp) {
                super();

                setName(snp.getName());
                setSwitchId(snp.getSwitchId());
                setSwitchPort(snp.getSwitchPort());
        }

        public SwitchAndPort(DBObject sapObj) {
             this.setName((String) sapObj.get("name"));
             this.setSwitchPort((int) sapObj.get("switchPort"));
             this.setSwitchId((ObjectId) sapObj.get("switchEntryId"));
        }

        public SwitchAndPort clone() {
            return new SwitchAndPort(this);
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
            this.name = name;
        }

       public String getSwitchIdStr() {
            return switchId.toString();
        }

        public ObjectId getSwitchId() {
            return this.switchId;
        }

        public void setSwitchId(String switchId) {
            this.switchId = new ObjectId(switchId);
        }

        public void setSwitchId(ObjectId switchId) {
            this.switchId = switchId;
        }

        public int getSwitchPort() {
                return switchPort;
        }

        public void setSwitchPort(int switchPort) {
                this.switchPort = switchPort;
        }

        public BasicDBObject getAsDocument() {
                BasicDBObject document = new BasicDBObject();

                document.put("name",           this.getName());
                document.put("switchEntryId",  this.getSwitchId());
                document.put("switchPort",     this.getSwitchPort());

                return document;
        }

}
