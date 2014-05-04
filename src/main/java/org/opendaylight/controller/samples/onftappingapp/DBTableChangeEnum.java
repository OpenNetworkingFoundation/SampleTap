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

public enum DBTableChangeEnum {
    TAP_POLICY("TAP_POLICY"),
    MATCH_CRITERIA("MATCH_CRITERIA"),
    SWITCH_ENTRY("SWITCH_ENTRY"),
    PORT_CHAIN("PORT_CHAIN"),
    CAPTURE_DEVICE("CAPTURE_DEVICE"),
    NEXT_HOP_SWITCH("NEXT_HOP_SWITCH");

    private String description;

    private DBTableChangeEnum(String description) {
            this.description = description;
    }

    /**
     * Prints the description associated to the code value
     */
    @Override
    public String toString() {
            return description;
    }

    public int calculateConsistentHashCode() {
        if (this.description != null) {
                return this.description.hashCode();
        } else {
                return 0;
        }
    }
}