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

import java.net.UnknownHostException;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class PortChain {

        // Connect to the ODL slf4j logger
        protected static final Logger logger = LoggerFactory.getLogger(TappingApp.class);

        private long referenceCount = 0;
        private String objectId = "";
        private String name = "";
        private String type = "";
        private SwitchAndPort outPort;
        private SwitchAndPort returnPort;
        private RewriteParams outRewrite;
        private RewriteParams returnRewrite;

        public PortChain() {
                this.referenceCount = 0;
        }

        public PortChain(DBObject portChainObj) {
                this.setObjectId(((ObjectId) portChainObj.get("_id")).toString());
                this.setName((String) portChainObj.get("name"));
                this.setType((String) portChainObj.get("type"));
                this.setReferenceCount((long) portChainObj.get("refCount"));

                BasicDBObject outPortObj    = (BasicDBObject) portChainObj.get("outPort");
                BasicDBObject returnPortObj = (BasicDBObject) portChainObj.get("returnPort");

                BasicDBObject outRewriteObj    = (BasicDBObject) portChainObj.get("outRewriteParams");
                BasicDBObject returnRewriteObj = (BasicDBObject) portChainObj.get("returnRewriteParams");

                RewriteParams outRewrite = null;
                RewriteParams returnRewrite = null;

                try {
                        outRewrite = new RewriteParams(outRewriteObj);
                        returnRewrite = new RewriteParams(returnRewriteObj);
                } catch (UnknownHostException e) {
                        logger.error("Unable to resolve IP address", e);
                }

                this.setOutRewrite(outRewrite);
                this.setReturnRewrite(returnRewrite);

                SwitchAndPort outPort = new SwitchAndPort();
                // outPort.setObjectId((String) outPortObj.get("_id"));
                outPort.setName((String) outPortObj.get("name"));
                outPort.setSwitchId((String) outPortObj.get("switchId"));
                outPort.setSwitchPort((int) outPortObj.get("switchPort"));
                this.setOutPort(outPort);

                SwitchAndPort returnPort = new SwitchAndPort();
                // returnPort.setObjectId((String) returnPortObj.get("_id"));
                returnPort.setName((String) returnPortObj.get("name"));
                returnPort.setSwitchId((String) returnPortObj.get("switchId"));
                returnPort.setSwitchPort((int) returnPortObj.get("switchPort"));
                this.setReturnPort(returnPort);
        }

        public long getReferenceCount(){
                return this.referenceCount;
        }

        public void setReferenceCount(final long refCount) {
                this.referenceCount = refCount;
        }

        public String getObjectId() {
                return objectId;
        }

        public void setObjectId(String objectId) {
                this.objectId = objectId;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public String getType(){
                return this.type;
        }

        public void setType(String type){
                this.type = type;
        }
        public SwitchAndPort getOutPort() {
                return outPort;
        }

        public void setOutPort(SwitchAndPort outPort) {
                this.outPort = outPort;
        }

        public SwitchAndPort getReturnPort() {
                return returnPort;
        }

        public void setReturnPort(SwitchAndPort returnPort) {
                this.returnPort = returnPort;
        }

        public RewriteParams getOutRewrite() {
                return outRewrite;
        }

        public void setOutRewrite(RewriteParams outRewrite) {
                this.outRewrite = outRewrite;
        }

        public RewriteParams getReturnRewrite() {
                return returnRewrite;
        }

        public void setReturnRewrite(RewriteParams returnRewrite) {
                this.returnRewrite = returnRewrite;
        }

        public BasicDBObject getAsDocument() {
                BasicDBObject document = new BasicDBObject();

                document.put("refCount",   this.getReferenceCount());
                document.put("name",       this.getName());
                document.put("type",       this.getType());
                document.put("outPort",    this.getOutPort().getAsDocument());
                document.put("returnPort", this.getReturnPort().getAsDocument());

                if (this.getOutRewrite() != null)
                        document.put("outRewriteParams",    this.getOutRewrite().getAsDocument());

                if (this.getReturnRewrite() != null)
                        document.put("returnRewriteParams", this.getReturnRewrite().getAsDocument());

                return document;
        }

        public static Boolean adjustReferenceCount(final ReferenceCountEnum adjType, String switchEntryId) throws NotFoundException {

                DB database = TappingApp.getDatabase();
                DBCollection table = database.getCollection(DatabaseNames.getPortChainTableName());

                // Look for the SwitchEntry object by object ID in the database
                BasicDBObject searchQuery = new BasicDBObject();
                ObjectId id = new ObjectId(switchEntryId);
                searchQuery.put("_id", id);
                DBObject dbObj = table.findOne(searchQuery);

                if (dbObj == null)
                        throw new NotFoundException();

                // create an increment query
                DBObject modifier = new BasicDBObject("refCount", (adjType == ReferenceCountEnum.DECREMENT)  ? -1 : 1);
                DBObject incQuery = new BasicDBObject("$inc", modifier);

                // increment a counter value atomically
                WriteResult result = table.update(searchQuery, incQuery);
                return (result != null) ? true : false;
        }
}

