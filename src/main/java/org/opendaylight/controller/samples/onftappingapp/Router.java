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

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.delete;
import static spark.Spark.externalStaticFileLocation;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.transaction.NotSupportedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;

public class Router {

    // Connect to the ODL slf4j logger
    protected static final Logger logger = LoggerFactory.getLogger(TappingApp.class);

    private TappingApp app = null;

    public Router(TappingApp tappingApp) {

        this.app = tappingApp;

        // Get the webserver root dir from the config file
        final String appDir = TappingApp.getWwwRootdir();

        //logger.info("User dir: " + System.getProperty("user.dir"));
        //logger.info("Classpath: " + System.getProperty("java.class.path"));
        logger.info("Web Server root directory: " + appDir);

        // final String appDir = "/home/andrewpearce/workspace/tapapp-GUI-1.2/src/main/resources/app";

        logger.info("Calling externalStaticFileLocation");
        // staticFileLocation(appDir);
        externalStaticFileLocation(appDir);

        File f = new File(appDir);
        if (!f.exists()) {
            logger.info("app directory not found!");
        } else if (!f.isDirectory()) {
              logger.info("app is not a directory");
        }
        else {
            logger.info("app directory exists");
        }

        // Tap Policies

        get(new TappingRoute("/tappolicies") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                return app.getAllTapPolicies();
            }
        });

        post(new TappingRoute("/tappolicies") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                TapPolicy policy = buildTapPolicy(params);
                app.addTapPolicy(policy);
                return policy;

            }
        });


        put(new TappingRoute("/tappolicies/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                TapPolicy policy = buildTapPolicy(params);
                app.modifyTapPolicy(request.params("id"), policy);
                return policy;

            }
        });

        delete(new TappingRoute("/tappolicies/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                app.deleteTapPolicy(request.params("id"));
                return "deleted";
            }
        });

        // Match Criteria

        get(new TappingRoute("/matchcriteria") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                return app.getAllMatchCriteria();
            }
        });

        post(new TappingRoute("/matchcriteria") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                MatchCriteria match = buildMatchCriteria(params);
                app.addMatchCriteria(match);
                return match;

            }


        });

        put(new TappingRoute("/matchcriteria/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                MatchCriteria matchCritera = buildMatchCriteria(params);
                app.modifyMatchCriteria(request.params("id"), matchCritera);
                return matchCritera;

            }
        });

        delete(new TappingRoute("/matchcriteria/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException, ObjectInUseException {

                app.deleteMatchCriteria(request.params("id"));
                return "deleted";
            }
        });

        // Switch Entries

        get(new TappingRoute("/switchentries") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                List<SwitchEntry> switchEntries = new ArrayList<SwitchEntry>();
                for (SwitchEntry switchEntry : app.getAllSwitchEntries()) {
                    switchEntries.add(app.getSwitchEntryById(switchEntry.getObjectId()));
                }
                return switchEntries;

            }
        });

        post(new TappingRoute("/switchentries") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotSupportedException {
                SwitchEntry switchEntry = buildSwitchEntry(params);
                app.addSwitchEntry(switchEntry);
                assignSwitch(app, params.get("opfSwitch"), switchEntry.getObjectId());

                return switchEntry;
            }
        });

        put(new TappingRoute("/switchentries/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {
                unassignSwitch(app, request.params("id"));
                SwitchEntry switchEntry = buildSwitchEntry(params);
                app.modifySwitchEntry(request.params("id"), switchEntry);
                assignSwitch(app, params.get("opfSwitch"), request.params("id"));
                return switchEntry;

            }
        });

        delete(new TappingRoute("/switchentries/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException, ObjectInUseException {
                unassignSwitch(app, request.params("id"));
                app.deleteSwitchEntry(request.params("id"));
                return "deleted";
            }
        });

        //Next Hop Switches
        get(new TappingRoute("/nexthopswitches") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {
                return app.getAllNextHopSwitches();
            }
        });

        post(new TappingRoute("/nexthopswitches") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                NextHopSwitch nextHopSwitch = buildNextHopSwitch(params);
                app.addNextHopSwitch(nextHopSwitch);
                return nextHopSwitch;
            }
        });

        put(new TappingRoute("/nexthopswitches/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                NextHopSwitch nextHopSwitch = buildNextHopSwitch(params);
                app.modifyNextHopSwitch(request.params("id"), nextHopSwitch);
                return nextHopSwitch;

            }
        });

        delete(new TappingRoute("/nexthopswitches/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException, ObjectInUseException {


                app.deleteNextHopSwitch(request.params("id"));
                return "deleted";
            }
        });

        //OPFSwitches
        get(new TappingRoute("/opfswitches") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {
                return app.getAllUnassignedSwitchEntries();
            }
        });

        // Capture devices

        get(new TappingRoute("/capturedevices") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                return app.getAllCaptureDevices();
            }
        });

        post(new TappingRoute("/capturedevices") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException, UnknownHostException {

                CaptureDev captureDevice = buildCaptureDevice(params);
                app.addCaptureDevice(captureDevice);
                return captureDevice;
            }


        });

        put(new TappingRoute("/capturedevices/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException, UnknownHostException {

                CaptureDev captureDevice = buildCaptureDevice(params);
                app.modifyCaptureDev(request.params("id"), captureDevice);
                return captureDevice;
            }
        });
        delete(new TappingRoute("/capturedevices/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException, ObjectInUseException {

                app.deleteCaptureDev(request.params("id"));
                return "deleted";
            }
        });

        // Port Chains

        get(new TappingRoute("/portchains") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {

                return app.getAllPortChains();
            }
        });

        post(new TappingRoute("/portchains") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException,
                    UnknownHostException {

                PortChain portChain = buildPortChain(params);
                app.addPortChain(portChain);
                return portChain;
            }

            });

        put(new TappingRoute("/portchains/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException, UnknownHostException {

                PortChain portChain = buildPortChain(params);
                app.modifyPortChain(request.params("id"), portChain);
                return portChain;
            }
        });

        delete(new TappingRoute("/portchains/:id") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException, ObjectInUseException {
                app.deletePortChain(request.params("id"));
                return "deleted";
            }
        });

        get(new TappingRoute("/logs") {

            @Override
            public Object execute(Map<String, Object> params, Request request,
                    Response response) throws TapAppException,
                    DuplicateEntryException, NotFoundException {
                Date startDate = new Date(System.currentTimeMillis() - (60 * 60 * 1000));
                return app.loadLoggedMessagesFromDatabase(startDate);
            }
        });
    }

    private static TapPolicy buildTapPolicy(Map<String, Object> params) throws NotFoundException {

        TapPolicy policy = new TapPolicy();

        if (params.containsKey("name"))
            policy.setName((String) params.get("name"));
        if (params.containsKey("description"))
            policy.setDescription((String) params.get("description"));
        if (params.containsKey("matchCriteriaIdList")) {

            List<String> criterias = (ArrayList<String>) params.get("matchCriteriaIdList");

            for (String matchCriteria : criterias) {

                System.out.println("adding match criteria " + matchCriteria);
                policy.addMatchCriteria(matchCriteria);
            }
        }

        if (params.containsKey("switchEntryIdList")) {

            List<String> devices = (ArrayList<String>) params.get("switchEntryIdList");

            for (String switchEntryID : devices) {
                System.out.println("adding switch entry " + switchEntryID);

                // TODO - Steven - change this to build a SwitchAndPort
                // policy.addSwitchEntry(switchEntryID);

                // Like this:
                //SwitchAndPort switchAndPort = getFromSomewhere;

                // TODO - temporary code until next code drop
                SwitchAndPort switchAndPort = new SwitchAndPort("SP 1", switchEntryID, 9);
                policy.addSwitchAndPort(switchAndPort);
            }
        }
        if (params.containsKey("captureDevIdList")) {

            List<String> devices = (ArrayList<String>) params.get("captureDevIdList");

            for (String captureDevId : devices) {
                System.out.println("adding capture device " + captureDevId);
                policy.addCaptureDev(captureDevId);
            }
        }
        if (params.containsKey("portChainIdList")) {

            List<String> devices = (ArrayList<String>) params.get("portChainIdList");

            for (String portChainId : devices) {
                System.out.println("adding port chain " + portChainId);
                policy.addPortChain(portChainId);
            }
        }

        if (params.containsKey("enabled") && (params.get("enabled") != null) && ((Boolean) params.get("enabled")))
            policy.setEnabled(true);
        return policy;

    }

    private static MatchCriteria buildMatchCriteria(Map<String, Object> params) {
        MatchCriteria match = new MatchCriteria();

        if (params.containsKey("name"))
            match.setName((String) params.get("name"));

        if (params.containsKey("enabled") && params.get("enabled") != null && ((Boolean) params.get("enabled")))
            match.setEnabled(true);

        if (params.containsKey("reflexive") && params.get("reflexive") != null && ((Boolean) params.get("reflexive")))
            match.setReflexive(true);

        if (params.containsKey("priority"))
            try {
                match.setPriority(Integer.valueOf((String) params.get("priority")));
            } catch (NumberFormatException e) {}


        if (params.containsKey("matchFieldList")) {

            List<MatchField> matchFields = new ArrayList<MatchField>();

            List<Map<String, String>> fields = (ArrayList<Map<String, String>>) params.get("matchFieldList");
            for (Map<String, String> field : fields) {
                if (field.get("type") != null) {
                    System.out.println("adding " + field);
                    MatchField matchField = new MatchField(field.get("type"), field.get("value"));
                    matchFields.add(matchField);
                }
            }
            match.setFields(matchFields);
        }

        return match;
    }


    private static RewriteParams buildRewriteParams(Map<String, String> params) {
        RewriteParams rewriteParams = new RewriteParams();
        if (params.containsKey("name"))
            rewriteParams.setName(params.get("name"));
        if (params.containsKey("ipAddress"))
            try {
                rewriteParams.setIpAddr(Long.valueOf(params.get("ipAddress")));
            } catch (NumberFormatException e) {}
        if (params.containsKey("macAddr"))
            rewriteParams.setMacAddr(params.get("macAddr"));
        if (params.containsKey("vlanId"))
            try {
                rewriteParams.setVlanId(Integer.valueOf(params.get("vlanId")));
            } catch (NumberFormatException e) {}
        return rewriteParams;
    }

    private static SwitchAndPort buildSwitchAndPort(Map<String, String> params) {
        SwitchAndPort result = new SwitchAndPort();
        if (params.containsKey("name"))
            result.setName(params.get("name"));
        if (params.containsKey("switchId"))
            result.setSwitchId(params.get("switchId"));
        if (params.containsKey("switchPort"))
            try {
                result.setSwitchPort(Integer.valueOf(params.get("switchPort")));
            } catch (NumberFormatException e) {}
        return result;
    }

    private static NextHopSwitch buildNextHopSwitch(Map<String, Object> params) {
        NextHopSwitch result = new NextHopSwitch();
        if (params.containsKey("switchName"))
            result.setName((String) params.get("switchName"));
        //if (params.containsKey("nextSwitchId")
        if (params.containsKey("VLANTag"))
            try {
                result.setVLANTag(Integer.valueOf((String) params.get("VLANTag")));
            } catch (NumberFormatException e) {}
        return result;
    }

    private static SwitchEntry buildSwitchEntry(Map<String, Object> params) {
        SwitchEntry switchEntry = new SwitchEntry();
        if (params.containsKey("name"))
            switchEntry.setName((String) params.get("name"));
        if (params.containsKey("tappingMode") && params.get("tappingMode") != null)
            switchEntry.setTappingMode(TappingModeEnum.valueOf((String) params.get("tappingMode")));
        if (params.containsKey("nextHopSwitch")) {
            switchEntry.setNextHopSwitch((String) params.get("nextHopSwitch"));
        }
        return switchEntry;
    }

    private static CaptureDev buildCaptureDevice(Map<String, Object> params) throws UnknownHostException {
        CaptureDev captureDevice = new CaptureDev();
        if (params.containsKey("name"))
            captureDevice.setName((String) params.get("name"));
        if (params.containsKey("captureType") && params.get("captureType") != null)
            captureDevice.setCaptureType(CaptureTypeEnum.valueOf((String) params.get("captureType")));
        if (params.containsKey("switchId"))
            captureDevice.setSwitchId((String) params.get("switchId"));
        if (params.containsKey("switchPort"))
            try {
                captureDevice.setSwitchPort( Integer.valueOf(((String) params.get("switchPort"))));
            } catch (NumberFormatException e) {}

        if (params.containsKey("ipAddr"))
            captureDevice.setIPAddress((String) params.get("ipAddr"));

        if (params.containsKey("vlanId"))
            try {
                captureDevice.setVlanId(Integer.valueOf((String) params.get("vlanId")));
            } catch (NumberFormatException e) {}

        if (params.containsKey("macAddr"))
            captureDevice.setMacAddr((String) params.get("macAddr"));

        if (params.containsKey("enabled") && params.get("enabled") != null && (Boolean) params.get("enabled"))
            captureDevice.setEnabled(true);
        return captureDevice;
    }

    private static PortChain buildPortChain(Map<String, Object> params) {
        PortChain portChain = new PortChain();

        if (params.containsKey("name"))
            portChain.setName((String) params.get("name"));

        if (params.containsKey("type"))
            portChain.setType((String) params.get("type"));

        if (params.containsKey("outPort")) {
            Map<String, String> outPortParams = (Map<String, String>) params.get("outPort");
            portChain.setOutPort(buildSwitchAndPort(outPortParams));
        }

        if (params.containsKey("returnPort")) {
            Map<String, String> inPortParams = (Map<String, String>) params.get("returnPort");
            portChain.setReturnPort(buildSwitchAndPort(inPortParams));
        }

        if (params.containsKey("outRewrite")) {
            Map<String, String> outRewriteParams = (Map<String, String>) params.get("outRewrite");
            portChain.setOutRewrite(buildRewriteParams(outRewriteParams));
        }

        if (params.containsKey("returnRewrite")) {
            Map<String, String> returnRewriteParams = (Map<String, String>) params.get("returnRewrite");
            portChain.setReturnRewrite(buildRewriteParams(returnRewriteParams));
        }
        return portChain;
    }

    private static void assignSwitch(TappingApp app, Object opfSwitchId, String switchEntryId) {
        try {
            System.out.println("Attempting to assign OPFSwitch " + opfSwitchId + " to SwitchEntry " + switchEntryId);
            app.assignSwitch((String) opfSwitchId, switchEntryId);
            System.out.println("Assigned switch successfully");
        } catch (OPFSwitchIsAssignedException e) {
            System.out.println("Error assigning switch, OPFSwitch is assigned");
        } catch (OPFSwitchNotFoundException e) {
            System.out.println("Error assigning switch, OPFSwitch not found");
        } catch (NotFoundException e) {
            System.out.println("Error assigning switch, something not found");
        }
    }


    private static void unassignSwitch(TappingApp app, String switchEntryId) {
        try {
            System.out.println("Attempting to unasssign OPFSwitch from SwitchEntry " + switchEntryId);
            SwitchEntry switchEntry = app.getSwitchEntryById(switchEntryId);
            if (switchEntry != null && switchEntry.getOPFSwitch() != null)
                app.unassignSwitch(switchEntry.getOPFSwitch().getDataPathDesc(), switchEntry.getObjectId());
                System.out.println("Unassigned switch successfully");
        } catch (OPFSwitchIsNotAssignedException e) {
            System.out.println("Error unassigning switch, OPFSwitch is not assigned");
        } catch (OPFSwitchNotFoundException e) {
            System.out.println("Error unassigning switch, OPFSwitch not found");
        } catch (NotFoundException e) {
            System.out.println("Error unassigning switch, something not found");
        }
    }
}
