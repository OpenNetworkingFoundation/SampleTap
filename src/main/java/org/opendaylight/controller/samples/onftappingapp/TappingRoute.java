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
import java.util.HashMap;
import java.util.Map;

import javax.transaction.NotSupportedException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import spark.Request;
import spark.Response;
import spark.Route;

public abstract class TappingRoute extends Route {

    Gson gson = new Gson();

    public TappingRoute(final String url) {
        super(url);
    }

    @Override
    public Object handle(final Request request, final Response response) {
        try {

            Map<String, Object> params = new Gson().fromJson(request.body(),
                    new TypeToken<HashMap<String, Object>>() {
                    }.getType());

            return gson.toJson(this.execute(params, request, response));
        } catch (NotFoundException e) {
            response.status(404);
            e.printStackTrace();
            return "not found";
        } catch (TapAppException e) {
            e.printStackTrace();
            return "App exception";
        } catch (DuplicateEntryException e) {
            // TODO Auto-generated catch block
            response.status(400);
            e.printStackTrace();
            return "Duplicate entry";
        } catch (ObjectInUseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response.status(400);
            return "object in use";
        } catch(OPFSwitchIsAssignedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response.status(400);
            return "OPFSwitch Is Not Assigned";
        } catch(OPFSwitchIsNotAssignedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response.status(400);
            return "OPFSwitch Is Not Assigned";
        } catch(OPFSwitchNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response.status(400);
            return "OPFSwitch Is Not Found";
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response.status(400);
            return "unknown host";
        } catch (NotSupportedException e) {
             // TODO Auto-generated catch block
            e.printStackTrace();
            response.status(400);
            return "Normal mode is not suppoted";
        }
    }

    public abstract Object execute(final Map<String, Object> params,
            final Request request, final Response response)
            throws TapAppException, DuplicateEntryException, NotFoundException,
            ObjectInUseException, UnknownHostException, OPFSwitchIsAssignedException,
            OPFSwitchIsNotAssignedException, OPFSwitchNotFoundException, org.opendaylight.controller.samples.onftappingapp.TapAppException, org.opendaylight.controller.samples.onftappingapp.DuplicateEntryException, org.opendaylight.controller.samples.onftappingapp.NotFoundException, NotSupportedException;

}
