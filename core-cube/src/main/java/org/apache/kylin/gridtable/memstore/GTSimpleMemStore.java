package org.apache.kylin.gridtable.memstore;

import org.apache.kylin.gridtable.GTInfo;
import org.apache.kylin.gridtable.GTRowBlock;
import org.apache.kylin.gridtable.GTScanRequest;
import org.apache.kylin.gridtable.IGTStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GTSimpleMemStore implements IGTStore {

    final GTInfo info;
    final List<GTRowBlock> rowBlockList;

    public GTSimpleMemStore(GTInfo info) {
        this.info = info;
        this.rowBlockList = new ArrayList<GTRowBlock>();

        if (info.isShardingEnabled())
            throw new UnsupportedOperationException();
    }

    @Override
    public GTInfo getInfo() {
        return info;
    }

    public long memoryUsage() {
        if (rowBlockList.size() == 0) {
            return 0;
        } else {
            return rowBlockList.get(0).exportLength() * Long.valueOf(rowBlockList.size());
        }
    }

    @Override
    public IGTStoreWriter rebuild(int shard) {
        rowBlockList.clear();
        return new Writer(rowBlockList);
    }

    @Override
    public IGTStoreWriter append(int shard, GTRowBlock.Writer fillLast) {
        if (rowBlockList.size() > 0) {
            GTRowBlock last = rowBlockList.get(rowBlockList.size() - 1);
            fillLast.copyFrom(last);
        }
        return new Writer(rowBlockList);
    }

    private static class Writer implements IGTStoreWriter {

        private final List<GTRowBlock> rowBlockList;

        Writer(List<GTRowBlock> rowBlockList) {
            this.rowBlockList = rowBlockList;
        }
        @Override
        public void close() throws IOException {
        }

        @Override
        public void write(GTRowBlock block) throws IOException {
            GTRowBlock copy = block.copy();
            int id = block.getSequenceId();
            if (id < rowBlockList.size()) {
                rowBlockList.set(id, copy);
            } else {
                assert id == rowBlockList.size();
                rowBlockList.add(copy);
            }
        }
    }

    @Override
    public IGTStoreScanner scan( GTScanRequest scanRequest) {

        return new IGTStoreScanner() {
            Iterator<GTRowBlock> it = rowBlockList.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public GTRowBlock next() {
                return it.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    public void drop() throws IOException {
        //will there be any concurrent issue? If yes, ArrayList should be replaced
        rowBlockList.clear();
    }

}