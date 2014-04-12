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

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class SwitchEntry {

        private long referenceCount = 0;
        private String objectId = "";
        private String name = "";
        private TappingModeEnum tappingMode = TappingModeEnum.NORMAL;

        // For TapAggr mode, defines the next switch in the chain by ObjectID
        private String nextHopSwitchId = "";

        // Contains the switch description data we get from OpenFlow
        private OPFSwitch opfSwitch = null; // There is no OPFSwitch yet

        private static final Logger logger = Logger.getLogger(TappingApp.class);

        // Constructor
        public SwitchEntry() {
        }

        public SwitchEntry(DBObject switchEntryObj, TappingApp tappingApp) {
            super();

            ObjectId oid = (ObjectId) switchEntryObj.get("_id");

            this.setObjectId(oid.toString());
            this.setName((String) switchEntryObj.get("name"));
            this.setReferenceCount((long) switchEntryObj.get("refCount"));
            this.setTappingMode(TappingModeEnum.valueOf((String) switchEntryObj.get("tappingMode")));

            String nextHopSwitchId = (String) switchEntryObj.get("nextHopSwitch");
            if ((nextHopSwitchId != null) && !nextHopSwitchId.isEmpty())
               this.setNextHopSwitch(nextHopSwitchId);

            // Get the datapathDescriptor for the OPF Switch from the database object
            String OPFSwitchDataPath = (String) switchEntryObj.get("OPFSwitchDatapath");

            logger.info("OPFSwitchDataPath " +  OPFSwitchDataPath);

            if ((OPFSwitchDataPath != null) && !(OPFSwitchDataPath.isEmpty())) {

                OPFSwitch opfSwitch = tappingApp.getAssignedOPFSwitchById(OPFSwitchDataPath);

                if (opfSwitch == null) {
                    // And get the OPFSwitch object from the TappingApp
                    opfSwitch = tappingApp.getUnAssignedOPFSwitchById(OPFSwitchDataPath);
                    if (opfSwitch != null) {
                        logger.info("Found Unassigned OPFSwitch with ID " +  OPFSwitchDataPath);
                        // Assign the OPFSwitch to this switch
                        this.assignOPFSwitch(opfSwitch);
                    }
                    else {
                         logger.info("OPFSwitch with ID " +  OPFSwitchDataPath + "is MIA");
                    }
                }
                else {
                    logger.info("Found Assigned OPFSwitch with ID " +  OPFSwitchDataPath);
                }
            }
        }

        public String getObjectId(){
                return objectId;
        }

        public void setObjectId(String objectId) {
                this.objectId = objectId;
        }

        public long getReferenceCount(){
                return this.referenceCount;
        }

        public void setReferenceCount(final long refCount) {
                this.referenceCount = refCount;
        }

        public TappingModeEnum getTappingMode() {
                return tappingMode;
        }

        public void setTappingMode(TappingModeEnum tappingMode) {
                this.tappingMode = tappingMode;
        }

        public String getName() {
                return name;
        }

        public void setName(String switchName) {
                this.name = switchName;
        }

        public OPFSwitch getOPFSwitch() {
                return opfSwitch;
        }

        public void setOPFSwitch(OPFSwitch opfSwitch) {
                this.opfSwitch = opfSwitch;
        }

        public void setNextHopSwitch(final String nextHopSwitchId) {
            if ((nextHopSwitchId == null)  || nextHopSwitchId.isEmpty()) {
                logger.info("SwitchEntry: NextHopSwitch is not set (null)");
                return;
             }

             // Increment the reference count
             try {
                    NextHopSwitch.adjustReference(ReferenceCountEnum.INCREMENT, nextHopSwitchId, this);
             } catch (NotFoundException e) {
                    e.printStackTrace();
             }

             // Add the next hop switch ID to this switch
             this.nextHopSwitchId = nextHopSwitchId;
        }

        public void setNextHopSwitch(final NextHopSwitch nextHopSwitch) {
                this.setNextHopSwitch(nextHopSwitch.getObjectId());
        }

        public String getNextHopSwitchId() {
                return nextHopSwitchId;
        }

    public static boolean adjustReferenceCount(final ReferenceCountEnum adjType, String switchEntryId) throws NotFoundException {

                DB database = TappingApp.getDatabase();
                DBCollection table = database.getCollection(DatabaseNames.getSwitchEntryTableName());

                // Look for the SwitchEntry object by object ID in the database
                BasicDBObject searchQuery = new BasicDBObject();
                ObjectId id = new ObjectId(switchEntryId);
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

                document.put("refCount",          this.getReferenceCount());
                document.put("name",              this.getName());
                document.put("tappingMode",       this.getTappingMode().toString());
                document.put("nextHopSwitch",     this.getNextHopSwitchId());
                document.put("OPFSwitchDatapath", (this.getOPFSwitch() != null) ? this.getOPFSwitch().getDataPathDesc() : "");

                return document;
        }

        //  Copy constructor.
         public SwitchEntry(SwitchEntry from) {
                 this.setNextHopSwitch(from.getNextHopSwitchId());
                 this.setName(from.getName());
                 this.setTappingMode(from.getTappingMode());
                 this.setOPFSwitch(from.getOPFSwitch());
          }

        public String toString() {
                return "Switch Entry: " + this.getName()
                                        + " Tapping Mode " + this.getTappingMode().toString()
                                        + " Next Hop " + ((this.getNextHopSwitchId() == null) ? "undefined" : this.getNextHopSwitchId()
                                        + " OPFSwitch " + ((this.getOPFSwitch() == null) ? " undefined " : this.getOPFSwitch()));
        }

        public void assignOPFSwitch(OPFSwitch opfSwitch) {
           opfSwitch.setAssigned(true);

           // The OPFSwitch is assigned to this switchEntry
           this.setOPFSwitch(opfSwitch);
        }

        public void unassignOPFSwitch(OPFSwitch opfSwitch) throws IllegalArgumentException {
            // Make sure the OPFSwitch passed in is assigned to this switch (uses the datapathdesc)
            if (!this.getOPFSwitch().isEqual(opfSwitch))
                    throw new IllegalArgumentException();

            // The OPFSwitch matches the one assigned to this SwitchEntry so remove it
            this.setOPFSwitch(null);

            opfSwitch.setAssigned(false);
        }
}
