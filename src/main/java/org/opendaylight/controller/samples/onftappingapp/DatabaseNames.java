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

public class DatabaseNames {
        private final static String databaseName = "TappingApp";                  // Database name

        // Collection table names
        private final static String loggerTableName           = "ApplicationLog";

        private final static String tapPolicyTableName        = "TapPolicies";
        private final static String matchCriteriaTableName    = "MatchCriteria";
        private final static String switchEntryTableName      = "SwitchEntries";
        private final static String unassignedSwitchTableName = "SwitchEntries";
        private final static String nextHopSwitchTableName    = "NextHopSwitch";
        private final static String captureDevTableName       = "CaptureDev";
        private final static String portChainTableName        = "PortChains";

        // private final static String rewriteParamsTableName    = "RewriteParams";
        // private final static String switchAndPortTableName    = "SwichAndPort";

        public static String getLoggerTableName() {
                return loggerTableName;
        }

        public static String getDatabaseName() {
                return databaseName;
        }

        public static String getTapPolicyTableName() {
                return tapPolicyTableName;
        }

        public static String getMatchCriteriaTableName() {
                return matchCriteriaTableName;
        }

        public static String getSwitchEntryTableName() {
                return switchEntryTableName;
        }

        public static String getNextHopSwitchTableName() {
                return nextHopSwitchTableName;
        }

        public static String getCaptureDevTableName() {
                return captureDevTableName;
        }

        public static String getPortChainTableName() {
                return portChainTableName;
        }

        public static String getUnassignedSwitchTableName() {
                return unassignedSwitchTableName;
        }
}
