package org.opencb.opencga.storage.hadoop.variant.gaps.write;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.opencb.opencga.storage.hadoop.variant.mr.AnalysisTableMapReduceHelper;

import java.io.IOException;

/**
 * Created on 09/03/18.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class FillMissingHBaseWriterMapper extends Mapper<BytesWritable, BytesWritable, ImmutableBytesWritable, Put> {

    @Override
    protected void map(BytesWritable key, BytesWritable value, Context context) throws IOException, InterruptedException {

        ClientProtos.MutationProto proto;
//        proto = ClientProtos.MutationProto.PARSER.parseFrom(new ByteArrayInputStream(value.getBytes(), 0, value.getLength()));
        proto = ClientProtos.MutationProto.PARSER.parseFrom(value.getBytes(), 0, value.getLength());

//        System.out.println(VariantPhoenixKeyFactory.extractVariantFromVariantRowKey(key.copyBytes()));

        Put put = ProtobufUtil.toPut(proto);
        context.getCounter(AnalysisTableMapReduceHelper.COUNTER_GROUP_NAME, "puts").increment(1);
        context.write(new ImmutableBytesWritable(put.getRow()), put);

    }
}
