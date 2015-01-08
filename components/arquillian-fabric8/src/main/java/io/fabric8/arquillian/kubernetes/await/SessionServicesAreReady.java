/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.arquillian.kubernetes.await;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.arquillian.utils.Util;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.utils.Filter;

import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class SessionServicesAreReady implements Callable<Boolean> {

    private final Session session;
    private final KubernetesClient kubernetesClient;
    private final String key;
    private final Boolean waitForConnection;

    public SessionServicesAreReady(KubernetesClient kubernetesClient, Session session, String key, Boolean waitForConnection) {
        this.session = session;
        this.kubernetesClient = kubernetesClient;
        this.key = key;
        this.waitForConnection = waitForConnection;
    }

    @Override
    public Boolean call() throws Exception {
        boolean result = true;
        Map<String, String> labels = Collections.singletonMap(key, session.getId());
        Filter<Service> serviceFilter = KubernetesHelper.createServiceFilter(labels);
        List<Service> services = Util.findServices(kubernetesClient, serviceFilter);

        if (services.isEmpty()) {
            result = false;
            session.getLogger().warn("No services are available yet, waiting...");
        } else if (waitForConnection) {

            for (Service s : services) {
                String serviceURL = KubernetesHelper.getServiceURL(s);
                String serviceStatus = null;
                try {
                    URL url = new URL(serviceURL);
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    serviceStatus = "Service: " + serviceURL + " is ready";
                } catch (Exception e) {
                    result = false;
                    serviceStatus = "Service: " + serviceURL + " is not ready! Error: " + e.getMessage();
                } finally {
                    session.getLogger().warn(serviceStatus);
                }
            }
        }
        return result;
    }

}