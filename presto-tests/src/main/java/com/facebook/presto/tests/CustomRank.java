/*
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
package com.facebook.presto.tests;

import com.facebook.presto.operator.PagesIndex;
import com.facebook.presto.operator.window.WindowFunction;
import com.facebook.presto.spi.block.BlockBuilder;
import com.facebook.presto.spi.type.BigintType;
import com.facebook.presto.spi.type.Type;

import static com.facebook.presto.spi.type.BigintType.BIGINT;

public class CustomRank
        implements WindowFunction
{
    private long rank;
    private long count;

    @Override
    public Type getType()
    {
        return BigintType.BIGINT;
    }

    @Override
    public void reset(int partitionStartPosition, int partitionRowCount, PagesIndex pagesIndex)
    {
        rank = 0;
        count = 1;
    }

    @Override
    public void processRow(BlockBuilder output, boolean newPeerGroup, int peerGroupCount)
    {
        if (newPeerGroup) {
            rank += count;
            count = 1;
        }
        else {
            count++;
        }
        BIGINT.writeLong(output, rank);
    }
}
