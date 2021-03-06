/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.ac.man.cs.llvm.ir.module;

import java.util.List;
import uk.ac.man.cs.llvm.bc.records.Records;
import uk.ac.man.cs.llvm.ir.FunctionGenerator;
import uk.ac.man.cs.llvm.ir.types.FunctionType;
import uk.ac.man.cs.llvm.ir.types.MetaType;
import uk.ac.man.cs.llvm.ir.types.PointerType;
import uk.ac.man.cs.llvm.ir.types.Type;

public class FunctionV32 extends FunctionV38 {

    public FunctionV32(ModuleVersion version, Types types, List<Type> symbols, FunctionGenerator generator, int mode) {
        super(version, types, symbols, generator, mode);
    }

    @Override
    protected void createAllocation(long[] args) {
        int i = 0;
        Type type = types.get(args[0]);
        i++; // Unused parameter
        int count = getIndexV0(args[i++]);
        int align = getAlign(args[i++]);

        code.createAllocation(type, count, align);

        symbols.add(type);
    }

    @Override
    protected void crateCall(long[] args) {
        int i = 2;
        int target = getIndex(args[i++]);
        int[] arguments = new int[args.length - i];
        int j = 0;
        while (i < arguments.length) {
            arguments[j++] = getIndex(args[i++]);
        }

        Type type = symbols.get(target).getType();

        if (type instanceof PointerType) {
            type = ((PointerType) type).getPointeeType();
        }

        Type returnType = ((FunctionType) type).getReturnType();

        code.createCall(returnType, target, arguments);

        if (returnType != MetaType.VOID) {
            symbols.add(returnType);
        }
    }

    @Override
    protected void createLoad(long[] args) {
        int i = 0;
        int source = getIndex(args[i++]);
        Type type;
        if (source < symbols.size()) {
            type = ((PointerType) symbols.get(source).getType()).getPointeeType();
        } else {
            type = ((PointerType) types.get(args[i++])).getPointeeType();
        }

        int align = getAlign(args[i++]);
        boolean isVolatile = args[i++] != 0;

        code.createLoad(type, source, align, isVolatile);

        symbols.add(type);
    }

    @Override
    protected void createSwitch(long[] args) {
        int i = 2;
        int condition = getIndex(args[i++]);
        int defaultBlock = (int) args[i++];
        int count = (int) args[i++];
        long[] caseConstants = new long[count];
        int[] caseBlocks = new int[count];
        for (int j = 0; j < count; j++) {
            i += 2;
            caseConstants[j] = Records.toSignedValue(args[i++]);
            caseBlocks[j] = (int) args[i++];
        }

        code.createSwitchOld(condition, defaultBlock, caseConstants, caseBlocks);

        code.exitBlock();
        code = null;
    }
}
