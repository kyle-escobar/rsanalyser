/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.objectweb.custom_asm.commons;

/**
 * A {@link LocalVariablesSorter} for type mapping.
 * 
 * @author Eugene Kuleshov
 */
public class RemappingMethodAdapter extends LocalVariablesSorter {

    protected final Remapper remapper;

    public RemappingMethodAdapter(final int access, final String desc,
                                  final org.objectweb.custom_asm.MethodVisitor mv, final Remapper remapper) {
        this(org.objectweb.custom_asm.Opcodes.ASM5, access, desc, mv, remapper);
    }

    protected RemappingMethodAdapter(final int api, final int access,
                                     final String desc, final org.objectweb.custom_asm.MethodVisitor mv, final Remapper remapper) {
        super(api, access, desc, mv);
        this.remapper = remapper;
    }

    @Override
    public org.objectweb.custom_asm.AnnotationVisitor visitAnnotationDefault() {
        org.objectweb.custom_asm.AnnotationVisitor av = super.visitAnnotationDefault();
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }

    @Override
    public org.objectweb.custom_asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        org.objectweb.custom_asm.AnnotationVisitor av = super.visitAnnotation(remapper.mapDesc(desc),
                visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }

    @Override
    public org.objectweb.custom_asm.AnnotationVisitor visitTypeAnnotation(int typeRef,
                                                                          org.objectweb.custom_asm.TypePath typePath, String desc, boolean visible) {
        org.objectweb.custom_asm.AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath,
                remapper.mapDesc(desc), visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }

    @Override
    public org.objectweb.custom_asm.AnnotationVisitor visitParameterAnnotation(int parameter,
                                                                               String desc, boolean visible) {
        org.objectweb.custom_asm.AnnotationVisitor av = super.visitParameterAnnotation(parameter,
                remapper.mapDesc(desc), visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
            Object[] stack) {
        super.visitFrame(type, nLocal, remapEntries(nLocal, local), nStack,
                remapEntries(nStack, stack));
    }

    private Object[] remapEntries(int n, Object[] entries) {
        for (int i = 0; i < n; i++) {
            if (entries[i] instanceof String) {
                Object[] newEntries = new Object[n];
                if (i > 0) {
                    System.arraycopy(entries, 0, newEntries, 0, i);
                }
                do {
                    Object t = entries[i];
                    newEntries[i++] = t instanceof String ? remapper
                            .mapType((String) t) : t;
                } while (i < n);
                return newEntries;
            }
        }
        return entries;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name,
            String desc) {
        super.visitFieldInsn(opcode, remapper.mapType(owner),
                remapper.mapFieldName(owner, name, desc),
                remapper.mapDesc(desc));
    }

    @Deprecated
    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc) {
        if (api >= org.objectweb.custom_asm.Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, desc);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, desc,
                opcode == org.objectweb.custom_asm.Opcodes.INVOKEINTERFACE);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        if (api < org.objectweb.custom_asm.Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, desc, itf);
    }

    private void doVisitMethodInsn(int opcode, String owner, String name,
            String desc, boolean itf) {
        // Calling super.visitMethodInsn requires to call the correct version
        // depending on this.api (otherwise infinite loops can occur). To
        // simplify and to make it easier to automatically remove the backward
        // compatibility code, we inline the code of the overridden method here.
        // IMPORTANT: THIS ASSUMES THAT visitMethodInsn IS NOT OVERRIDDEN IN
        // LocalVariableSorter.
        if (mv != null) {
            mv.visitMethodInsn(opcode, remapper.mapType(owner),
                    remapper.mapMethodName(owner, name, desc),
                    remapper.mapMethodDesc(desc), itf);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, org.objectweb.custom_asm.Handle bsm,
            Object... bsmArgs) {
        for (int i = 0; i < bsmArgs.length; i++) {
            bsmArgs[i] = remapper.mapValue(bsmArgs[i]);
        }
        super.visitInvokeDynamicInsn(
                remapper.mapInvokeDynamicMethodName(name, desc),
                remapper.mapMethodDesc(desc), (org.objectweb.custom_asm.Handle) remapper.mapValue(bsm),
                bsmArgs);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, remapper.mapType(type));
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(remapper.mapValue(cst));
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(remapper.mapDesc(desc), dims);
    }

    @Override
    public org.objectweb.custom_asm.AnnotationVisitor visitInsnAnnotation(int typeRef,
                                                                          org.objectweb.custom_asm.TypePath typePath, String desc, boolean visible) {
        org.objectweb.custom_asm.AnnotationVisitor av = super.visitInsnAnnotation(typeRef, typePath,
                remapper.mapDesc(desc), visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }

    @Override
    public void visitTryCatchBlock(org.objectweb.custom_asm.Label start, org.objectweb.custom_asm.Label end, org.objectweb.custom_asm.Label handler,
                                   String type) {
        super.visitTryCatchBlock(start, end, handler, type == null ? null
                : remapper.mapType(type));
    }

    @Override
    public org.objectweb.custom_asm.AnnotationVisitor visitTryCatchAnnotation(int typeRef,
                                                                              org.objectweb.custom_asm.TypePath typePath, String desc, boolean visible) {
        org.objectweb.custom_asm.AnnotationVisitor av = super.visitTryCatchAnnotation(typeRef, typePath,
                remapper.mapDesc(desc), visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature,
                                   org.objectweb.custom_asm.Label start, org.objectweb.custom_asm.Label end, int index) {
        super.visitLocalVariable(name, remapper.mapDesc(desc),
                remapper.mapSignature(signature, true), start, end, index);
    }

    @Override
    public org.objectweb.custom_asm.AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
                                                                                   org.objectweb.custom_asm.TypePath typePath, org.objectweb.custom_asm.Label[] start, org.objectweb.custom_asm.Label[] end, int[] index,
                                                                                   String desc, boolean visible) {
        org.objectweb.custom_asm.AnnotationVisitor av = super.visitLocalVariableAnnotation(typeRef,
                typePath, start, end, index, remapper.mapDesc(desc), visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }
}
