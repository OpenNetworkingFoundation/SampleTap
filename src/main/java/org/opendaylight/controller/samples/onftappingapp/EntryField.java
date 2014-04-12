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

public class EntryField {
        public final static String UNDEFINED = "Undefined";
        public final static String IP_ADDR   = "IPAddress";  // value in map is a long
        public final static String MAC_ADDR  = "MACAddress"; // value in map is a String
        public final static String VLAN_ID   = "VLAN_ID";    // value in map is an Integer

        private String type;
        private Object value;  // Variant type for the object's value

        public EntryField(){
        }

        public EntryField(final String type, Object value){
                this.type  = type;
                this.value = value;
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
}
