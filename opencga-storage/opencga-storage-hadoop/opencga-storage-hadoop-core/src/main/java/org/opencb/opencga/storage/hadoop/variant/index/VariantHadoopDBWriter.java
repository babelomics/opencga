/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.storage.hadoop.variant.index;

import org.apache.hadoop.hbase.client.Put;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.opencga.storage.core.metadata.ProjectMetadata;
import org.opencb.opencga.storage.core.metadata.StudyConfiguration;
import org.opencb.opencga.storage.hadoop.utils.AbstractHBaseDataWriter;
import org.opencb.opencga.storage.hadoop.utils.HBaseManager;
import org.opencb.opencga.storage.hadoop.variant.GenomeHelper;
import org.opencb.opencga.storage.hadoop.variant.HadoopVariantStorageEngine;
import org.opencb.opencga.storage.hadoop.variant.converters.study.StudyEntryToHBaseConverter;
import org.opencb.opencga.storage.hadoop.variant.search.HadoopVariantSearchIndexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 30/05/17.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class VariantHadoopDBWriter extends AbstractHBaseDataWriter<Variant, Put> {

    private final StudyEntryToHBaseConverter converter;
    private final GenomeHelper helper;
    private int skippedRefBlock = 0;
    private int skippedAll = 0;
    private int skippedRefVariants = 0;
    private int loadedVariants = 0;
    private int loadedVariantsAll = 0;
    private final Logger logger = LoggerFactory.getLogger(VariantHadoopDBWriter.class);

    private final LinkedHashMap<String, String> LOADED_VARIANTS_SET = new LinkedHashMap<String, String>(10000) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 10000;
        }
    };

    public VariantHadoopDBWriter(GenomeHelper helper, String tableName,
                                 ProjectMetadata pm, StudyConfiguration sc, HBaseManager hBaseManager) {
        this(helper, tableName, pm, sc, hBaseManager, false);
    }

    public VariantHadoopDBWriter(GenomeHelper helper, String tableName,
                                 ProjectMetadata pm, StudyConfiguration sc, HBaseManager hBaseManager,
                                 boolean includeReferenceVariantsData) {
        super(hBaseManager, tableName);
        this.helper = helper;
        converter = new StudyEntryToHBaseConverter(helper.getColumnFamily(), sc, true, pm.getRelease(), includeReferenceVariantsData);
    }

    @Override
    protected List<Put> convert(List<Variant> list) {
        List<Put> puts = new ArrayList<>(list.size());
        for (Variant variant : list) {
            if (HadoopVariantStorageEngine.TARGET_VARIANT_TYPE_SET.contains(variant.getType())) {
                if (!alreadyLoaded(variant)) {
                    Put put = converter.convert(variant);
                    if (put != null) {
                        HadoopVariantSearchIndexUtils.addUnknownSyncStatus(put, helper.getColumnFamily());
                        puts.add(put);
                        loadedVariants++;
                    } else {
                        skippedRefVariants++;
                    }
                }
                loadedVariantsAll++;
            } else { //Discard ref_block and symbolic variants.
                if (!alreadyLoaded(variant)) {
                    skippedRefBlock++;
                }
                skippedAll++;
            }
        }
        return puts;
    }

    private boolean alreadyLoaded(Variant v) {
        return LOADED_VARIANTS_SET.put(v.toString(), "") != null;
    }

    public int getSkippedRefBlock() {
        return skippedRefBlock;
    }

    public int getSkippedAll() {
        return skippedAll;
    }

    public int getSkippedRefVariants() {
        return skippedRefVariants;
    }

    public int getLoadedVariants() {
        return loadedVariants;
    }

    public int getLoadedVariantsAll() {
        return loadedVariantsAll;
    }
}
