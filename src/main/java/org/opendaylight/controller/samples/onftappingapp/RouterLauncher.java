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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouterLauncher {

    private static Logger logger = LoggerFactory.getLogger(TappingApp.class);

    public static void main(String[] args) {

        TappingApp tappingApp = null;
        try {
            tappingApp = new TappingApp("TappingApp", "127.0.0.1", null);
        } catch (DBAuthenticationException | DatabaseConnectException e) {
            logger.error("Exception while starting TappingApp ", e);
            e.printStackTrace();
        }

        // It is used
        @SuppressWarnings("unused")
        Router router = new Router(tappingApp);
    }
}