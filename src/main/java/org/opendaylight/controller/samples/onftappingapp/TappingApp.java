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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.transaction.NotSupportedException;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import org.bson.types.ObjectId;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.samples.onftappingapp.internal.ONFTappingAppImpl;

public class TappingApp {

        private static Logger logger = LoggerFactory.getLogger(ONFTappingAppImpl.class);

        // We use fatal in our application so get a marker for FATAL
        static Marker fatal = MarkerFactory.getMarker("FATAL");

        // Application global variables
        private static Properties applicationProps = new Properties(System.getProperties());
        private static String mongoDBUsername = "";
        private static String mongoDBPassword = "";
        private static String wwwRootdir = "";

        private final String appName;
        private final String hostname;
        private final String version = "1.0";

        private boolean captureARP = false;

        private static int mongoDBPort = 27017;       // Default MongoDB Port
        private MongoClient mongoClient;              // Our MongoDB (Client) Connector
        private static DB database;                   // Our handle to MongoDB database

        //  Instantiate the class that implements the application logic for pushing flows into ODL
        private TappingAppLogic tappingAppLogic = new TappingAppLogic(this);

        private ONFTappingAppImpl onfTappingAppImpl = null;

        // A Map<DataPathDescString, OPFSwitch> of unassigned OPF switches
        Map<String, OPFSwitch> unassignedOPFSwitchMap = new HashMap<String, OPFSwitch>();

        // A Map<DataPathDescString, OPFSwitch> of assigned OPF switches
        Map<String, OPFSwitch> assignedOPFSwitchMap = new HashMap<String, OPFSwitch>();

        // The ruleDB is a Map of Nodes to an ArrayList of FlowEntries. The index into the list is the port number
        private Map<Node, List<FlowEntry>> rulesDB = new HashMap<Node, List<FlowEntry>>();

        // Return codes from the programming of the perHost rules into the switch
        public enum RulesProgrammingReturnCode {
            SUCCESS, FAILED_FEW_SWITCHES, FAILED_ALL_SWITCHES, FAILED_WRONG_PARAMS
        }

        // Constructor
        public TappingApp(String appName, String hostname, ONFTappingAppImpl onfTappingAppImpl) throws DBAuthenticationException, DatabaseConnectException {
            this.appName           = appName;
            this.hostname          = hostname;
            this.onfTappingAppImpl = onfTappingAppImpl;

            // Configure the logging system
            PropertyConfigurator.configure("log4j.properties");
            BasicConfigurator.configure();

            // Announce that the application has been launched
            logger.info("Tapping App: " + this.getName() + " version: " + this.getVersion());

            // Load application configuration from the configuration file in CLASSPATH
            try {
                    loadConfigurationFromFile();
            } catch (IOException e) {
                    logger.error(fatal, "Unable to load ONS Tapping Application configuration file");
                    logger.debug("Exception:", e);
                    return;
            }

            // Set the mongo DB port if it is specified in the configuration file
            if (System.getProperties().containsKey("MongoDBPort"))
                    setMongoDBPort(Integer.getInteger("MongoDBPort"));

            try {
                    // MongoDB username and password are loaded from the application's configuration file
                    connectToDatabase(mongoDBUsername, mongoDBPassword);

            } catch (DBAuthenticationException authException) {
                    logger.error("Unable to connect to database " + authException.toString());
                    throw authException;

            } catch (DatabaseConnectException dbException) {
                    logger.error("Unable to connect to database " + dbException.toString());
                    throw dbException;
            }

            // TODO - remove after integration
            //this.purgeObjectModelFromDatabase();

            // TODO - remove after integration
            // Make stuff up for the GUI integration
            // makeFakeOPFSwitches();
        }

        // Class destructor - call on shutdown
        public void close() {

            try {
                saveConfigurationToFile();
            } catch (IOException e) {
                logger.error("Unable to save ONS Tapping Application configuration file");
                logger.debug("Exception:", e);
            }
        }

        public static int getMongoDBPort() {
            return mongoDBPort;
        }

        public void setMongoDBPort(int i_mongoDBPort) {
            mongoDBPort = i_mongoDBPort;
        }

        public static DB getDatabase() {
                return database;
        }

        // Getters and setters
        public String getName() {
                return appName;
        }

        public String getHostname() {
                return hostname;
        }

        public String getVersion() {
                return version;
        }

        public boolean getCaptureARP() {
                return captureARP;
        }

        public void setCaptureARP(boolean captureARP) {
                this.captureARP = captureARP;

                logger.info("ARP Capture was " + (captureARP ? "Enabled" : "Disabled"));
        }

        public static String getWwwRootdir() {
            return wwwRootdir;
        }

        private void loadConfigurationFromFile() throws IOException {
            // now load properties from last invocation
            FileInputStream in  = new FileInputStream("ONSTappingApp.config");
            applicationProps.load(in);
            in.close();

            // Store the application properties in the system properties
            System.setProperties(applicationProps);

            mongoDBUsername = applicationProps.getProperty("MongoDBUsername");
            mongoDBPassword = applicationProps.getProperty("MongoDBPassword");
            wwwRootdir      = applicationProps.getProperty("www-rootdir");

            logger.info("MongoDB credentials: username '" + mongoDBUsername + "' password '" + mongoDBPassword + "'");
            logger.info("WWW Root Dir: " + wwwRootdir);
        }

        // Store the application properties to the configuration file
        private static void saveConfigurationToFile() throws IOException {
            FileOutputStream out = new FileOutputStream("ONSTappingApp.config");

            Properties properties = new Properties();
            properties.setProperty("MongoDBUsername", mongoDBUsername);
            properties.setProperty("MongoDBPassword", mongoDBPassword);

            String mongoPort = Integer.toString(TappingApp.getMongoDBPort());
            properties.setProperty("MongoDBPort", mongoPort);

            properties.store(out, "Settings for the ONS Tapping Application");
            out.close();
        }


        public void connectToDatabase(String dbUsername, String dbPassword) throws DatabaseConnectException, DBAuthenticationException {
                try {
                        mongoClient = new MongoClient("localhost", mongoDBPort);

                        database = mongoClient.getDB(DatabaseNames.getDatabaseName());

                        // If the username and password are set then authenticate against the DB
                        if (dbUsername.isEmpty() || dbPassword.isEmpty()) {
                                logger.info("MongoDB username or password are not set, so database authentication is not used");
                        }
                        else {
                                boolean auth = database.authenticate(dbUsername, dbPassword.toCharArray());
                                if (!auth) {
                                        throw new DBAuthenticationException();
                                }
                        }

                        logger.info("Connected to configuration database " + database.toString());
                } catch (UnknownHostException e) {
                        logger.error("Error while connecting to Database", e);
                        throw new DatabaseConnectException();
                }
        }

        public void purgeObjectModelFromDatabase() {
                DBCollection tpTable = database.getCollection(DatabaseNames.getTapPolicyTableName());
                tpTable.drop();

                DBCollection mcTable = database.getCollection(DatabaseNames.getMatchCriteriaTableName());
                mcTable.drop();

                DBCollection seTable = database.getCollection(DatabaseNames.getSwitchEntryTableName());
                seTable.drop();

                DBCollection nhsTable = database.getCollection(DatabaseNames.getNextHopSwitchTableName());
                nhsTable.drop();

                DBCollection pcTable = database.getCollection(DatabaseNames.getPortChainTableName());
                pcTable.drop();

                DBCollection cdTable = database.getCollection(DatabaseNames.getCaptureDevTableName());
                cdTable.drop();

                DBCollection logTable = database.getCollection(DatabaseNames.getLoggerTableName());
                logTable.drop();
        }

        // Return a list containing all the Tap Policies by loading them from the database
        public List<TapPolicy> getAllTapPolicies() {

                DBCollection tpTable = database.getCollection(DatabaseNames.getTapPolicyTableName());
                DBCursor cursor = tpTable.find();

                List<TapPolicy> tapPolicyList = new ArrayList<TapPolicy>(cursor.size());

                try {
                   while(cursor.hasNext()) {
                           // Get the object from the database
                           DBObject tapPolicyObj = cursor.next();

                           // Construct a new tap policy from the DBobject and add it to list
                           tapPolicyList.add(new TapPolicy(tapPolicyObj));
                   }
                } catch (NotFoundException e) {
                        logger.error("Object referenced by Tap Policy cannot be found", e);
                } finally {
                   cursor.close();
                }

                return tapPolicyList;
        }

        // // Return a list containing all the Match Criteria by loading them from the database
        public List<MatchCriteria> getAllMatchCriteria() {

                DBCollection mcTable = database.getCollection(DatabaseNames.getMatchCriteriaTableName());
                DBCursor cursor = mcTable.find();

                List<MatchCriteria> matchCriteriaList = new ArrayList<MatchCriteria>(cursor.size());

                try {
                   while(cursor.hasNext()) {
                           DBObject matchCriteriaObj = cursor.next();
                           MatchCriteria matchCriteria = new MatchCriteria(matchCriteriaObj);

                           matchCriteriaList.add(matchCriteria);

                   }
                } finally {
                   cursor.close();
                }

                return matchCriteriaList;
        }


        // Return a list containing all the SwitchEntries
        public List<SwitchEntry> getAllSwitchEntries() {

            DBCollection seTable = database.getCollection(DatabaseNames.getSwitchEntryTableName());
            DBCursor cursor = seTable.find();

            List<SwitchEntry> switchEntryList = new ArrayList<SwitchEntry>(cursor.size());

            try {
               while(cursor.hasNext()) {
                   DBObject switchEntryObj = cursor.next();

                   SwitchEntry switchEntry = new SwitchEntry(switchEntryObj, this);

                   // Add the new SwitchEntry to the list
                   switchEntryList.add(switchEntry);
               }
            } finally {
               cursor.close();
            }

            return switchEntryList;
        }

        private void makeFakeOPFSwitches() {

            OPFSwitch opfSwitch1 = new OPFSwitch();

            opfSwitch1.setIPAddress("10.2.34.2");
            opfSwitch1.setDescription("Fake Switch 1");
            opfSwitch1.setMACAddress("aa:bb:cc:dd:ee:11");
            opfSwitch1.setMFRDescription("WTL Fake Switch");
            opfSwitch1.setHardwareDesc("Fake hardware");
            opfSwitch1.setSoftwareDesc("Fake software");
            opfSwitch1.setSerialNumber("ABCDEFG");
            opfSwitch1.setDataPathDesc("FGHD-JIVS-6DF1S-KLSAQ");

            // Simulate a 24 port switch
            List<PortDescription> fakePortDescList1 = new ArrayList<PortDescription>();
            for (int port = 0; port < 24; port++) {
                    fakePortDescList1.add(new PortDescription("Port " + port, "11:22:33:44:aa:bb", port));
            }
            opfSwitch1.setPortDescList(fakePortDescList1);

            // Add the OPFSwitch to the map
            unassignedOPFSwitchMap.put(opfSwitch1.getDataPathDesc(), opfSwitch1);

            OPFSwitch opfSwitch2 = new OPFSwitch();

            //ObjectId oid2 = new ObjectId();
            //opfSwitch2.setObjectId(oid2.toString());

            opfSwitch2.setIPAddress("10.2.15.143");
            opfSwitch2.setDescription("Fake Switch 2");
            opfSwitch2.setMACAddress("aa:bb:11:22:f4:1c");
            opfSwitch2.setMFRDescription("WTL Fake Switch");
            opfSwitch2.setHardwareDesc("Fake hardware");
            opfSwitch2.setSoftwareDesc("Fake software");
            opfSwitch2.setSerialNumber("ZXCVBNK");
            opfSwitch2.setDataPathDesc("JDSS-IODS-8FD8D-LKODQ");

            // Simulate a 24 port switch
            List<PortDescription> fakePortDescList2 = new ArrayList<PortDescription>();
            for (int port = 0; port < 24; port++) {
                    fakePortDescList2.add(new PortDescription("Port " + port, "bb:1c:f3:ac:12:ea", port));
            }
            opfSwitch2.setPortDescList(fakePortDescList2);

            // Add the OPF Switch to the map
            unassignedOPFSwitchMap.put(opfSwitch2.getDataPathDesc(), opfSwitch2);
        }


        // Return a list containing all the unassigned OPFSwitch objects
        public List<OPFSwitch> getAllUnassignedSwitchEntries() {

            logger.info("Getting App Unassigned Switch Entries");
            logger.info("Unassigned Map  " + this.unassignedOPFSwitchMap);
            logger.info("Assigned Map    " + this.assignedOPFSwitchMap);
            // Clone the Map into a list
            List<OPFSwitch> unassignedOPFSwitchList = new ArrayList<OPFSwitch>(unassignedOPFSwitchMap.values());

            return unassignedOPFSwitchList;
        }


        // Return a list containing all the NextHopSwitches
        public List<NextHopSwitch> getAllNextHopSwitches() {
            DBCollection nhsTable = database.getCollection(DatabaseNames.getNextHopSwitchTableName());
            DBCursor cursor = nhsTable.find();

            List<NextHopSwitch> nextHopSwitchList = new ArrayList<NextHopSwitch>(cursor.size());

            try {
               while(cursor.hasNext()) {
                       DBObject nhsObj = cursor.next();

                       NextHopSwitch nhs = new NextHopSwitch(nhsObj);

                       // Add the new SwitchEntry to the list
                       nextHopSwitchList.add(nhs);
               }
            } finally {
               cursor.close();
            }

            return nextHopSwitchList;
        }

        // Return a list containing all the Port Chain objects
        public List<PortChain> getAllPortChains() {
            DBCollection pcTable = database.getCollection(DatabaseNames.getPortChainTableName());
            DBCursor cursor = pcTable.find();

            List<PortChain> portChainList = new ArrayList<PortChain>(cursor.size());

            try {
               while(cursor.hasNext()) {
                   DBObject portChainObj = cursor.next();

                   // Construct a new port chain from the DB object
                   PortChain portChain = new PortChain(portChainObj);

                   // Add the new port chain to the list
                   portChainList.add(portChain);
               }
            } finally {
               cursor.close();
            }

            return portChainList;
        }

        // Return a list containing all the Capture Devices
        public List<CaptureDev> getAllCaptureDevices() {
                DBCollection pcTable = database.getCollection(DatabaseNames.getCaptureDevTableName());
                DBCursor cursor = pcTable.find();

                List<CaptureDev> captureDevList = new ArrayList<CaptureDev>(cursor.size());

                while(cursor.hasNext()) {
                        DBObject captureDevObj = cursor.next();
                        // Construct a new CaptureDev from the document
                        CaptureDev captureDev = new CaptureDev(captureDevObj);

                        // Add it to the list
                        captureDevList.add(captureDev);
                }

                return captureDevList;
        }


        // Returns a summary list of the Tap Policies that have been defined
        public List<TapPolicySummary> getTapPolicySummaries() {

                List<TapPolicySummary> summaryList = new ArrayList<TapPolicySummary>();

                DBCollection table = database.getCollection(DatabaseNames.getTapPolicyTableName());
                // Run a query to look for all the Tap Policies
                DBCursor cursor = table.find();

                while (cursor.hasNext()) {
                        DBObject doc = cursor.next();
                        TapPolicySummary summary = new TapPolicySummary((String) doc.get("_id").toString(), (String) doc.get("name"), (String) doc.get("description"), (Boolean) doc.get("enabled"));
                        summaryList.add(summary);
                }

                return summaryList;
        }


        public void assignSwitch(String OPFSwitchId, String switchEntryId) throws NotFoundException, OPFSwitchNotFoundException, OPFSwitchIsAssignedException {

                // Look for the OPF Switch in the assigned list
                OPFSwitch opfSwitch = getAssignedOPFSwitchById(OPFSwitchId);
                if (opfSwitch != null)
                        throw new OPFSwitchIsAssignedException();

                // Look for the OPF Switch in the unassigned list
                opfSwitch = getUnAssignedOPFSwitchById(OPFSwitchId);
                if (opfSwitch == null)
                        throw new OPFSwitchNotFoundException();

                // We found it. Does it think it is already assigned?
                if (opfSwitch.getAssigned() == true) {
                        // This OPFSwitch is already assigned
                        throw new OPFSwitchIsAssignedException();
                }

                // Look for the SwitchEntry by id
                SwitchEntry switchEntry = getSwitchEntryById(switchEntryId);
                logger.info("SwitchEntry " + switchEntry);

                // Finally, we can assign the OPF switch to the switch entry
                switchEntry.assignOPFSwitch(opfSwitch);

                // Update the database
                modifySwitchEntry(switchEntryId, switchEntry);

                // Move the OPFSwitch from the unassigned to the assigned map
                assignOPFSwitch(OPFSwitchId, opfSwitch);

                logger.info("Assigned OPFSwitch " + opfSwitch.getSerialNumber() + " at IP address " + opfSwitch.getIPAddressStr() + " to SwitchEntry with ID " +  switchEntryId);

                // Send the flows to the switch
                // TODO need to look up the node here so we can send the flows
                //this.switchChanged(UpdateType.CHANGED, node);
        }

        // Move the OPFSwitch from the unassigned to the assigned map
        private void assignOPFSwitch(String OPFSwitchId, OPFSwitch opfSwitch) {
                logger.info("Assigning OPF Switch " + opfSwitch);
                this.unassignedOPFSwitchMap.remove(OPFSwitchId);
                this.assignedOPFSwitchMap.put(OPFSwitchId, opfSwitch);
        }

        // Move the OPFSwitch from the assigned to the unassigned map
        private void unassignOPFSwitch(String OPFSwitchId, OPFSwitch opfSwitch) {
                logger.info("Unassigning OPF Switch " + opfSwitch);
                this.assignedOPFSwitchMap.remove(OPFSwitchId);
                this.unassignedOPFSwitchMap.put(OPFSwitchId, opfSwitch);
        }

        public void unassignSwitch(String OPFSwitchId, String switchEntryId) throws NotFoundException, OPFSwitchNotFoundException, OPFSwitchIsNotAssignedException {

                // Look for the OPF Switch in the unassigned list
                OPFSwitch opfSwitch = getUnAssignedOPFSwitchById(OPFSwitchId);
                if (opfSwitch != null)
                        throw new OPFSwitchIsNotAssignedException();

                // Look for the OPF Switch in the assigned list
                opfSwitch = getAssignedOPFSwitchById(OPFSwitchId);
                if (opfSwitch == null)
                        throw new OPFSwitchNotFoundException();

                // We found it. Does it think it is already unassigned?
                if (opfSwitch.getAssigned() == false) {
                        // This OPFSwitch is already assigned
                        throw new OPFSwitchIsNotAssignedException();
                }

                // Move the OPFSwitch from the assigned to the unassigned map
                unassignOPFSwitch(OPFSwitchId, opfSwitch);

                // Look for the SwitchEntry by id
                SwitchEntry switchEntry = getSwitchEntryById(switchEntryId);

                // Finally, we can un-assign the OPF switch from the switch entry
                switchEntry.unassignOPFSwitch(opfSwitch);

                // Update the database
                modifySwitchEntry(switchEntryId, switchEntry);

                logger.info("Unassigned OPFSwitch " + opfSwitch.getSerialNumber() + " at IP address " + opfSwitch.getIPAddressStr() + " from " +  switchEntryId);
        }

        // Add a new policy to the system.
        public void addTapPolicy(TapPolicy tapPolicy) throws DuplicateEntryException {

                // Check whether there is an existing policy with the same name
                if (tapPolicyExistsByName(tapPolicy.getName()))
                        throw new DuplicateEntryException();

                // Get the tap policy table
                DBCollection table = database.getCollection(DatabaseNames.getTapPolicyTableName());

                // Create a document containing the new tap policy
                BasicDBObject document = tapPolicy.getAsDocument();

                // Add the new tap policy document to the tap policy table
                table.insert(document);

                // Get the object ID from mongo and update it in the tap policy object
                ObjectId objectId = document.getObjectId("_id");
                tapPolicy.setObjectId(objectId.toString());

                logger.info("Added tap policy " + tapPolicy.getName() + " object ID " + tapPolicy.getObjectId());

                // Notify the application that the tap policy changed
                // tappingAppLogic.NotifyConfigChange(DBTableChangeEnum.TAP_POLICY, tapPolicy.getObjectId());
        }

        // Delete the TapPolicy with the specified objectId
        public void deleteTapPolicy(String objectId) throws NotFoundException {
            DBCollection table = database.getCollection(DatabaseNames.getTapPolicyTableName());

            // Create a query to look for the existing TapPolicy by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(objectId));
            DBObject dbObj = table.findOne(searchQuery);

            if (dbObj == null)
                throw new NotFoundException();

            // We are deleting this tapPolicy, so remove references to other objects
            TapPolicy.removeObjectReferences(dbObj);

            // Remove the TapPolicy from the database
            table.remove(searchQuery);

            logger.info("Removed tap policy " + (String) dbObj.get("name"));

            // Notify the application that the tap policy changed
            // TODO - add objectID
            // tappingAppLogic.NotifyConfigChange(DBTableChangeEnum.TAP_POLICY);
        }

        // Return true if the database contains a TapPolicy with the specified name
        private boolean tapPolicyExistsByName(String name) {
                DBCollection table = database.getCollection(DatabaseNames.getTapPolicyTableName());

                // Run a query to look for the TapPolicy name
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("name", name);
            DBObject dbObj = table.findOne(searchQuery);

            return (dbObj != null) ? true : false;
        }

        public void modifyTapPolicy(final String tapPolicyId, TapPolicy tapPolicy) throws NotFoundException {
                DBCollection table = database.getCollection(DatabaseNames.getTapPolicyTableName());

                // Create a query to look for the existing TapPolicy by ID
                BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(tapPolicyId));

            // Check whether the policy exists
            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
                throw new NotFoundException();

                // Create a document containing the new tap policy
                BasicDBObject newDocument = tapPolicy.getAsDocument();

                BasicDBObject updateObj = new BasicDBObject();
                updateObj.put("$set", newDocument);

                // Update the document in the database
                table.update(searchQuery, updateObj);

                logger.info("Updated tap policy id " + tapPolicyId);
        }

        // Add a new match criteria to the system
        public void addMatchCriteria(MatchCriteria matchCriteria) throws DuplicateEntryException {

                // Check whether there is an existing match criteria with the same name
                if (matchCriteriaExistsByName(matchCriteria.getName()))
                        throw new DuplicateEntryException();

                // Get the match criteria table
                DBCollection table = database.getCollection(DatabaseNames.getMatchCriteriaTableName());

                // Create a document containing the new match criteria
                BasicDBObject document = matchCriteria.getAsDocument();

                // Add the new tap policy document to the MatchCriteria table
                table.insert(document);

                // Get the object ID from mongo and update it in the MatchCriteria object
                ObjectId objectId = document.getObjectId("_id");
                matchCriteria.setObjectId(objectId.toString());

                logger.info("Added match criteria " + matchCriteria.getName() + " object ID " + matchCriteria.getObjectId());

                // tappingAppLogic.NotifyConfigChange(DBTableChangeEnum.MATCH_CRITERIA, matchCriteria.getObjectId());
        }

        private boolean matchCriteriaExistsByName(String name) {
                DBCollection table = database.getCollection(DatabaseNames.getMatchCriteriaTableName());

                // Run a query to look for the Match Criteria name
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("name", name);
            DBObject dbObj = table.findOne(searchQuery);

            return (dbObj != null) ? true : false;
        }

        public void deleteMatchCriteria(final String objectId) throws NotFoundException, ObjectInUseException {
                DBCollection table = database.getCollection(DatabaseNames.getMatchCriteriaTableName());

                // Create a query to look for the existing TapPolicy by ID
                BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(objectId));
            DBObject dbObj = table.findOne(searchQuery);

            if (dbObj == null)
                throw new NotFoundException();

        if ((long) dbObj.get("refCount") != 0) {
                throw new ObjectInUseException();
        }

                // Remove the Match Criteria from the database
                table.remove(searchQuery);

                logger.info("Removed match criteria " + (String) dbObj.get("name"));
        }

        public void modifyMatchCriteria(final String matchCriteriaId, MatchCriteria matchCriteria) throws NotFoundException {

            DBCollection table = database.getCollection(DatabaseNames.getMatchCriteriaTableName());

            logger.info("Match Criteria enabled " + matchCriteria.getEnabled());

            // Create a query to look for the existing TapPolicy by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(matchCriteriaId));

            // Check whether the policy exists
            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
                throw new NotFoundException();

            // Create a document containing the new tap policy
            BasicDBObject newDocument = matchCriteria.getAsDocument();

            BasicDBObject updateObj = new BasicDBObject();
            updateObj.put("$set", newDocument);

            // Update the document in the database
            table.update(searchQuery, updateObj);

            logger.info("Updated match criteria id " + matchCriteriaId);
        }


        public void addSwitchEntry(SwitchEntry switchEntry) throws DuplicateEntryException, NotSupportedException {

                // We aren't supporting this mode yet
                if (switchEntry.getTappingMode() == TappingModeEnum.NORMAL) {
                    throw new NotSupportedException("Normal Mode is not suppored");
                }

                // Check whether there is an existing switch entry with the same name
                if (switchEntryExistsByName(switchEntry.getName()))
                        throw new DuplicateEntryException();

                // Get the match criteria table
                DBCollection table = database.getCollection(DatabaseNames.getSwitchEntryTableName());

                // Create a document containing the new match criteria
                BasicDBObject document = switchEntry.getAsDocument();

                // Add the new tap policy document to the tap policy table
                table.insert(document);

                // Get the object ID from mongo and update it in the switch entry object
                ObjectId objectId = document.getObjectId("_id");
                switchEntry.setObjectId(objectId.toString());

                logger.info("Added Switch Entry " + switchEntry.getName() + " object ID " + switchEntry.getObjectId());
        }

        public void modifySwitchEntry(final String switchEntryId, SwitchEntry switchEntry) throws NotFoundException {

            DBCollection table = database.getCollection(DatabaseNames.getSwitchEntryTableName());

            // Create a query to look for the existing SwitchEntry by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(switchEntryId));

            // Check whether the switchEntry exists
            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
                throw new NotFoundException();

            // Create a document containing the new switch Entry
            BasicDBObject newDocument = switchEntry.getAsDocument();

            BasicDBObject updateObj = new BasicDBObject();
            updateObj.put("$set", newDocument);

            // Update the document in the database
            table.update(searchQuery, updateObj);

            logger.info("Updated SwitchEntry id " + switchEntryId);
        }


        // Delete the SwitchEntry with the specified objectId
        public void deleteSwitchEntry(String objectId) throws NotFoundException, ObjectInUseException {
            DBCollection table = database.getCollection(DatabaseNames.getSwitchEntryTableName());

            // Create a query to look for the existing SwitchEntry by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(objectId));
            DBObject dbObj = table.findOne(searchQuery);

            if (dbObj == null)
                throw new NotFoundException();

            if ((long) dbObj.get("refCount") != 0) {
                throw new ObjectInUseException();
            }

            // Get the existing portChain
            String switchName = (String) dbObj.get("name");

            // Remove the PortChain from the database
            table.remove(searchQuery);

            logger.info("Removed switch  " + switchName);

            // Notify the application that the port chaun changed
            // TODO - add objectID
            // tappingAppLogic.NotifyConfigChange(DBTableChangeEnum.SWITCH_ENTRY);
    }


        private boolean switchEntryExistsByName(String switchName) {
                DBCollection table = database.getCollection(DatabaseNames.getSwitchEntryTableName());

                // Run a query to look for the SwitchEntry name
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("name", switchName);
            DBObject dbObj = table.findOne(searchQuery);

            return (dbObj != null) ? true : false;
        }

        public SwitchEntry findSwitchByDataPathId(final String switchId) throws NotFoundException {
            DBCollection table = database.getCollection(DatabaseNames.getSwitchEntryTableName());

            // Create a query to look for the existing SwitchEntry by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("OPFSwitchDatapath", switchId);
            DBObject dbObj = table.findOne(searchQuery);

            if (dbObj == null)
                throw new NotFoundException();

            SwitchEntry switchEntry = null;
            switchEntry = new SwitchEntry(dbObj, this);

            return switchEntry;
        }

        public void addNextHopSwitch(NextHopSwitch nextHopSwitch) throws DuplicateEntryException {

                // Check whether there is an existing next hop switch with the same name
                if (nextHopSwitchExistsByName(nextHopSwitch.getName()))
                        throw new DuplicateEntryException();

                // Get the match criteria table
                DBCollection table = database.getCollection(DatabaseNames.getNextHopSwitchTableName());

                // Create a document containing the new match criteria
                BasicDBObject document = nextHopSwitch.getAsDocument();

                // Add the new tap policy document to the tap policy table
                table.insert(document);

                // Get the object ID from mongo and update it in the switch entry object
                ObjectId objectId = document.getObjectId("_id");
                nextHopSwitch.setObjectId(objectId.toString());

                logger.info("Added Next Hop Switch " + nextHopSwitch.getName() + " object ID " + nextHopSwitch.getObjectId());
        }

        public void modifyNextHopSwitch(final String nhsId, NextHopSwitch nextHopSwitch) throws NotFoundException {

                DBCollection table = database.getCollection(DatabaseNames.getNextHopSwitchTableName());

                // Create a query to look for the existing NextHopSwitch by ID
                BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(nhsId));

            // Check whether the nextHopSwitch exists
            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
                        throw new NotFoundException();

                // Create a document containing the new nextHopSwitch
                BasicDBObject newDocument = nextHopSwitch.getAsDocument();

                BasicDBObject updateObj = new BasicDBObject();
                updateObj.put("$set", newDocument);

                // Update the document in the database
                table.update(searchQuery, updateObj);

                logger.info("Updated NextHopSwitch id " + nhsId);
        }

        // Delete the NextHopSwitch with the specified objectId
        public void deleteNextHopSwitch(String objectId) throws NotFoundException {
            DBCollection table = database.getCollection(DatabaseNames.getNextHopSwitchTableName());

            // Create a query to look for the existing NextHopSwitch by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(objectId));

            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
                throw new NotFoundException();

                // Get the existing nextHopSwitch name
                String nhsName = (String) dbObj.get("name");

                // Remove the nextHopSwitch from the database
                table.remove(searchQuery);

                logger.info("Removed Next Hop Switch  " + nhsName);

                // Notify the application that the capture device changed
                // TODO - add objectID
                // tappingAppLogic.NotifyConfigChange(DBTableChangeEnum.NEXT_HOP_SWITCH;
        }

        private boolean nextHopSwitchExistsByName(String switchName) {
            DBCollection table = database.getCollection(DatabaseNames.getSwitchEntryTableName());

            // Run a query to look for the SwitchEntry name
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("name", switchName);
            DBObject dbObj = table.findOne(searchQuery);

            return (dbObj != null) ? true : false;
        }

        public void addCaptureDevice(CaptureDev captureDev) throws DuplicateEntryException {

            // Check whether there is an existing capture device with the same name
            if (captureDeviceExistsByName(captureDev.getName()))
                    throw new DuplicateEntryException();

            // Get the match criteria table
            DBCollection table = database.getCollection(DatabaseNames.getCaptureDevTableName());

            // Create a document containing the new match criteria
            BasicDBObject document = captureDev.getAsDocument();

            // Add the new tap policy document to the tap policy table
            table.insert(document);

            // Get the object ID from mongo and update it in the switch entry object
            ObjectId objectId = document.getObjectId("_id");
            captureDev.setObjectId(objectId.toString());

            logger.info("Added Next Hop Switch " + captureDev.getName() + " object ID " + captureDev.getObjectId());
        }

        private boolean captureDeviceExistsByName(String name) {
            DBCollection table = database.getCollection(DatabaseNames.getCaptureDevTableName());

            // Run a query to look for the SwitchEntry name
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("name", name);
            DBObject dbObj = table.findOne(searchQuery);

            return (dbObj != null) ? true : false;
        }

        public void modifyCaptureDev(final String captureDevId, CaptureDev captureDev) throws NotFoundException {

            DBCollection table = database.getCollection(DatabaseNames.getCaptureDevTableName());

            // Create a query to look for the existing CaptureDev by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(captureDevId));

            // Check whether the CaptureDev exists
            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
                    throw new NotFoundException();

                    // Create a document containing the new switch Entry
                    BasicDBObject newDocument = captureDev.getAsDocument();

                    BasicDBObject updateObj = new BasicDBObject();
                    updateObj.put("$set", newDocument);

                    // Update the document in the database
                    table.update(searchQuery, updateObj);

                    logger.info("Updated CaptureDev id " + captureDevId);
            }

        // Delete the CaptureDev with the specified objectId
        public void deleteCaptureDev(String objectId) throws NotFoundException, ObjectInUseException {
            DBCollection table = database.getCollection(DatabaseNames.getCaptureDevTableName());

            // Create a query to look for the existing CaptureDev by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(objectId));
            DBObject dbObj = table.findOne(searchQuery);

            if (dbObj == null)
                throw new NotFoundException();

            if ((long) dbObj.get("refCount") != 0) {
                throw new ObjectInUseException();
        }

        // Get the existing captureDev
        String captureDevName = (String) dbObj.get("name");

            // Remove the captureDev from the database
            table.remove(searchQuery);

            logger.info("Removed Capture Device  " + captureDevName);

            // Notify the application that the capture device changed
            // TODO - add objectID
            // tappingAppLogic.NotifyConfigChange(DBTableChangeEnum.CAPTURE_DEVICE);
        }


        public void addPortChain(PortChain portChain) throws DuplicateEntryException {

                // Check whether there is an existing port chain with the same name
                if (portChainExistsByName(portChain.getName()))
                        throw new DuplicateEntryException();

                // Get the match criteria table
                DBCollection table = database.getCollection(DatabaseNames.getPortChainTableName());

                // Create a document containing the new match criteria
                BasicDBObject document = portChain.getAsDocument();

                // Add the new tap policy document to the tap policy table
                table.insert(document);

                // Get the object ID from mongo and update it in the switch entry object
                ObjectId objectId = document.getObjectId("_id");
                portChain.setObjectId(objectId.toString());

                logger.info("Added Port Chain " + portChain.getName() + " object ID " + portChain.getObjectId());
        }

        private boolean portChainExistsByName(String name) {
                DBCollection table = database.getCollection(DatabaseNames.getPortChainTableName());

                // Run a query to look for the PortChain name
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("name", name);
            DBObject dbObj = table.findOne(searchQuery);

            return (dbObj != null) ? true : false;
        }

        public void modifyPortChain(final String portChainId, PortChain portChain) throws NotFoundException {

            DBCollection table = database.getCollection(DatabaseNames.getPortChainTableName());

            // Create a query to look for the existing PortChain by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(portChainId));

            // Check whether the PortChain exists
            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
                        throw new NotFoundException();

            // Create a document containing the new portChain
            BasicDBObject newDocument = portChain.getAsDocument();

            BasicDBObject updateObj = new BasicDBObject();
            updateObj.put("$set", newDocument);

            // Update the document in the database
            table.update(searchQuery, updateObj);

            logger.info("Updated PortChain ID " + portChainId);
        }


        // Delete the PortChain with the specified objectId
        public void deletePortChain(String objectId) throws NotFoundException, ObjectInUseException {
            DBCollection table = database.getCollection(DatabaseNames.getPortChainTableName());

            // Create a query to look for the existing TapPolicy by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(objectId));
            DBObject dbObj = table.findOne(searchQuery);

            if (dbObj == null)
                throw new NotFoundException();

            if ((long) dbObj.get("refCount") != 0) {
                throw new ObjectInUseException();
        }

        // Get the existing PortChain
        String portChainName = (String) dbObj.get("name");

            // Remove the PortChain from the database
            table.remove(searchQuery);

            logger.info("Removed Port Chain " + portChainName);

            // Notify the application that the port chaun changed
            // TODO - add objectID
            // tappingAppLogic.NotifyConfigChange(DBTableChangeEnum.PORT_CHAIN);
        }


        public List<LoggedMessage> loadLoggedMessagesFromDatabase() {

            DBCollection logTable = database.getCollection(DatabaseNames.getLoggerTableName());

            DBCursor cursor = logTable.find();
            List<LoggedMessage> loggedMessageList = processLogMessageCursor(cursor);
            cursor.close();

            return loggedMessageList;
        }


        public List<LoggedMessage> loadLoggedMessagesFromDatabase(final Date startDate, final Date endDate) {
                DBCollection logTable = database.getCollection(DatabaseNames.getLoggerTableName());

                // Create a query to look for log entries after the start date
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("timestamp", BasicDBObjectBuilder.start("$gte", startDate).add("$lte", endDate).get());

                DBCursor cursor = logTable.find(searchQuery);
                List<LoggedMessage> loggedMessageList = processLogMessageCursor(cursor);
            cursor.close();

                return loggedMessageList;
        }

        public List<LoggedMessage> loadLoggedMessagesFromDatabase(final Date startDate) {
                DBCollection logTable = database.getCollection(DatabaseNames.getLoggerTableName());

                // Create a query to look for log entries after the start date
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("timestamp", new BasicDBObject("$gte", startDate));

                DBCursor cursor = logTable.find(searchQuery);
                List<LoggedMessage> loggedMessageList = processLogMessageCursor(cursor);
            cursor.close();

                return loggedMessageList;
        }

        private List<LoggedMessage> processLogMessageCursor(DBCursor cursor) {
                List<LoggedMessage> loggedMessageList = new ArrayList<LoggedMessage>(cursor.size());

           while(cursor.hasNext()) {
                   DBObject logObj = cursor.next();
                   LoggedMessage logMsg = new LoggedMessage();

                   ObjectId oid = (ObjectId) logObj.get("_id");
                   logMsg.setObjectId(oid.toString());
                   logMsg.setTimestamp((Date) logObj.get("timestamp"));
                   logMsg.setLogLevel(LoggingLevelEnum.getEnum((String) logObj.get("level")));
                   logMsg.setThreadId((String) logObj.get("thread"));
                   logMsg.setMessage((String) logObj.get("message"));
                   logMsg.setFileName((String) logObj.get("fileName"));
                   logMsg.setMethodName((String) logObj.get("method"));
                   logMsg.setLineNumber(Integer.parseInt((String) logObj.get("lineNumber")));

                   // get these fields from the "host" sub-document
                   DBObject hostObj = (DBObject) logObj.get("host");
                   logMsg.setHostname((String) hostObj.get("name"));
                   logMsg.setHostIP((String) hostObj.get("ip"));

                   // Add the message to the list
                   loggedMessageList.add(logMsg);
           }
           return loggedMessageList;
        }

        public List<PortStatus> getPortStatus(String switchId) {
                // Get the switch info from ODL
                SwitchInfo switchInfo = getSwitchInfo(switchId);

                List<PortStatus> portStatusList = new ArrayList<PortStatus>(switchInfo.numPorts);

                for (int port = 0; port < switchInfo.numPorts; port++) {
                        LinkStateEnum portState = switchInfo.portStateList.get(port);
                        boolean live = ((port % 3 == 0) ? true : false);
                        boolean blocked = ((port % 10 == 0) ? true : false);
                        PortTypeEnum type = PortTypeEnum.NETWORK_DEVICE;
                        if (port == 22) type = PortTypeEnum.CAPTURE_DEVICE;
                        if ((port == 10) || (port == 11)) type = PortTypeEnum.PORT_CHAIN;
                        portStatusList.add(new PortStatus(port, type, portState, blocked, live));
                }

                return portStatusList;
        }

        public List<PortStatistics> getPortStatistics(String switchId) {
                // Get the switch statistics from ODL
                SwitchInfo switchInfo = getSwitchInfo(switchId);

                List<PortStatistics> portStatisticsList = new ArrayList<PortStatistics>(switchInfo.numPorts);

                for (int port = 0; port < switchInfo.numPorts; port++) {
                        PortStatistics portStatistics = switchInfo.portStatisticsList.get(port);
                        portStatisticsList.add(portStatistics);
                }

                return portStatisticsList;
        }

        private SwitchInfo getSwitchInfo(String switchId) {

                // Note switch ID is not used yet. Eventually plumb this into ODL
                SwitchInfo switchInfo = new SwitchInfo();
                return switchInfo;
        }

        public TapPolicy getTapPolicyById(String tapPolicyId) throws NotFoundException {

                DBCollection table = database.getCollection(DatabaseNames.getTapPolicyTableName());

                // Create a query to look for the existing TapPolicy by ID
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("_id", new ObjectId(tapPolicyId));

                // Check whether the policy exists
                DBObject dbObj = table.findOne(searchQuery);
                if (dbObj == null)
                   throw new NotFoundException();

                // Construct a new tap policy from the DBobject and add it to list
                TapPolicy tapPolicy = new TapPolicy(dbObj);

            return tapPolicy;
        }

        public MatchCriteria getMatchCriteriaById(String matchCriteriaId) throws NotFoundException {
                DBCollection table = database.getCollection(DatabaseNames.getMatchCriteriaTableName());

                // Create a query to look for the existing MatchCriteria by ID
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("_id", new ObjectId(matchCriteriaId));

                // Check whether the policy exists
                DBObject dbObj = table.findOne(searchQuery);
                if (dbObj == null)
                   throw new NotFoundException();

                // Construct a new match criteria from the DBobject and add it to list
                MatchCriteria matchCriteria = new MatchCriteria(dbObj);

            return matchCriteria;
        }

        public SwitchEntry getSwitchEntryById(String switchId) throws NotFoundException {
            DBCollection table = database.getCollection(DatabaseNames.getSwitchEntryTableName());

            // Create a query to look for the existing SwitchEntry by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(switchId));

            // Check whether the policy exists
            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
               throw new NotFoundException();

            // Construct a new switch entry from the DBobject and return it
            SwitchEntry switchEntry = new SwitchEntry(dbObj, this);
            logger.info("SwitchEntry " + switchEntry);

            return switchEntry;
        }

        public PortChain getPortChainById(String portChainId) throws NotFoundException {
            DBCollection table = database.getCollection(DatabaseNames.getPortChainTableName());

            // Create a query to look for the existing PortChain by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(portChainId));

            // Check whether the port chain exists
            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
               throw new NotFoundException();

            // Construct a new port chain from the DBobject and return it
            PortChain portChain = new PortChain(dbObj);
            return portChain;
        }

        public static CaptureDev getCaptureDevById(String captureDevId) throws NotFoundException {
            DBCollection table = database.getCollection(DatabaseNames.getCaptureDevTableName());

            // Create a query to look for the existing CaptureDev by ID
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(captureDevId));

            // Check whether the captureDev exists
            DBObject dbObj = table.findOne(searchQuery);
            if (dbObj == null)
               throw new NotFoundException();

            // Construct a caprture dev from the DBobject and return it
            CaptureDev captureDev = new CaptureDev(dbObj);
            return captureDev;
        }

        public OPFSwitch getUnAssignedOPFSwitchById(String oPFSwitchDataPath) {
                OPFSwitch opfSwitch = unassignedOPFSwitchMap.get(oPFSwitchDataPath);
                return opfSwitch;
        }

        public OPFSwitch getAssignedOPFSwitchById(String oPFSwitchDataPath) {
        OPFSwitch opfSwitch = assignedOPFSwitchMap.get(oPFSwitchDataPath);
        return opfSwitch;
        }

        private void runGetAllObjectsUnitTest() {
            List<TapPolicy>  tapPolicyList = this.getAllTapPolicies();
            for(TapPolicy tp : tapPolicyList) {
                    logger.info(tp.toString());
                    System.out.println(tp.toString());
            }

            List<MatchCriteria> matchCriteriaList = this.getAllMatchCriteria();
            for(MatchCriteria mc : matchCriteriaList) {
                    logger.info(mc.toString());
                    System.out.println(mc.toString());
             }

            List<SwitchEntry> switchEntryList = this.getAllSwitchEntries();
            for(SwitchEntry se : switchEntryList) {
                    logger.info(se.toString());
                    System.out.println(se.toString());
             }

            List<NextHopSwitch> nextHopSwitchList = this.getAllNextHopSwitches();
            for(NextHopSwitch nhs : nextHopSwitchList) {
                    logger.info(nhs.toString());
                    System.out.println(nhs.toString());
             }

            List<CaptureDev> captureDevList = this.getAllCaptureDevices();
            for(CaptureDev cd : captureDevList) {
                    logger.info(cd.toString());
                    System.out.println(cd.toString());
             }

            List<PortChain> portChains = this.getAllPortChains();
            for (PortChain pc : portChains) {
                    logger.info(pc.toString());
            }
        }

        private void runLogMessageUnitTest() {
            List<LoggedMessage> logMessages = this.loadLoggedMessagesFromDatabase();

            for(LoggedMessage lm : logMessages) {
                logger.info("Log Message " + lm);
                System.out.println("Log Messsage " + lm);
             }
       }

        // Called from ODL when a switch is added/modified/removed
        public void switchChanged(UpdateType type, Node node) {
            logger.info("TappingApp::switchChanged");

            this.tappingAppLogic.switchChanged(type, node);
        }

        public List<TapPolicy> getTapPoliciesForSwitch(SwitchEntry switchEntry) {

             DBCollection table = database.getCollection(DatabaseNames.getTapPolicyTableName());

             List<TapPolicy> tapPoliciesForSwitch = new ArrayList<TapPolicy>();

             logger.info("Searching for Tap Policies for switch with ID " + switchEntry.getObjectId());
             BasicDBObject searchQuery = new BasicDBObject();
             ObjectId switchId = new ObjectId(switchEntry.getObjectId());
             searchQuery.put("switchAndPort.switchEntryId", switchId);
             DBCursor cursor = table.find(searchQuery);

             logger.info("Query result set contains "+ cursor.count() + "Tap Policies");

             try {
                while(cursor.hasNext()) {
                    TapPolicy tapPolicy;
                    try {
                        tapPolicy = new TapPolicy(cursor.next());
                        tapPoliciesForSwitch.add(tapPolicy);
                        logger.info("Tap Policy  " + tapPolicy);
                    } catch (NotFoundException e) {
                        logger.warn("Cannot find tap policy");
                    }
                }
          } finally {
               cursor.close();
          }

          return tapPoliciesForSwitch;
        }

        // A new switch connected to ODL. We add it to the list of unassigned switches
        public OPFSwitch createOPFSwitch(Node node) {

            logger.info("TappingAppLogic::createOPFSwitch");

            final String switchId = node.getNodeIDString();

            // Get the OPFSwith populated with values from the Node/NodeDescriptor
            OPFSwitch opfSwitch = null;
            try {
                opfSwitch = this.getOPFSwitchFromNode(node);
            } catch (InternalError e) {
                logger.warn("Unable to get information about OPFSwitch from the Node");
                return opfSwitch;
            }

            if (opfSwitch != null) {
                // TODO -get this somehow
                opfSwitch.setIPAddress("127.0.0.1");
                opfSwitch.setDataPathDesc(switchId);

                // Simulate a 24 port switch
                List<PortDescription> fakePortDescList = new ArrayList<PortDescription>();
                for (int port = 0; port < 24; port++) {
                    fakePortDescList.add(new PortDescription("Port " + port, "11:22:33:44:aa:bb", port));
                }
                opfSwitch.setPortDescList(fakePortDescList);

                logger.info("Adding ONF Switch entry to unassignedOPFSwitchMap " + opfSwitch);
                logger.info("Map Key is " + opfSwitch.getDataPathDesc());

                // Add the OPFSwitch to the map
                this.unassignedOPFSwitchMap.put(opfSwitch.getDataPathDesc(), opfSwitch);

                List<OPFSwitch> foo = this.getAllUnassignedSwitchEntries();
                logger.info("Added switch " + switchId + " to the unrecognied switch list");
                logger.info("Unassigned Map  " + unassignedOPFSwitchMap);
                logger.info("Unassigned list " +  foo);
             }
             return opfSwitch;
       }

        public void removeOPFSwitch(Node node) {

            final String switchId = node.getNodeIDString();

            logger.info("Removing OPF switch ID " + switchId);

            // Look for the switch in the unassigned Map
            OPFSwitch opfSwitch = this.unassignedOPFSwitchMap.get(switchId);
            if (opfSwitch != null) {
                this.unassignedOPFSwitchMap.remove(switchId);
                logger.info("Removed unassigned OPF Switch " + switchId);
            }
            else {
                 // Look for the switch in the assigned Map
                opfSwitch = this.assignedOPFSwitchMap.get(switchId);
                if (opfSwitch != null) {
                    this.assignedOPFSwitchMap.remove(switchId);
                }
                else {
                    logger.warn("Attempt to remove nonexistent OPF Switch " + switchId);
                }
            }
        }

//        // Temporary code just until we can hook up the GUI
//        public void bindSwitchEntryToOPF(String opfSwitchId) throws InternalError {
//
//            SwitchEntry switchEntry = null;
//            try {
//                 switchEntry = findSwitchByDataPathId(opfSwitchId);
//            } catch (NotFoundException e) {
//                 logger.info("Can't find switch with ID " + opfSwitchId);
//            }
//
//            if (switchEntry == null ) {
//                logger.info("Creating new SwitchEntry");
//
//            SwitchEntry newSwitchEntry = new SwitchEntry();
//            newSwitchEntry.setName("OVS Switch 1: br_int");
//            newSwitchEntry.setTappingMode(TappingModeEnum.TAPAGGR);
//
//            // Add the new switch entry to the system
//            try {
//                this.addSwitchEntry(newSwitchEntry);
//            } catch (DuplicateEntryException e1) {
//                logger.error("Internal Error: Duplicate SwitchEntry", e1);
//                throw new InternalError("Internal Error: Duplicate SwitchEntry but can't find it");
//            }
//
//            logger.info("SwitchEntry Added " + newSwitchEntry);
//
//            // Assign the OPF Switch to the new SwitchEntry
//            try {
//                this.assignSwitch(opfSwitchId, newSwitchEntry.getObjectId());
//            } catch (NotFoundException e) {
//                    logger.error("SwitchEntry Not Found", e);
//            } catch (OPFSwitchNotFoundException e) {
//                    logger.error("OPF Switch Not Found, e");
//            } catch (OPFSwitchIsAssignedException e) {
//                    logger.error("OPFSwitch is already assigned", e);
//            }
//
//            // this.tappingAppLogic.createDemoObjects(newSwitchEntry);
//        }
//    }

    // Return an OPFSwitch object populated with values from the NodeDescriptor
    public OPFSwitch getOPFSwitchFromNode(Node node) throws InternalError {
        if (this.onfTappingAppImpl.getStatisticsManager() == null) {
            logger.warn("ONFTappingAppImpl.StatisticsManager is null");
            throw new InternalError();
        }

        NodeDescription nodeDesc = this.onfTappingAppImpl.getStatisticsManager().getNodeDescription(node);
        if (nodeDesc == null){
            logger.warn("Node Description is null");
            throw new InternalError();
        }
        else
            logger.info("NodeDesc " + nodeDesc);

        return new OPFSwitch(nodeDesc, node.getNodeIDString());
    }

    // Check the assigned and unassigned switch maps
    public OPFSwitch getOPFSwitchById(String opfSwitchId) {

        // See if we have an existing entry for this OPF Switch
        OPFSwitch opfSwitch = this.getAssignedOPFSwitchById(opfSwitchId);
        if (opfSwitch == null)
            opfSwitch = this.getUnAssignedOPFSwitchById(opfSwitchId);

        return opfSwitch;

    }

    // Add the FlowRules to the ruleMap (in-memory database).
    // Rules are installed into the switch by InstallSwitchRules
    public void installFlowRules(List<FlowEntry> flowRules, Node node) {

        // Add the rules to the rule map (key is the node)
        this.rulesDB.put(node, flowRules);

        Set<Node> switchesToProgram = new  HashSet<Node>();

        // Add the switch to the set of switches to program
        switchesToProgram.add(node);

        // And now send them to the switch
        this.pushFlowRulesToSwitch(switchesToProgram);
    }

     // Routine that fetches the switch rules from the rulesDB and installs them into
     // the switch hardware. The one having the same match rules will be
     // overwritten silently.
     //
     // @return a return code to indicate the result of programming the switch

    private RulesProgrammingReturnCode pushFlowRulesToSwitch(Set<Node> switchesToProgram) {

        logger.info("pushFlowRulesToSwitch");

        if (switchesToProgram == null) {
            logger.info("No switches to program");
            logger.info("Leaving installSwitchRules. Return code is: " +   RulesProgrammingReturnCode.FAILED_WRONG_PARAMS);

            return RulesProgrammingReturnCode.FAILED_WRONG_PARAMS;
        }

        // Get the Forwarding Rules Manager from ODL (via the Impl)
        IForwardingRulesManager forwardinRulesMgr = this.onfTappingAppImpl.getForwardingRulesManager();

        if (forwardinRulesMgr == null) {
            logger.warn("Cannot install flow ... Fowarding Rules Mananger is null");
            return RulesProgrammingReturnCode.FAILED_ALL_SWITCHES;
        }

        RulesProgrammingReturnCode returnCode = RulesProgrammingReturnCode.SUCCESS;

        // Now program every switch
        logger.info("Inside pushFlowRulesToSwitch. There are " + switchesToProgram.size() + " switches to program");

        // Program each switch ...
        for (Node swId : switchesToProgram) {

            List<FlowEntry> flowEntryList = this.rulesDB.get(swId);
            if (flowEntryList == null) {
                logger.info("Flow entry list is null");
                continue;
            }

            logger.info("Inside installSwitchRules. There are " + flowEntryList.size() +  " flows");

            for (FlowEntry flowEntry : flowEntryList) {
                if (flowEntry != null) {
                    logger.info("Installing flowEntry " + flowEntry.toString());

                    // Populate the Policy field now
                    logger.info("Installing flow " + flowEntry  + " using the ForwardingManager");
                    Status poStatus = forwardinRulesMgr.modifyOrAddFlowEntry(flowEntry);
                    logger.info("FowardingManager returned " + poStatus);

                    if (!poStatus.isSuccess()) {
                        logger.error("Failed to install policy: "
                            + flowEntry.getGroupName() + " ("
                            + poStatus.getDescription() + ")");

                        returnCode = RulesProgrammingReturnCode.FAILED_FEW_SWITCHES;
                        // Remove the entry from the DB, it was not installed!
                        this.rulesDB.remove(swId);
                    } else {
                        logger.debug("Successfully installed policy "
                                + flowEntry.toString() + " on switch " + swId);
                    }
                } else {
                    logger.error("Cannot find a policy for SW:({}))", swId);
                }
            }
        }

        logger.info("Leaving installSwitchRules. Return code is: " + returnCode);

        return returnCode;
    }

    // Get the set of NodeConnectors for a node (requires acces to the SwitchManager
    public Set<NodeConnector> getODLNodeConnectors(Node node) {

        // Get the set of nodeConnectors
        Set<NodeConnector> ports = this.onfTappingAppImpl.getSwitchManager().getNodeConnectors(node);

        Iterator<NodeConnector> portIt = ports.iterator();

        // List<NodeConnector> portsArray = new ArrayList<NodeConnector>();

        while (portIt.hasNext()) {
            logger.info("NodeConnector " + portIt.next());
            //portsArray.add(portIt.next());
        }

        return ports;

        //return this.onfTappingAppImpl.getSwitchManager().getNodeConnectors(node);
 //             .getPhysicalNodeConnectors(node);
    }
}