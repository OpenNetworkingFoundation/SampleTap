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

public class PortStatus {
        private final int portNum;
        private final PortTypeEnum portType;
        private final LinkStateEnum  portState;
        private final boolean blocked;
        private final boolean isLive;

        public PortStatus(final int portNum, final PortTypeEnum portType, final LinkStateEnum portState, final boolean blocked, final boolean isLive) {
                this.portNum   = portNum;
                this.portType  = portType;
                this.portState = portState;
                this.blocked   = blocked;
                this.isLive    = isLive;
        }

        public LinkStateEnum getPortState() {
                return portState;
        }

        public int getPortNum() {
                return portNum;
        }

        public PortTypeEnum getPortType() {
                return portType;
        }

        public boolean isBlocked() {
                return blocked;
        }

        public boolean isLive() {
                return isLive;
        }
}
