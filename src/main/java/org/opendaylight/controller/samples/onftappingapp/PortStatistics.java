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

import java.util.Date;

public class PortStatistics {
        private String objectId = "";
        private SwitchVersionEnum version = SwitchVersionEnum.UNDEFINED;
        private Date updateTime;
        private long timeDuration;
        private int packetCount;
        private int byteCount;

        public PortStatistics(){
        }

        public String getObjectId() {
       return objectId;
        }

        public void setObjectId(String objectId) {
      this.objectId = objectId;
        }

        public SwitchVersionEnum getVersion() {
      return version;
        }

        public void setVersion(SwitchVersionEnum version) {
       this.version = version;
        }

        public Date getUpdateTime() {
       return updateTime;
        }

        public void setUpdateTime(Date updateTime) {
       this.updateTime = updateTime;
        }

        public long getTimeDuration() {
       return timeDuration;
        }

        public void setTimeDuration(long timeDuration) {
       this.timeDuration = timeDuration;
        }

        public int getPacketCount() {
       return packetCount;
        }

        public void setPacketCount(int packetCount) {
        this.packetCount = packetCount;
        }

        public int getByteCount() {
        return byteCount;
    }

        public void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }
}
