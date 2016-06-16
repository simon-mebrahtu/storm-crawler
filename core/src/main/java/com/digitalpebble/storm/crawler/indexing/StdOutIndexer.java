/**
 * Licensed to DigitalPebble Ltd under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalpebble.storm.crawler.indexing;

import static com.digitalpebble.storm.crawler.Constants.StatusStreamName;

import java.util.Iterator;
import java.util.Map;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import com.digitalpebble.storm.crawler.Metadata;
import com.digitalpebble.storm.crawler.persistence.Status;

/**
 * Indexer which generates fields for indexing and sends them to the standard
 * output. Useful for debugging and as an illustration of what
 * AbstractIndexerBolt provides.
 */
@SuppressWarnings("serial")
public class StdOutIndexer extends AbstractIndexerBolt {
    OutputCollector _collector;

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map conf, TopologyContext context,
            OutputCollector collector) {
        super.prepare(conf, context, collector);
        _collector = collector;
    }

    @Override
    public void execute(Tuple tuple) {
        String url = tuple.getStringByField("url");

        // Distinguish the value used for indexing
        // from the one used for the status
        String normalisedurl = valueForURL(tuple);

        Metadata metadata = (Metadata) tuple.getValueByField("metadata");

        // should this document be kept?
        boolean keep = filterDocument(metadata);
        if (!keep) {
            // treat it as successfully processed even if
            // we do not index it
            _collector.emit(StatusStreamName, tuple, new Values(url, metadata,
                    Status.FETCHED));
            _collector.ack(tuple);
            return;
        }

        // display text of the document?
        if (fieldNameForText() != null) {
            String text = tuple.getStringByField("text");
            System.out.println(fieldNameForText() + "\t" + trimValue(text));
        }

        if (fieldNameForURL() != null) {
            System.out.println(fieldNameForURL() + "\t"
                    + trimValue(normalisedurl));
        }

        // which metadata to display?
        Map<String, String[]> keyVals = filterMetadata(metadata);

        Iterator<String> iterator = keyVals.keySet().iterator();
        while (iterator.hasNext()) {
            String fieldName = iterator.next();
            String[] values = keyVals.get(fieldName);
            for (String value : values) {
                System.out.println(fieldName + "\t" + trimValue(value));
            }
        }

        _collector.emit(StatusStreamName, tuple, new Values(url, metadata,
                Status.FETCHED));
        _collector.ack(tuple);
    }

    private String trimValue(String value) {
        if (value.length() > 100)
            return value.length() + " chars";
        return value;
    }

}