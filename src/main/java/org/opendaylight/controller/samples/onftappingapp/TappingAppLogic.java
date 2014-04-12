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
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.UpdateType;

// This class implements the logic that receives updates from the persistence layer
// (the mongo database) and interacts with ODL to add/modify/remove flows
public class TappingAppLogic     {

        private static final Logger logger = Logger.getLogger(TappingApp.class);

        private TappingApp tappingApp;

        public TappingAppLogic(TappingApp tappingApp) {
                // Remember where to get our config from
                this.tappingApp = tappingApp;
        }

        // Called from the mid-tier API whenever the user modifies the database contents
        public void NotifyConfigChange(DBTableChangeEnum DBTableChange) {
                logger.info("Database config changed for objects " + DBTableChange.name());

                switch (DBTableChange)  {
                case TAP_POLICY:
                        logger.info("Tap Policy DB change");
                        break;

                case MATCH_CRITERIA:
                        logger.info("Match Criteria DB change");
                        break;

                case SWITCH_ENTRY:
                        logger.info("Switch Entry DB change");
                        break;

                case CAPTURE_DEVICE:
                        logger.info("Capture Device DB change");
                        break;

                case PORT_CHAIN:
                        logger.info("Port Chain DB change");
                        break;

                default:
                        logger.warn("Unhanded object type in Database notify receiver" );
                }
        }

        // Called from the mid-tier API whenever the user modifies a specific object
        public void NotifyConfigChange(DBTableChangeEnum DBTableChange, String objectId) {
                logger.info("Database config changed for objects " + DBTableChange.name());

                switch (DBTableChange)  {
                case TAP_POLICY:
                        // Process the Add Tap Policy logic and configure the switches
                        ProcessTapPolicy(objectId);
                break;

                case MATCH_CRITERIA:
                        // Process the Add Match Criteria logic and configure the switches
                        ProcessMatchCriteria(objectId);
                break;

                default:
                        logger.warn("Unhanded object type in Database notify receiver" );
                }

        }

        private void ProcessTapPolicy(String objectId) {
            TapPolicy tapPolicy = null;

            try {
                    tapPolicy = tappingApp.getTapPolicyById(objectId);
            } catch (NotFoundException e) {
                    logger.error("Cannot find Tap Policy " + objectId);
            }

            logger.info("Process Tap Policy " + tapPolicy.getName());

            // iterate over all the switches
            List<SwitchAndPort> switchAndPortList = tapPolicy.getSwitchAndPortList();
            for (SwitchAndPort switchAndPort : switchAndPortList) {

                String switchId = switchAndPort.getSwitchIdStr();

                SwitchEntry switchEntry = null;
                try {
                    switchEntry = tappingApp.getSwitchEntryById(switchId);
                } catch (NotFoundException e) {
                    logger.error("Unable to resolve DNS name for switch ", e);
                }

                // Apply this tap policy to each switch
                this.processSwitchEntryForTapPolicy(switchEntry, tapPolicy);
            }
        }


        private void processSwitchEntryForTapPolicy(SwitchEntry switchEntry, TapPolicy tapPolicy) {
            // Process port chains if we have them for this policy
            processPortChainListForTapPolicy(switchEntry, tapPolicy);

            processCaptureDeviceListForTapPolicy(switchEntry, tapPolicy);

            processMatchCriteriaForTapPolicy(switchEntry, tapPolicy);
        }

        private void processMatchCriteriaForTapPolicy(SwitchEntry switchEntry, TapPolicy tapPolicy) {

            List<String> matchCriteriaIdList = tapPolicy.getMatchCriteriaIdList();

            // Nothing to do if the list is empty
            if (matchCriteriaIdList.isEmpty())
                return;

            for (String matchCriteriaId : matchCriteriaIdList) {
                try {
                    MatchCriteria matchCriteria = tappingApp.getMatchCriteriaById(matchCriteriaId);
                } catch (NotFoundException e) {
                    logger.error("Unable to find Match Criteria with ID " + matchCriteriaId);
                }
            }
        }

        private void processCaptureDeviceListForTapPolicy(SwitchEntry switchEntry, TapPolicy tapPolicy) {

            List<String> captureDevIdList = tapPolicy.getCaptureDevList();

            // Nothing to do if the list is empty
            if (captureDevIdList.isEmpty())
                return;

            for (String captureDevId : captureDevIdList) {
                try {
                    CaptureDev captureDev = tappingApp.getCaptureDevById(captureDevId);
                    // TODO - push rules to the switch to configure this capture device's port
                } catch (NotFoundException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
                }
            }
        }

        private void processPortChainListForTapPolicy(SwitchEntry switchEntry, TapPolicy tapPolicy) {

            List<String> portChainIdList = tapPolicy.getPortChainList();

            // Nothing to do if the list is empty
            if (portChainIdList.isEmpty())
                return;

            for (String portChainId : portChainIdList) {
                try {
                    PortChain portChain = tappingApp.getPortChainById(portChainId);

                    // TODO - add logic to clone packets for all flows to the port chain out port
                    // TODO - add logic to receive packets from the return port and send them to the capture device

                } catch (NotFoundException e) {
                    logger.error("Error while processing port chain for Tap Policy "  + tapPolicy.getName());
                }
            }
        }

        private void ProcessMatchCriteria(String objectId) {
            MatchCriteria matchCriteria;
            try {
                matchCriteria = tappingApp.getMatchCriteriaById(objectId);
                logger.info("Added Match Criteria " + matchCriteria.getName());
            } catch (NotFoundException e) {
                logger.error("Cannot find Tap Policy " + objectId);
            }
        }

        // Called from ODL when a switch is added/modified/removed
        public void switchChanged(UpdateType type, Node node) {

            logger.info("TappingAppLogic::switchChanged");

            // Add or remove the OPFSwitch based on the action type
            this.addOrRemoveONFSwitchEntry(type, node);

            // Get the switch's ID from the Node
            final String switchId = node.getNodeIDString();

            // And look for the corresponding SwitchEntry in the database
            SwitchEntry switchEntry = null;
            try {
                switchEntry = this.tappingApp.findSwitchByDataPathId(switchId);
            } catch (NotFoundException e) {
                logger.info("Switch " + switchId + " connected but is not configred in ONFTappingApp");
            }

            logger.info("SwitchEntry is rec/unrec " + switchEntry);

            if ((switchEntry != null) && (switchEntry.getOPFSwitch() != null)) {
                // A switch connected and we are managing it
                this.processRecognizedSwitch(type, node);
            }
            else {
                // A switch connected but we aren't managing it
                // We have added the ONFSwitch to the unrecognized list. The rest is up to the user
            }
        }

        private void addOrRemoveONFSwitchEntry(UpdateType type, Node node) {

            // Get the switch's ID
            final String opfSwitchId = node.getNodeIDString();

            // See if we have an existing entry for this OPF Switch
            OPFSwitch opfSwitch = this.tappingApp.getOPFSwitchById(opfSwitchId);

            switch (type) {
            case ADDED:
            case CHANGED:
                if (opfSwitch == null) {
                    // Create a new OPF Switch Entry
                    this.tappingApp.createOPFSwitch(node);
                }
                break;

            case REMOVED:
                if (opfSwitch != null) {
                    this.tappingApp.removeOPFSwitch(node);
                }
            }
        }

        private void processRecognizedSwitch(UpdateType type, Node node) {

            logger.info("proceessRecognizedSwitch");

            // Holds the set of rules generated from the policy
            List<FlowEntry> flowRules = null;

            switch (type) {
            case ADDED:
                try {
                     flowRules = this.createFlowRulesForSwitch(node);
                } catch (NotFoundException e) {
                     logger.warn("Can't find SwitchEntry for Node ID " + node.getNodeIDString());
                } catch (InconsistentDatabaseException e) {
                    logger.warn(e.getLocalizedMessage());
                }
                break;

            case CHANGED:
                try {
                    flowRules = this.createFlowRulesForSwitch(node);
                } catch (NotFoundException e) {
                     logger.warn("Can't find SwitchEntry for Node ID " + node.getNodeIDString());
                } catch (InconsistentDatabaseException e) {
                     logger.warn(e.getLocalizedMessage());
                }
                break;

            case REMOVED:
                // The switch was removed (disconnected). We can't do anything useful since its gone
                break;
            }

            // We generated some rules so add them to the rulesDB and push them to the switch
            if (flowRules != null) {
                tappingApp.installFlowRules(flowRules, node);
            }
        }

        // Called when a switch (controlled by this app) connects to us
        private List<FlowEntry> createFlowRulesForSwitch(Node node) throws InconsistentDatabaseException, NotFoundException {

            logger.info("createFlowRulesForSwitch");

            // Flow Rules for this switch
            List<FlowEntry> flowRules = new ArrayList<FlowEntry>();

            final String nodeIDString = node.getNodeIDString();

            SwitchEntry switchEntry = null;
            try {
                switchEntry = tappingApp.findSwitchByDataPathId(nodeIDString);
            } catch (NotFoundException e) {
                logger.info("Can't find switch with ID " + nodeIDString);
                throw new NotFoundException();
            }

            List<TapPolicy> tappingPolicies = tappingApp.getTapPoliciesForSwitch(switchEntry);
            logger.info("Tapping Policies for switch " + switchEntry + " are: " + tappingPolicies);

            // Now we have all the tap policies that match this switch ID
            for (TapPolicy tapPolicy : tappingPolicies) {

                // Only use Tap Policies that are enabled
                if (tapPolicy.isEnabled()) {
                    tapPolicy.createFlowRules(node, flowRules, this);
                }
                else {
                    logger.info("TapPolicy " + tapPolicy.getName() + " is disabled");
                }
            }

            logger.info("Generated " + flowRules.size() + " rules for switch " + nodeIDString);

            // Return the complete set of rules to the caller
            return flowRules;
        }

//
//                // Generate the list of actions for this policy
//                List<Action> actionList = tapPolicy.generateActionList(node, this);
//
//                logger.info("returned from generateActions");
//
//                // Iterate over the match criteria for this policy
//                for (String matchCriteriaId : tapPolicy.getMatchCriteriaIdList()) {
//                    try {
//                        MatchCriteria matchCriteria = tappingApp.getMatchCriteriaById(matchCriteriaId);
//
//                        // Only include MatchCriteria that are enabled
//                        if (matchCriteria.getEnabled() == true) {
//                          // Generate the flow rules for this switch
//                          matchCriteria.generateFlowRules(flowRules, actionList, node);
//                        }
//                    } catch (NotFoundException e) {
//                        // logger.info("Can't find MatchCriteria "  + matchCriteriaId);
//                        throw new InconsistentDatabaseException("Can't find Match Criteria " + matchCriteriaId);
//                    }
//                }
//            }
//
//            logger.info("Generated " + flowRules.size() + " rules for switch " + nodeIDString);
//
//            // Return the complete set of rules to the caller
//            return flowRules;
//        }

        public NodeConnector getODLNodeConnectorByNodeAndPort(Node node, int portNum) throws NotFoundException {

            Set<NodeConnector> nodeConnectorSet = this.tappingApp.getODLNodeConnectors(node);
            final int numPorts = nodeConnectorSet.size();

            logger.info("Number of ports found " + numPorts);

            // search the set looking for a port number match
            for (NodeConnector nodeConnector : nodeConnectorSet) {
                // This is far from ideal but ODL does not provide a way to index to a physical port
                logger.info("NC ID " + nodeConnector.getID());
                final short portNumber = (short) nodeConnector.getID();
                if (portNumber == portNum) {
                    return nodeConnector;
                }
            }

            logger.info("throwing exception not found");
            throw new NotFoundException();
        }

        public SwitchEntry getSwitchEntryById(String switchId) throws NotFoundException {
            return this.tappingApp.getSwitchEntryById(switchId);
        }

        public MatchCriteria getMatchCriteriaById(String matchCriteriaId) throws NotFoundException {
            return tappingApp.getMatchCriteriaById(matchCriteriaId);
        }

//        public void createDemoObjects(SwitchEntry switchEntry) {
//
//            // Define the match criteria for the policy
//            MatchCriteria matchCriteria = new MatchCriteria();
//            matchCriteria.setName("Match Criteria 1");
//
//            // Define the match fields
//            matchCriteria.addMatchField(new MatchField(MatchField.ETH_TYPE, "2054"));
//            matchCriteria.addMatchField(new MatchField(MatchField.IP_PROTO, "6"));
//            matchCriteria.addMatchField(new MatchField(MatchField.IP_TOS ,  "1234"));
//            matchCriteria.addMatchField(new MatchField(MatchField.VLAN_TAG, "5555"));
//
//            // Match on source address from a class B subnet
//            AddressDesc sourceAddrDesc = new AddressDesc();
//
//            sourceAddrDesc.setIPAddress("192.168.55.33");
//            sourceAddrDesc.setIPMask("255.255.0.0");
//
//            // Match a specific destination host on IP and MAC
//            AddressDesc destAddrDesc = new AddressDesc();
//
//            destAddrDesc.setIPAddress("10.10.52.19");
//            destAddrDesc.setIPMask("255.255.255.255");
//
//            matchCriteria.addMatchField(new MatchField(MatchField.PROTO_PORT, "[80-80, 443, 20-21]"));
//
//            logger.info(matchCriteria.toString());
//
//            // Add the match criteria to the system
//            try {
//                tappingApp.addMatchCriteria(matchCriteria);
//            } catch (DuplicateEntryException e1) {
//                // TODO Auto-generated catch block
//                logger.error("Duplicate Match Criteria", e1);
//            }
//
//            // ********************* Capture Device ***********************
//
//            // The capture device is connected to the same switch so copy it
//            SwitchEntry captureDevSwitch = new SwitchEntry(switchEntry);
//
//            CaptureDev recorderCaptureDev = new CaptureDev();
//            recorderCaptureDev.setName("Recorder Capture Dev");
//            recorderCaptureDev.setCaptureType(CaptureTypeEnum.RECORDER);
//            recorderCaptureDev.setSwitchPort(23);
//            recorderCaptureDev.setSwitchId(captureDevSwitch.getObjectId());
//            recorderCaptureDev.setEnabled(true);
//
//            try {
//                recorderCaptureDev.setIPAddress("192.168.55.32");
//            } catch (UnknownHostException e) {
//                logger.debug("Exception: ", e);
//            }
//
//            // Add the captureDev to the system
//            try {
//                tappingApp.addCaptureDevice(recorderCaptureDev);
//            } catch (DuplicateEntryException e) {
//                // TODO Auto-generated catch block
//                logger.info("Duplicate capture dev");
//            }
//
//            // ********************* Next Hop Switch ***********************
//
//            SwitchEntry upstreamSwitch = new SwitchEntry();
//            upstreamSwitch.setName("Upstream Switch");
//            upstreamSwitch.setTappingMode(TappingModeEnum.TAPAGGR);
//
//            // Add the switch entry to the system
//            try {
//                tappingApp.addSwitchEntry(upstreamSwitch);
//            } catch (DuplicateEntryException e2) {
//                // TODO Auto-generated catch block
//                e2.printStackTrace();
//            }
//
//            NextHopSwitch nextHopSwitch = new NextHopSwitch();
//            nextHopSwitch.setName("Next Hop Switch");
//            nextHopSwitch.setSwitchEntry(upstreamSwitch);
//            nextHopSwitch.setVLANTag(0);
//
//            // Add the NextHopSwitch to the system
//            try {
//                tappingApp.addNextHopSwitch(nextHopSwitch);
//            } catch (DuplicateEntryException e1) {
//                // TODO Auto-generated catch block
//                e1.printStackTrace();
//            }
//
//            // Associate the NextHopSwitch with the SwitchEntry
//            switchEntry.setNextHopSwitch(nextHopSwitch);
//
//            // ********************* Tap Policy ***********************
//
//            // Create a new policy to add to the system
//            TapPolicy tapPolicy1 = new TapPolicy();
//
//            // Set all the fields (except objectId)
//            tapPolicy1.setName("Tap Policy 1");
//            tapPolicy1.setDescription("test policy 1");
//            tapPolicy1.setEnabled(true);
//
//            // Add the matching criteria to the policy
//            try {
//                tapPolicy1.addMatchCriteria(matchCriteria);
//            } catch (NotFoundException e) {
//                logger.info("Match Criteria Not Found");
//            }
//
//            // Add the switch entry to the tap policy
//            try {
//                tapPolicy1.addSwitchEntry(switchEntry);
//            } catch (NotFoundException e) {
//                // TODO Auto-generated catch block
//                logger.info("SwitchEntry not found");
//            }
//
//            // Add the capture device to the tap policy
//            try {
//                tapPolicy1.addCaptureDev(recorderCaptureDev);
//            } catch (NotFoundException e) {
//                logger.info("Capture Dev not found");
//            }
//
//            // Add the tap policy to the system. NOTE: This updates the object ID field in the tap policy
//            try {
//                tappingApp.addTapPolicy(tapPolicy1);
//            } catch (DuplicateEntryException e) {
//                logger.info("Duplicate Tap Policy");
//            }
//        }

};
