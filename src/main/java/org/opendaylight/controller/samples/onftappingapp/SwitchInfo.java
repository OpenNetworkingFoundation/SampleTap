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

public class SwitchInfo {
         public int numPorts;

         public List<LinkStateEnum>  portStateList;
         public List<PortStatistics> portStatisticsList;

         public SwitchInfo() {
                 this.numPorts = 24;
                 this.portStateList = new ArrayList<LinkStateEnum>(numPorts);
                 this.portStatisticsList = new ArrayList<PortStatistics>(numPorts);

                 for (int port = 0; port < this.numPorts; port++) {
                         LinkStateEnum portState = LinkStateEnum.PORT_DOWN;
                         if ((port % 5) == 0) portState = LinkStateEnum.PORT_UP;
                         portStateList.add(portState);
                 }

                 for (int port = 0; port < this.numPorts; port++) {
                         PortStatistics portStats = new PortStatistics();
                         // Sawtooth pattern between 0 and 100000 with some randomness
                         if (portStats.getByteCount() > 1000000)
                                 portStats.setByteCount(0);

                         portStats.setByteCount((portStats.getByteCount() + (int) (Math.random() * 100)));

                         portStatisticsList.add(portStats);
                 }
         }
}
