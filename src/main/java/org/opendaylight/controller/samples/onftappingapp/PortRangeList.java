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

import javax.naming.OperationNotSupportedException;

import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class PortRangeList {

        List<PortRange> portList =  null;

        public PortRangeList() {
             portList = new ArrayList<PortRange>();
        }

        // Argument format is "[80-100, 20-22, 8080-80801]
        public PortRangeList(String portListStr) throws IllegalArgumentException {
                this.portList = new ArrayList<PortRange>();

                if (portListStr.charAt(0) != '[') {
                        throw new IllegalArgumentException();
                }

                if (portListStr.charAt(portListStr.length() - 1) != ']') {
                        throw new IllegalArgumentException();
                }

                String [] portRanges = portListStr.substring(1, portListStr.length() - 1).split(",");
                for (String portRangeStr : portRanges) {
                        this.portList.add(new PortRange(portRangeStr));
                }
        }

        public String toString() {
                StringBuilder str = new StringBuilder();
                str.append("[");

                if (this.portList.isEmpty())
                        str.append("Empty");
                else {
                        boolean first = true;

                        for (PortRange portRange : this.portList) {
                                if (!first) str.append(',');
                                first = false;
                                str.append(portRange.toString());
                        }
                }

                str.append("]");
                return str.toString();
        }

        public BasicDBObject getAsDocument() {
                BasicDBObject document = new BasicDBObject();

                List<Object> dbList = new BasicDBList();
                for (PortRange portRange : this.portList) {
                        dbList.add(portRange.getAsDocument());
                }

                document.put("portRanges", dbList);
                return document;
        }

        public List<Match> getODLMatchList() {

            // Returns a list of ODL Match objects
            List<Match> matchList = new ArrayList<Match>(1);

            for (PortRange portRange : this.portList) {
                for (Short port = portRange.getStartPort(); port <= portRange.getEndPort(); port++) {

                    // OpenDaylight Match object
                    Match match = new Match();
                    match.setField(MatchType.TP_SRC, port);
                    matchList.add(match);
                }
            }

            return matchList;
        }

        public void addODLMatch(Match match) throws OperationNotSupportedException {
            throw new OperationNotSupportedException();
            // TODO Auto-generated method stub
        }
}

