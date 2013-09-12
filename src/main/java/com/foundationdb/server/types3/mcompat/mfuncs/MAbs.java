/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foundationdb.server.types3.mcompat.mfuncs;

import com.foundationdb.server.types3.LazyList;
import com.foundationdb.server.types3.TExecutionContext;
import com.foundationdb.server.types3.TScalar;
import com.foundationdb.server.types3.common.BigDecimalWrapper;
import com.foundationdb.server.types3.common.funcs.Abs;
import com.foundationdb.server.types3.mcompat.mtypes.MBigDecimal;
import com.foundationdb.server.types3.mcompat.mtypes.MApproximateNumber;
import com.foundationdb.server.types3.mcompat.mtypes.MNumeric;
import com.foundationdb.server.types3.pvalue.PValueSource;
import com.foundationdb.server.types3.pvalue.PValueTarget;

public class MAbs {

    private static final int DEC_INDEX = 0;

    public static final TScalar TINYINT = new Abs(MNumeric.TINYINT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt8((byte) Math.abs(inputs.get(0).getInt8()));
        }
    };
    public static final TScalar SMALLINT = new Abs(MNumeric.SMALLINT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt16((short) Math.abs(inputs.get(0).getInt16()));
        }
    };
    public static final TScalar MEDIUMINT = new Abs(MNumeric.MEDIUMINT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt32((int) Math.abs(inputs.get(0).getInt32()));
        }
    };
    public static final TScalar BIGINT = new Abs(MNumeric.BIGINT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt64((long) Math.abs(inputs.get(0).getInt64()));
        }
    };
    public static final TScalar INT = new Abs(MNumeric.INT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt32((int) Math.abs(inputs.get(0).getInt32()));
        }
    };
    public static final TScalar DECIMAL = new Abs(MNumeric.DECIMAL) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            BigDecimalWrapper wrapper =
                MBigDecimal.getWrapper(context, DEC_INDEX)
                .set(MBigDecimal.getWrapper(inputs.get(0), context.inputTInstanceAt(0)));
            output.putObject(wrapper.abs());
        }
    };
    public static final TScalar DOUBLE = new Abs(MApproximateNumber.DOUBLE) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putDouble(Math.abs(inputs.get(0).getDouble()));
        }
    };
}