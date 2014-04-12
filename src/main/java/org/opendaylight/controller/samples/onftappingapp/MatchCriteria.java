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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.samples.onftappingapp.internal.ONFTappingAppImpl;

public class MatchCriteria {

        private long referenceCount = 0;
        private String objectId = "";
        private String name = "";
        private boolean enabled = true;
        private boolean reflexive;
        private int priority;


        // Values to match
        List<MatchField> matchFieldList = new ArrayList<MatchField>();

        private static Logger logger = LoggerFactory.getLogger(ONFTappingAppImpl.class);

        public MatchCriteria() {
                this.referenceCount = 0;
        }

        // Copy constructor
        public MatchCriteria(MatchCriteria matchCriteria) {
                this.objectId    = matchCriteria.getObjectId();
                this.name        = matchCriteria.getName();
                this.enabled     = matchCriteria.getEnabled();
                this.reflexive   = matchCriteria.getReflexive();
                this.priority    = matchCriteria.getPriority();

                this.matchFieldList = new ArrayList<MatchField>(matchCriteria.getFields());
        }

        public MatchCriteria(DBObject matchCriteriaObj) {

            this.setName((String) matchCriteriaObj.get("name"));
            this.setReferenceCount((long) matchCriteriaObj.get("refCount"));

            ObjectId oid = (ObjectId) matchCriteriaObj.get("_id");
            this.setObjectId(oid.toString());
            this.setEnabled( (Boolean) matchCriteriaObj.get("enabled"));
            this.setReflexive((Boolean) matchCriteriaObj.get("reflexive"));
            this.setPriority((int) matchCriteriaObj.get("priority"));

            BasicDBList mfList = (BasicDBList) matchCriteriaObj.get("matchFields");
            BasicDBObject[] mfArray = mfList.toArray(new BasicDBObject[0]);
            for(BasicDBObject dbObj : mfArray) {
                MatchField matchField = new MatchField();
                matchField.setType(dbObj.getString("type"));
                matchField.setValue(dbObj.getString("value"));
                    // Add the match field to the match criteria
                    this.addMatchField(matchField);
            }
        }

        public long getReferenceCount(){
                return this.referenceCount;
        }

        public void setReferenceCount(final long refCount){
                this.referenceCount = refCount;
        }

        public String getObjectId(){
                return this.objectId;
        }

        public void setObjectId(String objId){
                this.objectId = objId;
        }

        public boolean isReflexive() {
                return reflexive;
        }

        public String getName() {
                return this.name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public boolean getEnabled() {
                return this.enabled;
        }

        public void setEnabled(boolean enabled) {
                this.enabled = enabled;
        }

        public boolean getReflexive() {
                return this.reflexive;
        }

        public int getPriority() {
                return priority;
        }

        public void setPriority(int priority) {
                this.priority = priority;
        }

        public void setReflexive(boolean reflexive1) {
                this.reflexive = reflexive1;
        }

        public List<MatchField> getFields() {
            List<MatchField> cloneList = new ArrayList<MatchField>(matchFieldList.size());

            Iterator<MatchField> it = matchFieldList.iterator();

            while (it.hasNext()) {
                    MatchField matchField = it.next();
                    cloneList.add(matchField);
            }

            return cloneList;
        }

        // Construct the match criteria fields from a list of MatchField key/value pairs
        public void addMatchField(MatchField matchField)
        {
                logger.info(matchField.toString());

                // Copy the match criteria into the map and set the bit mask to indicate it is part of the match
                switch(matchField.getType()) {
                case MatchField.UNDEFINED:
                        // Ignored
                        break;

                case MatchField.ETH_TYPE:
                case MatchField.IP_PROTO:
                case MatchField.IP_TOS:
                case MatchField.VLAN_TAG:
                case MatchField.SOURCE_IP:
                case MatchField.SOURCE_MAC:
                case MatchField.DEST_IP:
                case MatchField.DEST_MAC:
                case MatchField.PROTO_SVC:
                case MatchField.PROTO_PORT:
                        matchFieldList.add(matchField);
                        break;

                default:
                        logger.warn("Unhandled Match Field Type " + matchField.getType() );
                        break;
                }
        }

        // Construct the match criteria fields from a list of MatchField key/value pairs
        public void setFields(List<MatchField> matchFields) {
                Iterator<MatchField> it = matchFields.iterator();

                while (it.hasNext()) {
                        MatchField matchField = it.next();
                        addMatchField(matchField);
                }
        }

        public String toString() {
                String objString;

                objString = "Match Criteria: Object ID: " + getObjectId() + " Name:" + getName();

                Iterator<MatchField> it = matchFieldList.iterator();
                while (it.hasNext()) {
                        MatchField matchField = it.next();
                        objString += ", " + matchField.toString();
                }

                return objString;
        }

        public BasicDBObject getAsDocument() {
                BasicDBObject document = new BasicDBObject();

                document.put("refCount",  this.getReferenceCount());
                document.put("name",      this.getName());
                document.put("enabled",   this.getEnabled());
                document.put("reflexive", this.getReflexive());
                document.put("priority",  this.getPriority());

                List<Object> matchFieldsDoc = new BasicDBList();
                for ( int i = 0; i < matchFieldList.size(); i++) {
                        MatchField matchField = matchFieldList.get(i);
                        matchFieldsDoc.add(matchField.getAsDocument());
                }
                document.put("matchFields", matchFieldsDoc);

                return document;
        }

        public static boolean adjustReferenceCount(final ReferenceCountEnum adjType, String matchCriteriaId) throws NotFoundException {

                DB database = TappingApp.getDatabase();
                DBCollection table = database.getCollection(DatabaseNames.getMatchCriteriaTableName());

                // Look for the Match Criteria object by object ID in the database
                BasicDBObject searchQuery = new BasicDBObject();
                ObjectId id = new ObjectId(matchCriteriaId);
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

        public void generateFlowRules(List<FlowEntry> flowRules, List<Action> actions, Node node) {

            // Add a Match for the set of MatchFields in the MatchCriteria
            List<MatchField> matchFields = this.getFields();

            // OpenDaylight Match object
            Match match = new Match();

            for (MatchField matchField : matchFields) {
                try {
                    logger.info("MatchField " + matchField);

                    // Get the OpenDaylight Match object for this match field
                    matchField.addODLMatch(match);
                } catch (UnknownHostException e) {
                    logger.info("Unable to resolve hostname for a MatchField used by MatchCritera " + this.getName());
                } catch (IllegalArgumentException e) {
                    logger.info("Cannot create flow from " + matchField + " used by MatchCriteria " + this.getName() + ": Illegal Argument");
                }
            }

            Flow flow = new Flow(match, actions);
            // Install the flows permanently (no timeouts)
            flow.setIdleTimeout((short) 0);
            flow.setHardTimeout((short) 0);

            // Set the priority based on the user input
            flow.setPriority((short) this.getPriority());

            if (this.isReflexive()) {
                logger.info("MatchCriteria is reflexive ... adding reverse flow");
                // TODO -- add this here
            }

            FlowEntry flowEntry = new FlowEntry("ONFTappingApp", this.getName(), flow, node);
            flowRules.add(flowEntry);
        }
}

