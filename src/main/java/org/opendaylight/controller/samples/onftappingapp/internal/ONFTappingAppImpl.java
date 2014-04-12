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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;

import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.SwitchConfig;
import org.opendaylight.controller.samples.onftappingapp.DBAuthenticationException;
import org.opendaylight.controller.samples.onftappingapp.DatabaseConnectException;
import org.opendaylight.controller.samples.onftappingapp.TappingApp;

/**
 * This class implements port tapping for hosts connected to the managed devices.
 * <br/>
 * The work flow is as follows:
 * This module listens for new switch nodes from the
 * {@link  org.opendaylight.controller.switchmanager.IInventoryListener }
 * service and on discovering a new switch it adds flow rules based on the config
 */
public class ONFTappingAppImpl implements IfNewHostNotify, IInventoryListener {

    private static Logger logger = LoggerFactory.getLogger(ONFTappingAppImpl.class);

    // Our TappingApp
    TappingApp tappingApp = null;

    // Web-server for TappingAPp GUI
    Router router = null;

    // SwitchManager from ODL. Used to configure switches and obtain their properties
    protected ISwitchManager switchManager = null;

    // Forwarding Rules Manager
    private IForwardingRulesManager forwardingRulesManager = null;

    // Statistics Manager
    private IStatisticsManager statisticsManager = null;

    // Function called when the plugin gets activated
    public void startUp() {
        logger.info("ONFTappingApp start up");
        logger.info("Application directory is " + System.getProperty("user.dir"));

        try {
            // Construct an instance of the TappingApp
            tappingApp = new TappingApp("ONS Tapping App", "localhost", this);

            // Construct an instance of the webs-server for the GUI
            router = new Router(tappingApp);

        } catch (DBAuthenticationException authException) {
            logger.error("Unable to connect to database " + authException.toString());
            authException.printStackTrace();

        } catch (DatabaseConnectException dbException) {
            logger.error("Unable to connect to database " + dbException.toString());
            dbException.printStackTrace();
        }
    }

   // Function called when the plugin is stopped
    public void shutDown() {
        logger.info("ONFTappingApp shutdown");
        logger.info("Destroy all the switch Rules");
    }

    public void setSwitchManager(ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    public ISwitchManager getSwitchManager() {
        return this.switchManager;
    }

    public void unsetSwitchManager(ISwitchManager switchManager) {
        if (this.switchManager == switchManager) {
            this.switchManager = null;
        }
    }

    public void setForwardingRulesManager(IForwardingRulesManager forwardingRulesManager) {
        logger.debug("Setting ForwardingRulesManager");
        this.forwardingRulesManager = forwardingRulesManager;
    }

    public void unsetForwardingRulesManager(IForwardingRulesManager forwardingRulesManager) {
        if (this.forwardingRulesManager == forwardingRulesManager) {
            this.forwardingRulesManager = null;
        }
    }

    public IForwardingRulesManager getForwardingRulesManager() {
        return forwardingRulesManager;
    }

    public IStatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public void setStatisticsManager(IStatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void notifyHTClient(HostNodeConnector arg0) {
       logger.info("notifyHTClient ignored");
    }

    @Override
    public void notifyHTClientHostRemoved(HostNodeConnector arg0) {
        logger.info("notifyHTClientHostRemoved ignored");
    }

    @Override
    public void notifyNode(Node node, UpdateType type, Map<String, Property> propMap) {
        logger.info("notifyNode: Type " + type);

        if (node == null) {
            logger.info("Node is null ");
            return;
        }

        // We only support OpenFlow switches for now
        if (!node.getType().equals(Node.NodeIDType.OPENFLOW)) {
            logger.debug("OpenFlow node {} added. Initialize flows", node);
            return;
        }

        // Delegate the processing to the tappingApp
        tappingApp.switchChanged(type, node);
    }

    // Print all we can find out about a node
    private void logNodeInfo(Node node, Map<String, Property> propMap) {
        logger.info("logNodeInfo Node {} added ", node);

        SwitchConfig config = this.switchManager.getSwitchConfig(node.getNodeIDString());
        logger.info("Config " + config);

        Map<String,Property> nodeProps = this.switchManager.getNodeProps(node);
        logger.info("NodeProperties " + nodeProps);

        // Get the set of NodeConnectors for the Node (ie. the set of switch ports for the switch)
        Set<NodeConnector> switchPorts = switchManager.getNodeConnectors(node);
        for (NodeConnector switchPort : switchPorts) {
            Map<String, Property> ncprop = switchManager.getNodeConnectorProps(switchPort);
            logger.info("NC props " + ncprop);
        }

        logger.info("Node {} ID {}", node, node.getNodeIDString());
        if (propMap != null)
                logger.info("Properties " + propMap);

        List<Switch> switches = this.switchManager.getNetworkDevices();
        logger.info("Switches " + switches);

        List<Switch> networkDevs = switchManager.getNetworkDevices();
        if (networkDevs == null) {
            logger.debug("No OF nodes learned yet in {}", node);
        }
        else {
            logger.info("Number of net dev nodes " + networkDevs.size());
            for (Switch netDevNode : networkDevs) {
                logger.info("Network Dev Node: " + netDevNode);
            }
        }

        logger.info("Controller props " + switchManager.getControllerProperties());

        Set<Node> nodes = switchManager.getNodes();
        logger.info("Nodes " + nodes);

        Iterator<Node> nit = nodes.iterator();
        while (nit.hasNext()) {
             logger.info("Node: " + nit.next());
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap) {

        logger.info("notifyNodeConnector");

        if (nodeConnector == null) {
                logger.info("NodeConnector is null");
            return;
        }
        Node node = nodeConnector.getNode();
        this.logNodeInfo(node, propMap);

        switch (type) {
        case ADDED:
           logger.info("NodeConnector {} added ", nodeConnector);
           break;

        case CHANGED:
            logger.info("NodeConnector {} changed ", nodeConnector);
            break;

        case REMOVED:
            logger.info("NodeConnector {} removed, doing a cleanup", nodeConnector);
            break;

        default:
            logger.info("Unknown type received " + type);
            break;
        }
    }

    // Function called by the dependency manager when all the required
    // dependencies are satisfied
    void init() {
        logger.info("init called");
        startUp();
    }

    // Function called by the dependency manager when at least one
    // dependency become unsatisfied or when the component is shutting
    // down because for example bundle is being stopped.
    void destroy() {
        logger.info("destroy called");
    }

    // Function called by dependency manager after "init ()" is called
    // and after the services provided by the class are registered in
    //the service registry
    void start() {
       logger.info("start called");
    }

    // Function called by the dependency manager before the services
    // exported by the component are unregistered, this will be
    // followed by a "destroy ()" calls
    void stop() {
        logger.info("start called");
    }
}
