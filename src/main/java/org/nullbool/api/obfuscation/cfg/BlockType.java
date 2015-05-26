package org.nullbool.api.obfuscation.cfg;

import org.nullbool.api.util.InstructionUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.topdank.banalysis.filter.Filter;

public enum BlockType {
	
	EMPTY(new Filter<FlowBlock>() {
		@Override
		public boolean accept(FlowBlock block) {
			return block.cleansize() == 1 && block.accept(new Filter<AbstractInsnNode>() {
				@Override
				public boolean accept(AbstractInsnNode ai) {
					return ai.opcode() == Opcodes.GOTO;
				}
			});
		}
	}),
	
	END(new Filter<FlowBlock>() {
		@Override
		public boolean accept(FlowBlock block) {
			AbstractInsnNode ain = block.last();
			return ain != null && InstructionUtil.isExit(ain.opcode());
		}
	}),
	
	IMMEDIATE(new Filter<FlowBlock>() {
		@Override
		public boolean accept(FlowBlock block) {
			return !EMPTY.filter.accept(block) && !END.filter.accept(block);
		}
	});

	protected final Filter<FlowBlock> filter;

	BlockType(Filter<FlowBlock> filter) {
		this.filter = filter;
	}
}