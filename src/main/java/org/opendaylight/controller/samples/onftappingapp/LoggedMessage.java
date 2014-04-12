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

import java.util.Date;

public class LoggedMessage {
        private String objectId;
        private Date timestamp;
        private LoggingLevelEnum logLevel;
        private String threadId;
        private String message;
        private String fileName;
        private String methodName;
        private long   lineNumber;
        private String hostname;
        private String hostIP;


        private String getObjectId() {
                return objectId;
        }
        public void setObjectId(String objectId) {
                this.objectId = objectId;
        }
        public Date getTimestamp() {
                return timestamp;
        }
        public void setTimestamp(Date timestamp) {
                this.timestamp = timestamp;
        }
        public LoggingLevelEnum getLogLevel() {
                return logLevel;
        }
        public void setLogLevel(LoggingLevelEnum logLevel) {
                this.logLevel = logLevel;
        }
        public String getThreadId() {
                return threadId;
        }
        public void setThreadId(String threadId) {
                this.threadId = threadId;
        }
        public String getMessage() {
                return message;
        }
        public void setMessage(String message) {
                this.message = message;
        }

        public String getFileName() {
                return fileName;
        }
        public void setFileName(String fileName) {
                this.fileName = fileName;
        }
        public String getMethodName() {
                return methodName;
        }
        public void setMethodName(String methodName) {
                this.methodName = methodName;
        }
        public long getLineNumber() {
                return lineNumber;
        }
        public void setLineNumber(long lineNumber) {
                this.lineNumber = lineNumber;
        }

        public String toString() {
                String logMsgStr = getTimestamp().toString() + " " + getLogLevel() + " " + getMessage();
                return logMsgStr;
        }
        public String getHostname() {
                return hostname;
        }
        public void setHostname(String hostname) {
                this.hostname = hostname;
        }
        public String getHostIP() {
                return hostIP;
        }
        public void setHostIP(String hostIP) {
                this.hostIP = hostIP;
        }

}




