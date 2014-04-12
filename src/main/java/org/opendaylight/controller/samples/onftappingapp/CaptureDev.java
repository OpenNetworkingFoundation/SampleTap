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
import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.bson.types.ObjectId;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class CaptureDev {

        // Connect to the ODL slf4j logger
        protected static final Logger logger = LoggerFactory.getLogger(TappingApp.class);

        private long referenceCount = 0;

        private String objectId = "";
        private String name = "";
        private CaptureTypeEnum captureType = CaptureTypeEnum.UNDEFINED;
        private String switchId = "";
        private int switchPort;
        private long ipAddr;
        private String macAddr = "";
        private int vlanId;
        private boolean enabled = false;

        public CaptureDev() {
                this.referenceCount = 0;
        }

        public CaptureDev(DBObject captureDevObj) {
                ObjectId oid = (ObjectId) captureDevObj.get("_id");
                this.setObjectId(oid.toString());

                this.setReferenceCount((long) captureDevObj.get("refCount"));
                this.setName((String) captureDevObj.get("name"));
                this.setCaptureType(CaptureTypeEnum.getEnum((String) captureDevObj.get("devType")));
                this.setSwitchId( (String) captureDevObj.get("switchID"));
                this.setSwitchPort((int) captureDevObj.get("switchPort"));
                this.setMacAddr((String) captureDevObj.get("MACAddress"));
                this.setVlanId( (int) captureDevObj.get("VLAN_ID"));
                this.setEnabled((Boolean) captureDevObj.get("enabled"));

                try {
                        this.setIPAddress((String) captureDevObj.get("IPAddress"));
                } catch (UnknownHostException e) {
                        logger.warn("Unable to resolve Capture Device IP Address " + captureDevObj.get("IPAddress"));
                }
        }

        public long getReferenceCount(){
                return this.referenceCount;
        }

        public void setReferenceCount(final long refCount) {
                this.referenceCount = refCount;
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

        public CaptureTypeEnum getCaptureType(){
                return this.captureType;
        }

        public void setCaptureType(CaptureTypeEnum type){
                this.captureType = type;
        }

        public String getSwitchId() {
                return switchId;
        }

        public void setSwitchId(String switchId) {
                this.switchId = switchId;
        }

        public int getSwitchPort() {
                return switchPort;
        }

        public void setSwitchPort(int switchPort) {
                this.switchPort = switchPort;
        }

        public long getIpAddr() {
                return ipAddr;
        }

        public void setIpAddr(long ipAddr) {
                this.ipAddr = ipAddr;
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

        public void setIPAddress(String dottedString) throws UnknownHostException {
                this.ipAddr = pack(InetAddress.getByName(dottedString).getAddress());
        }

        public String getIPAddressStr() {
                try {
                        return InetAddress.getByAddress(unpack(this.ipAddr)).getHostAddress();
                } catch (UnknownHostException e) {
                        return "Unknown";
                }
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

        public boolean isEnabled() {
                return enabled;
        }

        public void setEnabled(boolean enabled) {
                this.enabled = enabled;
        }

        public Boolean getEnabled() {
                return this.enabled;
        }

        public BasicDBObject getAsDocument() {
                BasicDBObject document = new BasicDBObject();

                document.put("refCount",   this.getReferenceCount());
                document.put("name",       this.getName());
                document.put("devType",    this.getCaptureType().toString());
                document.put("IPAddress",  this.getIPAddressStr());
                document.put("switchID",   this.getSwitchId());
                document.put("switchPort", this.getSwitchPort());
                document.put("MACAddress", this.getMacAddr());
                document.put("VLAN_ID",    this.getVlanId());
                document.put("enabled",    this.getEnabled());

                return document;
        }

        public static boolean adjustReferenceCount(final ReferenceCountEnum adjType, String switchEntryId) throws NotFoundException {

                DB database = TappingApp.getDatabase();
                DBCollection table = database.getCollection(DatabaseNames.getCaptureDevTableName());

                // Look for the CaptureDev object by object ID in the database
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

        public String toString() {
                return "Capture Dev: " + this.getName() + "  IP " + this.getIPAddressStr() + " Type " + this.getCaptureType() + " Enabled " + this.getEnabled();
        }

        public void generateActionList(List<Action> actions, Node node, TappingAppLogic tappingAppLogic, TapPolicy tapPolicy) throws NotFoundException {

            // Get the switch ID and the switch port that connects to the capture device
            final String switchId = this.getSwitchId();
            final int switchPort  = this.getSwitchPort();

            // Get the switch entry associated with the CaptureDevice
            SwitchEntry switchEntry = tappingAppLogic.getSwitchEntryById(switchId);

            // Get the OPFSwitch for the captureDev's SwitchEntry
            OPFSwitch captureDevOPFSwitch = switchEntry.getOPFSwitch();

            logger.info("CaptureDevOPFSwitch " + captureDevOPFSwitch);
            // And get its data path ID
            String captureDevOPFSwitchId = captureDevOPFSwitch.getDataPathDesc();

            // Get the switch node for the captureDev
            Node captureDevSwitchNode = null;
            logger.info("**** Comparing node with ID '" + node.getNodeIDString() + "' to switch ID '" + captureDevOPFSwitchId + "'");

            if (node.getNodeIDString().equals(captureDevOPFSwitchId)) {
                logger.info("Nodes match");
                captureDevSwitchNode = node;
            }
            else {
                logger.info("nodes don't match");
                // Get the capture device switch
                 try {
                    captureDevSwitchNode = tapPolicy.getODLSwitchById(switchId);
                } catch (OperationNotSupportedException e) {
                    logger.error("TapPolicy::getODLSwitchById is not supported", e);
                }
                if (captureDevSwitchNode == null) {
                    logger.info("Unable to locate ODL Switch ID " + switchId);
                    throw new NotFoundException();
                }
            }

            logger.info("Looking for captureDevSwitchNode for port " + switchPort);
            // Get the NodeConnector for the Switch and Port that its connected to
            NodeConnector captureDevNode =
                    tappingAppLogic.getODLNodeConnectorByNodeAndPort(captureDevSwitchNode, switchPort);

            logger.info("CaptureDevNode: " + captureDevNode);

            // Create an action to Output packets to the captureDev switch and port
            // and add the action to the action list
            actions.add(new Output(captureDevNode));
        }
}
