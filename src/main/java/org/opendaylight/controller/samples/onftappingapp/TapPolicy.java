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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.naming.OperationNotSupportedException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.core.Node;

public class TapPolicy {

        private String objectId = "";
        private String name = "";
        private String description = "";
        private boolean enabled = false;

        // Specifies the criteria used to match this policy
        List<String> matchCriteriaIdList = new ArrayList<String>();
        List<String> captureDevIdList    = new ArrayList<String>();
        List<String> portChainIdList     = new ArrayList<String>();

        // Stores the SwitchAndPort objects defined for this policy
        List<SwitchAndPort> switchAndPortList = new ArrayList<SwitchAndPort>();

        private static final Logger logger = Logger.getLogger(TappingApp.class);

        // Construct an empty object
        public TapPolicy() {
        }

        public TapPolicy(String objectId, String name, String description, boolean enabled) {
                this.objectId = objectId;
                setName(name);
                setDescription(description);
                setEnabled(enabled);
        }

        // Construct from a database object
        public TapPolicy(DBObject tapPolicyObj) throws NotFoundException {
            ObjectId oid = (ObjectId) tapPolicyObj.get("_id");
            this.setObjectId(oid.toString());
            this.setName((String) tapPolicyObj.get("name"));
            this.setDescription((String) tapPolicyObj.get("description"));
            this.setEnabled((Boolean) tapPolicyObj.get("enabled"));

            BasicDBList mcList = (BasicDBList) tapPolicyObj.get("matchCriteria");
            Iterator<Object> mcListIt = mcList.iterator();
            while (mcListIt.hasNext()) {
               String matchCriteriaId = (String) mcListIt.next();
               this.addMatchCriteria(matchCriteriaId);
            }

            BasicDBList swAndPortList = (BasicDBList) tapPolicyObj.get("switchAndPort");
            Iterator<Object> swAndPortListIt = swAndPortList.iterator();
            while (swAndPortListIt.hasNext()) {
                DBObject sapObj = (DBObject) swAndPortListIt.next();
                SwitchAndPort switchAndPort = new SwitchAndPort(sapObj);
                this.addSwitchAndPort(switchAndPort);
            }

            BasicDBList cdList = (BasicDBList) tapPolicyObj.get("captureDev");
            Iterator<Object> cdListIt = cdList.iterator();
            while (cdListIt.hasNext()) {
               String captureDevId = (String) cdListIt.next();
               this.addCaptureDev(captureDevId);
            }

            BasicDBList pcList = (BasicDBList) tapPolicyObj.get("portChain");
            Iterator<Object> pcListIt = pcList.iterator();
            while (pcListIt.hasNext()) {
               String portChainId = (String) pcListIt.next();
               this.addPortChain(portChainId);
            }
        }

        // Copy constructor
        public TapPolicy(TapPolicy tapPolicy) {
            this.objectId    = tapPolicy.getObjectId();
            this.name        = tapPolicy.getName();
            this.description = tapPolicy.getDescription();
            this.enabled     = tapPolicy.isEnabled();

            this.matchCriteriaIdList = new ArrayList<String>(tapPolicy.getMatchCriteriaIdList());
            this.captureDevIdList    = new ArrayList<String>(tapPolicy.getCaptureDevList());
            this.portChainIdList     = new ArrayList<String>(tapPolicy.getPortChainList());

            this.switchAndPortList   = new ArrayList<SwitchAndPort>(tapPolicy.getSwitchAndPortList());
        }

        // Getters and setters
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

        public String getDescription() {
                return description;
        }

        public void setDescription(String description) {
                this.description = description;
        }

        public boolean isEnabled() {
                return enabled;
        }

        public void setEnabled(boolean enable) {
                this.enabled = enable;
        }

        public void addMatchCriteria(final MatchCriteria matchCriteria) throws NotFoundException {
                this.addMatchCriteria(matchCriteria.getObjectId());
        }

        public void addMatchCriteria(final String matchCriteriaId) throws NotFoundException {
                // Increment the reference count
                MatchCriteria.adjustReferenceCount(ReferenceCountEnum.INCREMENT, matchCriteriaId);

                // Add the match criteria to this policy's list
                this.matchCriteriaIdList.add(matchCriteriaId);
        }

        public void removeMatchCriteria(String matchCriteriaId) throws  NotFoundException{
            // Decrement the reference count
            MatchCriteria.adjustReferenceCount(ReferenceCountEnum.DECREMENT, matchCriteriaId);

            // Remove the match criteria to this policy's list
            this.matchCriteriaIdList.remove(matchCriteriaId);
        }

        public List<String> getMatchCriteriaIdList() {
            List<String> cloneList = new ArrayList<String>(matchCriteriaIdList.size());

            Iterator<String> it = matchCriteriaIdList.iterator();

            while (it.hasNext()) {
                    String matchCriteriaId = it.next();
                    cloneList.add(matchCriteriaId);
            }

            return cloneList;
        }

        public void addSwitchAndPort(final SwitchAndPort switchAndPort) throws NotFoundException {

            // Get the Switch ID from the SwitchAndPort
            String switchEntryId = switchAndPort.getSwitchIdStr();

            // Increment the reference count on the SwitchEntry
            SwitchEntry.adjustReferenceCount(ReferenceCountEnum.INCREMENT, switchEntryId);

            // Add the switch entry to this policy's list
            this.switchAndPortList.add(switchAndPort);
        }

        public void removeSwitchAndPort(SwitchAndPort switchAndPort) throws NotFoundException {

            // Get the Switch ID from the SwitchAndPort
            String switchEntryId = switchAndPort.getSwitchIdStr();

            // Decrement the reference count
            SwitchEntry.adjustReferenceCount(ReferenceCountEnum.DECREMENT, switchEntryId);

            // Remove the SwitchAndPort from this policy's list
            this.switchAndPortList.remove(switchAndPort);
        }


        public List<SwitchAndPort> getSwitchAndPortList () {
             List<SwitchAndPort> cloneList = new ArrayList<SwitchAndPort>(switchAndPortList.size());

             for (SwitchAndPort switchAndPort : switchAndPortList) {

                 // Copy the SwitchAndPort objects into the cloned list
                 cloneList.add(switchAndPort.clone());
             }

             return cloneList;
        }

        public void addCaptureDev(CaptureDev captureDev) throws NotFoundException {
            this.addCaptureDev(captureDev.getObjectId());
        }

        public void addCaptureDev(String captureDevId) throws NotFoundException {
            // Increment the reference count
            CaptureDev.adjustReferenceCount(ReferenceCountEnum.INCREMENT, captureDevId);

            // Add the match criteria to this policy's list
            this.captureDevIdList.add(captureDevId);
        }

        public void removeCaptureDev(String captureDevId) throws NotFoundException {
            // Decrement the reference count
            CaptureDev.adjustReferenceCount(ReferenceCountEnum.DECREMENT, captureDevId);

            // Remove the match criteria to this policy's list
            this.captureDevIdList.remove(captureDevId);
        }

        public List<String> getCaptureDevList() {
            List<String> cloneList = new ArrayList<String>(captureDevIdList.size());

            Iterator<String> it = captureDevIdList.iterator();

            while (it.hasNext()) {
                    String captureDevId = it.next();
                    cloneList.add(captureDevId);
            }

            return cloneList;
        }

        public void addPortChain(PortChain portChain) throws NotFoundException {
            this.addPortChain(portChain.getObjectId());
        }

        public void addPortChain(final String portChainId) throws NotFoundException {
            // Increment the reference count
            PortChain.adjustReferenceCount(ReferenceCountEnum.INCREMENT, portChainId);

            // Add the port chain to this policy's list
            this.portChainIdList.add(portChainId);
        }


        public void removePortChain(String portChainId) throws NotFoundException {
            // Decrement the reference count
            PortChain.adjustReferenceCount(ReferenceCountEnum.DECREMENT, portChainId);

            // Remove the match criteria to this policy's list
            this.portChainIdList.remove(portChainId);
        }


        public List<String> getPortChainList() {
            List<String> cloneList = new ArrayList<String>(portChainIdList.size());

            Iterator<String> it = portChainIdList.iterator();

            while (it.hasNext()) {
                    String portChainId = it.next();
                    cloneList.add(portChainId);
            }

            return cloneList;
        }

        // Return a summary of the Tap Policy object (for the GUI)
        public TapPolicySummary getSummary() {

                TapPolicySummary summary = new TapPolicySummary(getObjectId(), getName(), getDescription(), isEnabled());
                return summary;
        }

        public BasicDBObject getAsDocument() {
            BasicDBObject document = new BasicDBObject();

            document.put("name", getName());
            document.put("description", getDescription());
            document.put("enabled", isEnabled());
            document.put("creationDate", new Date());

            // Add Match Criteria ID list Sub-document
            BasicDBList matchCriteriaListDoc = new BasicDBList();
            for (int i = 0; i < matchCriteriaIdList.size(); i++) {
                String matchCriteriaId = matchCriteriaIdList.get(i);
                matchCriteriaListDoc.add(matchCriteriaId);
            }
            document.put("matchCriteria", matchCriteriaListDoc);

            // Add SwitchAndPort Sub-document
            BasicDBList switchAndPortListDoc = new BasicDBList();
            for (SwitchAndPort switchAndPort : switchAndPortList) {
                switchAndPortListDoc.add(switchAndPort.getAsDocument());
            }
            document.put("switchAndPort", switchAndPortListDoc);

            // Add Capture Device Sub-document
            BasicDBList captureDevListDoc = new BasicDBList();
            for (int i = 0; i < captureDevIdList.size(); i++) {
                String captureDevId = captureDevIdList.get(i);
                captureDevListDoc.add(captureDevId);
            }
            document.put("captureDev", captureDevListDoc);

            // Add Port Chain Sub-document
            BasicDBList portChainListDoc = new BasicDBList();
            for (int i = 0; i < portChainIdList.size(); i++) {
                String portChainId = portChainIdList.get(i);
                portChainListDoc.add(portChainId);
            }
            document.put("portChain", portChainListDoc);

            return document;
        }

        public String toString() {
            String objString = "Tap Policy: Object ID: " + getObjectId() + " Name: " + name + " Description: " + description + " enabled: " + enabled;
            return objString;
        }

        // Remove object references for this TapPolicy referred to by the tapPolicyDBObj
        public static void removeObjectReferences(DBObject tapPolicyDBObj) throws NotFoundException {

            BasicDBList mcList = (BasicDBList) tapPolicyDBObj.get("matchCriteria");
            Iterator<Object> mcListIt = mcList.iterator();
            while (mcListIt.hasNext()) {
               String matchCriteriaId = (String) mcListIt.next();
               // Decrement the reference count in the database object
               MatchCriteria.adjustReferenceCount(ReferenceCountEnum.DECREMENT, matchCriteriaId);
            }

            BasicDBList seList = (BasicDBList) tapPolicyDBObj.get("switchEntries");
            Iterator<Object> seListIt = seList.iterator();
            while (seListIt.hasNext()) {
               String switchEntryId = (String) seListIt.next();
               // Decrement the reference count in the database object
               SwitchEntry.adjustReferenceCount(ReferenceCountEnum.DECREMENT, switchEntryId);
            }

            BasicDBList cdList = (BasicDBList) tapPolicyDBObj.get("captureDev");
            Iterator<Object> cdListIt = cdList.iterator();
            while (cdListIt.hasNext()) {
               String captureDevId = (String) cdListIt.next();
               // Decrement the reference count in the database object
               CaptureDev.adjustReferenceCount(ReferenceCountEnum.DECREMENT, captureDevId);
            }

            BasicDBList pcList = (BasicDBList) tapPolicyDBObj.get("portChain");
            Iterator<Object> pcListIt = pcList.iterator();
            while (pcListIt.hasNext()) {
               String portChainId = (String) pcListIt.next();
               // Decrement the reference count in the database object
               PortChain.adjustReferenceCount(ReferenceCountEnum.DECREMENT, portChainId);
            }
        }

        public List<Action> generateActionList(Node node, TappingAppLogic tappingAppLogic) throws NotFoundException {
            List<Action> actions = new ArrayList<Action>();

            for (String captureDevId : this.getCaptureDevList()) {
                CaptureDev captureDev = null;
                try {
                    captureDev = TappingApp.getCaptureDevById(captureDevId);
                } catch (NotFoundException e) {
                    logger.warn("Can't find CaptureDev with ID " + captureDevId);
                }

                if (captureDev.isEnabled()) {
                    // Only generate actions for CaptureDevices that are enabled
                    captureDev.generateActionList(actions, node, tappingAppLogic, this);
                }
                else
                    logger.info("CaptureDev " + this.getName() + " is disabled");
            }

            logger.info("Generated " + actions.size() + " actions");
            return actions;
        }

        Node getODLSwitchById(String switchId) throws OperationNotSupportedException {
            logger.info("TapPolicy:getODLSwitchById is not implemented");
            throw new OperationNotSupportedException();
        }

        public void createFlowRules(Node node, List<FlowEntry> flowRules, TappingAppLogic tappingAppLogic) throws NotFoundException, InconsistentDatabaseException {

            // Generate the list of actions for this policy
            List<Action> actionList = this.generateActionList(node, tappingAppLogic);

            // Iterate over the match criteria for this policy
            for (String matchCriteriaId : this.getMatchCriteriaIdList()) {
                try {
                    MatchCriteria matchCriteria = tappingAppLogic.getMatchCriteriaById(matchCriteriaId);

                    // Only include MatchCriteria that are enabled
                    if (matchCriteria.getEnabled() == true) {
                        // Generate the flow rules for this switch
                        matchCriteria.generateFlowRules(flowRules, actionList, node);
                    }
                    else {
                        logger.info("MatchCriteria " + matchCriteria.getName() + " is disabled");
                     }
                } catch (NotFoundException e) {
                    // logger.info("Can't find MatchCriteria "  + matchCriteriaId);
                    throw new InconsistentDatabaseException("Can't find Match Criteria " + matchCriteriaId);
                }
           }

           logger.info("Generated " + flowRules.size() + " rules for switch " + node.getNodeIDString());
    }
}