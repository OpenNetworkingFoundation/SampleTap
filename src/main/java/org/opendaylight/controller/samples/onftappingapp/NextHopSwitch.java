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
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class NextHopSwitch {

        private long referenceCount = 0;
        private String objectId = "";
        private String name = "";
        private String switchEntryId = "";
        private int VLANTag;

        public NextHopSwitch() {
                this.referenceCount = 0;
        }

        public void setReferenceCount(long refCount) {
                this.referenceCount = refCount;
        }

        public long getReferenceCount() {
                return this.referenceCount;
        }

        public NextHopSwitch(DBObject nhsObj) {

                ObjectId oid = (ObjectId) nhsObj.get("_id");
                this.setObjectId(oid.toString());
                this.setReferenceCount((long) nhsObj.get("refCount"));
                this.setName((String) nhsObj.get("name"));
                this.setSwitchEntryId((String) nhsObj.get("switchEntryId"));
                this.setVLANTag((int) nhsObj.get("VLAN_ID"));
        }

        public String getObjectId() {
                return this.objectId;
        }

        public void setObjectId(String objId) {
                this.objectId = objId;
        }

        public String getName() {
                return name;
        }

        public void setName(String switchName) {
                this.name = switchName;
        }

        public String getSwitchEntryId() {
                return switchEntryId;
        }

        public void setSwitchEntryId(String switchEntryId) {
                this.switchEntryId = switchEntryId;
        }

        public void setSwitchEntry(SwitchEntry switchEntry) {
                this.switchEntryId = switchEntry.getObjectId().toString();
        }

        public int getVLANTag() {
                return VLANTag;
        }

        public void setVLANTag(int vLANTag) {
                VLANTag = vLANTag;
        }

        public static boolean adjustReference(final ReferenceCountEnum adjType, String nhsId, SwitchEntry tapPolicy) throws NotFoundException {

                DB database = TappingApp.getDatabase();
                DBCollection table = database.getCollection(DatabaseNames.getNextHopSwitchTableName());

                // Look for the SwitchEntry object by object ID in the database
                BasicDBObject searchQuery = new BasicDBObject();
                ObjectId id = new ObjectId(nhsId);
                searchQuery.put("_id", id);
                DBObject dbObj = table.findOne(searchQuery);

                if (dbObj == null)
                        throw new NotFoundException();

                // create an increment query
                DBObject modifier = new BasicDBObject("refCount", (adjType == ReferenceCountEnum.DECREMENT)  ? -1 : 1);
                DBObject incQuery = new BasicDBObject("$inc", modifier);

                // increment a counter value atomically
                WriteResult result = table.update(searchQuery, incQuery);
                return (result != null) ? true : false;
        }

        public BasicDBObject getAsDocument() {
                BasicDBObject document = new BasicDBObject();

                document.put("refCount",      this.getReferenceCount());
                document.put("name",          this.getName());
                document.put("switchEntryId", this.getSwitchEntryId());
                document.put("VLAN_ID",       this.getVLANTag());

                return document;
        }

        public String toString() {
                return "Next Hop Switch: " + this.getName() + " SwitchEntryId: " + this.getSwitchEntryId() + " VLAN " + this.getVLANTag();
        }
}
