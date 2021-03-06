package org.opencb.opencga.storage.hadoop.variant.gaps;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos;
import org.apache.hadoop.io.BytesWritable;
import org.opencb.opencga.storage.core.metadata.StudyConfiguration;
import org.opencb.opencga.storage.hadoop.variant.index.VariantTableHelper;
import org.opencb.opencga.storage.hadoop.variant.mr.AnalysisTableMapReduceHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created on 09/03/18.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class FillMissingFromArchiveMapper extends TableMapper<BytesWritable, BytesWritable> {
    private AbstractFillFromArchiveTask task;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        VariantTableHelper helper = new VariantTableHelper(context.getConfiguration());

        StudyConfiguration studyConfiguration = helper.readStudyConfiguration();
        boolean overwrite = FillGapsFromArchiveMapper.isOverwrite(context.getConfiguration());
        task = new FillMissingFromArchiveTask(studyConfiguration, helper, overwrite);
        task.setQuiet(true);
        task.pre();
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        super.cleanup(context);
        try {
            task.post();
        } finally {
            updateStats(context);
        }
    }

    @Override
    protected void map(ImmutableBytesWritable key, Result value, Context context) throws IOException, InterruptedException {
        List<Put> puts = task.apply(Collections.singletonList(value));

        for (Put put : puts) {
            ClientProtos.MutationProto proto = ProtobufUtil.toMutation(ClientProtos.MutationProto.MutationType.PUT, put);
            context.write(new BytesWritable(put.getRow()), new BytesWritable(proto.toByteArray()));
        }
        updateStats(context);
    }

    private void updateStats(Context context) {
        for (Map.Entry<String, Long> entry : task.takeStats().entrySet()) {
            context.getCounter(AnalysisTableMapReduceHelper.COUNTER_GROUP_NAME, entry.getKey()).increment(entry.getValue());
        }
    }

}
