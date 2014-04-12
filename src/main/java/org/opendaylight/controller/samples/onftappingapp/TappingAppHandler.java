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

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import spark.Request;
import spark.Response;
import spark.Route;

public class TappingAppHandler {

    @SuppressWarnings("unused")
    private String url = null;

    public TappingAppHandler(String url) {

        this.url = url;

        get(new Route(url) {

            @Override
            public Object handle(Request request, Response response) {
                try {
                    return getAll();
                } catch (NotFoundException e) {
                    return "none found";
                }
            }

        });

        post(new Route(url) {

            @Override
            public Object handle(Request request, Response response) {

                Map<String, String> params = new Gson().fromJson(request.body(),
                        new TypeToken<HashMap<String, String>>() {}.getType());

                try {
                    return add(params);
                } catch (TapAppException | DuplicateEntryException e) {
                    response.status(404);
                    return "error";
                }
            }
        });

        delete(new Route(url + "/:id") {

            @Override
            public Object handle(Request request, Response response) {
                try {
                    remove(request.params("id"));
                    return "deleted";
                } catch (NotFoundException e) {
                    response.status(400);
                    return "not found";
                }
            }

        });

    }


    public String getAll() throws NotFoundException {
        return "[]";
    }

    public String add(Map<String, String> params) throws TapAppException, DuplicateEntryException {
        return "[]";
    }

    public String remove(String id) throws NotFoundException {
        return "[]";
    }

}