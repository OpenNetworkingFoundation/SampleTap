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

package org.opendaylight.controller.samples.onftappingapp.internal;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);


    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    public Object[] getImplementations() {
        Object[] res = { ONFTappingAppImpl.class };
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies
     * is required.
     *
     * @param c dependency manager Component object, used for
     * configuring the dependencies exported and imported
     * @param imp Implementation class that is being configured,
     * needed as long as the same routine can configure multiple
     * implementations
     * @param containerName The containerName being configured, this allow
     * also optional per-container different behavior if needed, usually
     * should not be the case though.
     */
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(ONFTappingAppImpl.class)) {
            // export the service
            c.setInterface(new String[] { IInventoryListener.class.getName(),
                    IfNewHostNotify.class.getName() }, null);

                c.add(createContainerServiceDependency(containerName).setService(
                        ISwitchManager.class).setCallbacks("setSwitchManager",
                        "unsetSwitchManager").setRequired(false));

                c.add(createContainerServiceDependency(containerName).setService(
                        IForwardingRulesManager.class).setCallbacks(
                        "setForwardingRulesManager", "unsetForwardingRulesManager")
                        .setRequired(false));

               c.add(createServiceDependency().setService(IStatisticsManager.class)
                       .setCallbacks("setStatisticsManager", "unsetStatisticsManager").setRequired(false));

        }
    }
}
