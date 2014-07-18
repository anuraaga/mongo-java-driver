/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.selector;

import org.mongodb.connection.ClusterDescription;
import org.mongodb.connection.ServerDescription;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.assertions.Assertions.notNull;

/**
 * A server selector that composes a list of server selectors, and selects the servers by iterating through the list from start to
 * finish, passing the result of the previous into the next, and finally returning the result of the last one.
 *
 * @since 3.0
 */
public final class CompositeServerSelector implements ServerSelector {
    private final List<ServerSelector> serverSelectors;

    /**
     * Constructs a new instance.
     *
     * @param serverSelectors the list of composed server selectors
     */
    public CompositeServerSelector(final List<ServerSelector> serverSelectors) {
        notNull("serverSelectores", serverSelectors);
        if (serverSelectors.isEmpty()) {
            throw new IllegalArgumentException("Server selectors can not be an empty list");
        }
        for (ServerSelector cur : serverSelectors) {
            if (cur == null) {
                throw new IllegalArgumentException("Can not have a null server selector in the list of composed selectors");
            }
        }
        this.serverSelectors = new ArrayList<ServerSelector>();
        for (ServerSelector cur : serverSelectors) {
            if (cur instanceof CompositeServerSelector) {
                this.serverSelectors.addAll(((CompositeServerSelector) cur).serverSelectors);
            } else {
                this.serverSelectors.add(cur);
            }
        }
    }

    @Override
    public List<ServerDescription> select(final ClusterDescription clusterDescription) {
        ClusterDescription curClusterDescription = clusterDescription;
        List<ServerDescription> choices = null;
        for (ServerSelector cur : serverSelectors) {
            choices = cur.select(curClusterDescription);
            curClusterDescription = new ClusterDescription(clusterDescription.getConnectionMode(), clusterDescription.getType(), choices);
        }

        return choices;
    }

    @Override
    public String toString() {
        return "{"
               + "serverSelectors=" + serverSelectors
               + '}';
    }
}