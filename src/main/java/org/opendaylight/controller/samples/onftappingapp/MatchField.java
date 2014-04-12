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

import javax.naming.OperationNotSupportedException;

import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.samples.onftappingapp.internal.ONFTappingAppImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;

public class MatchField {

        private static Logger logger = LoggerFactory.getLogger(ONFTappingAppImpl.class);

        // Match types we support
        public static final String UNDEFINED      = "UNDEFINED";
        public static final String ETH_TYPE       = "ETHER_TYPE";
        public static final String IP_PROTO       = "IP_PROTO";
        public static final String IP_TOS         = "IP_TOS";
        public static final String VLAN_TAG       = "VLAN_TAG";

        public static final String SOURCE_IP      = "SOURCE_IP";
        public static final String SOURCE_MAC     = "Source_MAC";

        public static final String DEST_IP        = "DEST_IP";
        public static final String DEST_MAC       = "DEST_MAC";

        public static final String PROTO_SVC      = "PROTO_SVC";
        public static final String PROTO_PORT     = "PROTO_PORT";

        private String type = "";    // type
        private Object value = null; // Variant type for the object's value

        public MatchField() {
        }

        public MatchField(String type, String value) throws IllegalArgumentException {
            switch (type) {
            case SOURCE_IP:
            case DEST_IP:
                this.type = type;
                this.value = new AddressDesc(value);
                break;

            case PROTO_PORT:
                this.type = type;
                this.value = new PortRangeList(value);
                break;

            default:
                this.type = type;
                this.value = value;
                break;
        }
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getValue() {
            return (value);
        }

        public void setValue(Object value){
            this.value = value;
        }

        public String toString() {
            return type.toString() + " " + value;
        }

        public BasicDBObject getAsDocument() {
            BasicDBObject document = new BasicDBObject();

            document.put("type",  this.getType());

            switch (this.getType()) {
            case PROTO_PORT:
                // It is OK to ignore the warning because we checked the type
                PortRangeList portRangeList = (PortRangeList) this.getValue();

                document.put("value", portRangeList.getAsDocument());
                break;

            case SOURCE_IP:
            case DEST_IP:
                AddressDesc addrDesc = (AddressDesc) this.getValue();
                document.put("value", addrDesc.getAsDocument());
                break;

            default:
                document.put("value", this.getValue());
            }

            return document;
        }

        public void addODLMatch(Match match) throws UnknownHostException, IllegalArgumentException {

            switch (this.getType()) {
            case UNDEFINED:
                throw new IllegalArgumentException();

            case ETH_TYPE:
                logger.info("ETH_TYPE '" + this.getValue() + "'");

                match.setField(MatchType.DL_TYPE, Short.parseShort((String) (this.getValue())));
                break;

            case IP_PROTO:
                match.setField(MatchType.NW_PROTO, (byte) Short.parseShort((String) (this.getValue())));
                break;

            case IP_TOS:
                match.setField(MatchType.NW_TOS, (byte) Short.parseShort((String) (this.getValue())));
                break;

            case VLAN_TAG:
                match.setField(MatchType.DL_VLAN, Short.parseShort((String) (this.getValue())));
                break;

            case SOURCE_IP:
                // Can throw UnknownHostException
                InetAddress sourceIp = InetAddress.getByName((String) this.getValue());
                match.setField(MatchType.NW_SRC, sourceIp);
                break;

            case SOURCE_MAC:
                match.setField(MatchType.DL_SRC, this.getMACAddress());
                break;

            case DEST_IP:
                // Can throw UnknownHostException
                InetAddress destIp = InetAddress.getByName((String) this.getValue());
                match.setField(MatchType.NW_DST, destIp);
                break;

            case DEST_MAC:
                match.setField(MatchType.DL_DST, this.getMACAddress());
                break;

            case PROTO_SVC:
                match.setField(MatchType.TP_SRC, Short.parseShort((String) (this.getValue())));
                break;

            case PROTO_PORT:
                PortRangeList portRangeList = (PortRangeList) this.getValue();
                if (portRangeList == null)
                    throw new IllegalArgumentException();

                try {
                    portRangeList.addODLMatch(match);
                } catch (OperationNotSupportedException e) {
                    logger.error("Exception " , e);
                }
            }
        }

//        public List<Match> getODLMatchList() throws UnknownHostException, IllegalArgumentException {
//
//            // Returns a list of ODL Match objects
//            List<Match> matchList = new ArrayList<Match>(1);
//
//            // OpenDaylight Match object
//            Match match = new Match();
//
//            switch (this.getType()) {
//            case UNDEFINED:
//                throw new IllegalArgumentException();
//
//            case ETH_TYPE:
//                logger.info("ETH_TYPE '" + this.getValue() + "'");
//
//                match.setField(MatchType.DL_TYPE, Short.parseShort((String) (this.getValue())));
//                matchList.add(match);
//                break;
//
//            case IP_PROTO:
//                match.setField(MatchType.NW_PROTO, (byte) Short.parseShort((String) (this.getValue())));
//                matchList.add(match);
//                break;
//
//            case IP_TOS:
//                match.setField(MatchType.NW_TOS, (byte) Short.parseShort((String) (this.getValue())));
//                matchList.add(match);
//                break;
//
//            case VLAN_TAG:
//                match.setField(MatchType.DL_VLAN, Short.parseShort((String) (this.getValue())));
//                matchList.add(match);
//                break;
//
//            case SOURCE_IP:
//                // Can throw UnknownHostException
//                InetAddress sourceIp = InetAddress.getByName((String) this.getValue());
//                match.setField(MatchType.NW_SRC, sourceIp);
//                matchList.add(match);
//                break;
//
//            case SOURCE_MAC:
//                match.setField(MatchType.DL_SRC, this.getMACAddress());
//                matchList.add(match);
//                break;
//
//            case DEST_IP:
//                // Can throw UnknownHostException
//                InetAddress destIp = InetAddress.getByName((String) this.getValue());
//                match.setField(MatchType.NW_DST, destIp);
//                matchList.add(match);
//                break;
//
//            case DEST_MAC:
//                match.setField(MatchType.DL_DST, this.getMACAddress());
//                matchList.add(match);
//                break;
//
//            case PROTO_SVC:
//                match.setField(MatchType.TP_SRC, Short.parseShort((String) (this.getValue())));
//                matchList.add(match);
//                break;
//
//            case PROTO_PORT:
//                PortRangeList portRangeList = (PortRangeList) this.getValue();
//                if (portRangeList == null)
//                    throw new IllegalArgumentException();
//
//                return portRangeList.getODLMatchList();
//            }
//
//            return matchList;
//        }

        // Returns the MAC address as a byte array
        private byte[] getMACAddress() {

            String [] macAddrStr = ((String) this.getValue()).split(":");

            short index = 0;
            byte [] macAddrBytes = new byte[macAddrStr.length];

            for (String byteStr : macAddrStr) {
                macAddrBytes[index++] = Integer.decode("0x" + byteStr).byteValue();
            }

            return macAddrBytes;
        }
}