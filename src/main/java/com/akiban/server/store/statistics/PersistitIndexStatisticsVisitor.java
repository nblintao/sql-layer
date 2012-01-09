/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server.store.statistics;

import static com.akiban.server.store.statistics.IndexStatistics.*;

import com.akiban.ais.model.Index;
import com.akiban.server.store.IndexVisitor;

import com.akiban.server.store.statistics.histograms.Bucket;
import com.akiban.server.store.statistics.histograms.Sampler;
import com.akiban.server.store.statistics.histograms.Splitter;
import com.akiban.util.Flywheel;
import com.persistit.Key;
import com.persistit.Persistit;
import com.persistit.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** Analyze index exhaustively by visiting every key.
 */
public class PersistitIndexStatisticsVisitor extends IndexVisitor
{
    private static final Logger logger = LoggerFactory.getLogger(PersistitIndexStatisticsVisitor.class);
    private static final int BUCKETS_COUNT = 32;
    
    private Index index;
    private int columnCount;
    private long timestamp;
    private int rowCount;
    private Sampler<Key> keySampler;
    private Flywheel<Key> keysFlywheel = new Flywheel<Key>() {
        @Override
        protected Key createNew() {
            return new Key((Persistit)null);
        }
    };

    public PersistitIndexStatisticsVisitor(Index index, long indexRowCount) {
        this.index = index;
        
        columnCount = index.getColumns().size();
        timestamp = System.currentTimeMillis();
        rowCount = 0;
        KeySplitter splitter = new KeySplitter(columnCount, keysFlywheel);
        keySampler = new Sampler<Key>(splitter, BUCKETS_COUNT, indexRowCount, keysFlywheel);
    }
    
    private static class KeySplitter implements Splitter<Key> {
        @Override
        public int segments() {
            return keys.size();
        }

        @Override
        public List<? extends Key> split(Key keyToSample) {
            Key prev = keyToSample;
            for (int i = keys.size() ; i > 0; i--) {
                Key truncatedKey = keysFlywheel.get();
                prev.copyTo(truncatedKey);
                truncatedKey.setDepth(i);
                keys.set(i-1 , truncatedKey);
                prev = truncatedKey;
            }
            return keys;
        }

        private KeySplitter(int columnCount, Flywheel<Key> keysFlywheel) {
            keys = Arrays.asList(new Key[columnCount]);
            this.keysFlywheel = keysFlywheel;
        }

        private List<Key> keys;
        private Flywheel<Key> keysFlywheel;
    }
    
    public void init() {
        keySampler.init();
    }

    public void finish() {
        keySampler.finish();
    }

    protected void visit(Key key, Value value) {
        List<? extends Key> recycles = keySampler.visit(key);
        rowCount++;
        for (int i=0, len=recycles.size(); i < len; ++i) {
            keysFlywheel.recycle(recycles.get(i));
        }
    }

    public IndexStatistics getIndexStatistics() {
        IndexStatistics result = new IndexStatistics(index);
        result.setAnalysisTimestamp(timestamp);
        result.setRowCount(rowCount);
        result.setSampledCount(rowCount);
        List<List<Bucket<Key>>> segmentBuckets = keySampler.toBuckets();
        assert segmentBuckets.size() == columnCount
                : "expected " + columnCount + " seguments, saw " + segmentBuckets.size() + ": " + segmentBuckets;
        for (int colCountSegment = 0; colCountSegment < columnCount; colCountSegment++) {
            List<Bucket<Key>> segmentSamples = segmentBuckets.get(colCountSegment);
            int samplesCount = segmentSamples.size();
            List<HistogramEntry> entries = new ArrayList<HistogramEntry>(samplesCount);
            for (int s = 0; s < samplesCount; ++s) {
                Bucket<Key> sample = segmentSamples.get(s);
                Key key = sample.value();
                byte[] keyBytes = new byte[key.getEncodedSize()];
                System.arraycopy(key.getEncodedBytes(), 0, keyBytes, 0, keyBytes.length);
                HistogramEntry entry = new HistogramEntry(
                        key.toString(),
                        keyBytes,
                        sample.getEqualsCount(),
                        sample.getLessThanCount(),
                        sample.getLessThanDistinctsCount()
                );
                entries.add(entry);
            }
            Histogram histogram = new Histogram(index, colCountSegment+1, entries);
            result.addHistogram(histogram);
        }
        return result;
    }

}
