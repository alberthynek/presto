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
package com.facebook.presto.operator.aggregation;

import com.facebook.presto.operator.aggregation.state.AccumulatorStateSerializer;
import com.facebook.presto.operator.aggregation.state.VarianceState;
import com.facebook.presto.spi.Page;
import com.facebook.presto.spi.block.Block;
import com.facebook.presto.spi.type.Type;
import com.facebook.presto.spi.type.TypeManager;
import com.facebook.presto.spi.type.TypeSignature;
import com.google.common.base.CaseFormat;
import com.google.common.base.Function;

import javax.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Locale.ENGLISH;

public final class AggregationUtils
{
    private AggregationUtils()
    {
    }

    public static void updateVarianceState(VarianceState state, double value)
    {
        state.setCount(state.getCount() + 1);
        double delta = value - state.getMean();
        state.setMean(state.getMean() + delta / state.getCount());
        state.setM2(state.getM2() + delta * (value - state.getMean()));
    }

    public static void mergeVarianceState(VarianceState state, VarianceState otherState)
    {
        long count = otherState.getCount();
        double mean = otherState.getMean();
        double m2 = otherState.getM2();

        checkArgument(count >= 0, "count is negative");
        if (count == 0) {
            return;
        }
        long newCount = count + state.getCount();
        double newMean = ((count * mean) + (state.getCount() * state.getMean())) / (double) newCount;
        double delta = mean - state.getMean();
        double m2Delta = m2 + delta * delta * count * state.getCount() / (double) newCount;
        state.setM2(state.getM2() + m2Delta);
        state.setCount(newCount);
        state.setMean(newMean);
    }

    public static Type getOutputType(@Nullable Method outputFunction, AccumulatorStateSerializer<?> serializer, TypeManager typeManager)
    {
        if (outputFunction == null) {
            return serializer.getSerializedType();
        }
        else {
            return typeManager.getType(TypeSignature.parseTypeSignature(outputFunction.getAnnotation(OutputFunction.class).value()));
        }
    }

    public static String generateAggregationName(String baseName, Type outputType, List<Type> inputTypes)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, outputType.getTypeSignature().toString()));
        for (Type inputType : inputTypes) {
            sb.append(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, inputType.getTypeSignature().toString()));
        }
        sb.append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, baseName.toLowerCase(ENGLISH)));

        return sb.toString();
    }

    // used by aggregation compiler
    @SuppressWarnings("UnusedDeclaration")
    public static Function<Integer, Block> pageBlockGetter(final Page page)
    {
        return new Function<Integer, Block>()
        {
            @Override
            public Block apply(Integer input)
            {
                return page.getBlock(input);
            }
        };
    }
}
