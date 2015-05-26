package org.nullbool.api.obfuscation.cfg;

import static org.objectweb.asm.commons.util.Assembly.OPCODES;
import static org.objectweb.asm.commons.util.Assembly.pad;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.objectweb.asm.commons.util.Assembly;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.topdank.banalysis.filter.Filter;

/**
 * @author Bibl (don't ban me pls)
 * @created 25 May 2015
 */
public class FlowBlock {

	private static final String PADDING = "=====";
	private static final String PREFIX  = System.lineSeparator() + "   ";
	
	private final String id;
	private final List<AbstractInsnNode> insns;
	private final List<FlowBlock> predecessors;
	private final List<FlowBlock> successors;
	
	private FlowBlock next, prev, target;
	private boolean visited;

	public FlowBlock(String id) {
		this.id      = id;
		insns        = new ArrayList<AbstractInsnNode>();
		predecessors = new ArrayList<FlowBlock>();
		successors   = new ArrayList<FlowBlock>();
	}
	
	public String id() {
		return id;
	}
	
	public List<AbstractInsnNode> insns() {
		return insns;
	}
	
	public int size() {
		return insns.size();
	}
	
	public int cleansize() {
		int i = 0;
		ListIterator<AbstractInsnNode> it = insns.listIterator();
		while(it.hasNext()) {
			AbstractInsnNode ain = it.next();
			if(ain.opcode() != -1)
				i++;
		}
		return i;
	}
	
	public AbstractInsnNode first() {
		if(insns.size() > 0)
			return insns.get(0);
		return null;
	}
	
	public AbstractInsnNode last() {
		if(insns.size() > 0)
			return insns.get(insns.size() - 1);
		return null;
	}
	
	public List<FlowBlock> predecessors() {
		return predecessors;
	}
	
	public List<FlowBlock> successors() {
		return successors;
	}
	
	public FlowBlock next() {
		return next;
	}

	public void setNext(FlowBlock next) {
		this.next = next;
	}

	public FlowBlock prev() {
		return prev;
	}

	public void setPrev(FlowBlock prev) {
		this.prev = prev;
	}

	public FlowBlock target() {
		return target;
	}

	public void setTarget(FlowBlock target) {
		this.target = target;
	}

	public boolean visited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public void transfer(InsnList list, AbstractInsnNode pos) {
		for(AbstractInsnNode ain : insns) {
			list.insert(pos, ain);
			pos = ain;
		}
	}
	
	public BlockType type() {
		if (BlockType.EMPTY.filter.accept(this)) {
			return BlockType.EMPTY;
		} else if (BlockType.END.filter.accept(this)) {
			return BlockType.END;
		}
		return BlockType.IMMEDIATE;
	}
	
	public boolean accept(Filter<AbstractInsnNode> filter) {
		for (AbstractInsnNode ain : insns) {
			if (filter.accept(ain))
				return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return String.format("Block %s", id);
	}
	
	public String toVerboseString(Map<LabelNode, FlowBlock> labels) {
		StringBuilder sb = new StringBuilder(PADDING);

		int size = size();

		sb.append("Block #").append(id);
		sb.append("(len=").append(size);
		sb.append("(").append(cleansize()).append(")");
		sb.append(", pred=").append(predecessors.size());
		sb.append(", succ=").append(successors.size());
		sb.append(", ").append(type().name());
		sb.append(")");
		sb.append(PADDING);

		if(size > 0) {
			ListIterator<AbstractInsnNode> it = insns().listIterator();
			while(it.hasNext()) {
				AbstractInsnNode ain = it.next();
				sb.append(PREFIX).append(toString(ain, labels));
			}
		}
		
		for(FlowBlock block : predecessors) {
			sb.append(PREFIX).append("  > pred: #").append(block.id());
		}
		
		for(FlowBlock block : successors) {
			sb.append(PREFIX).append("  > succ: #").append(block.id());
		}

		return sb.toString();
	}

	private String toString(AbstractInsnNode insn, Map<LabelNode, FlowBlock> labels) {
		if (insn == null) {
			return "null";
		}
		int op = insn.opcode();
		if (op == -1) {
			return insn.toString();
		}
		StringBuilder sb = new StringBuilder();
		/* pad the opcode name so that all the extra information for the instructions is aligned on the column.
		 * TODO: maybe change the column length to the longest opcode name in the instruction set rather than
		 * out of all the possible ones(statically, the longest opcode name is invokedynamic).*/
		sb.append(pad(OPCODES[op].toLowerCase(), Assembly.LONGEST_OPCODE_NAME));

		switch (insn.type()) {
			case INT_INSN:
				sb.append(((IntInsnNode) insn).operand);
				break;
			case VAR_INSN:
				sb.append('#').append(((VarInsnNode) insn).var);
				break;
			case TYPE_INSN:
				sb.append(((TypeInsnNode) insn).desc);
				break;
			case FIELD_INSN:
				FieldInsnNode fin = (FieldInsnNode) insn;
				sb.append(fin.owner).append('.').append(fin.name).append(' ').append(fin.desc);
				break;
			case METHOD_INSN:
				MethodInsnNode min = (MethodInsnNode) insn;
				sb.append(min.owner).append('.').append(min.name).append(' ').append(min.desc);
				break;
			case JUMP_INSN:
				LabelNode target = ((JumpInsnNode) insn).label;
				sb.append('#');
				FlowBlock targ = labels.get(target);
				sb.append(targ != null ? targ.id() : "null");
				break;
			case LDC_INSN:
				Object cst = ((LdcInsnNode) insn).cst;
				if(cst instanceof String) {
					sb.append("\"").append(cst).append("\"");
				} else {
					sb.append(cst);
				}
				sb.append(" (").append(cst.getClass().getName()).append(")");
				break;
			case IINC_INSN:
				IincInsnNode iin = (IincInsnNode) insn;
				sb.append('#').append(iin.var).append(' ').append(iin.incr);
				break;
			case TABLESWITCH_INSN: {
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) insn;
				sb.append("TODO: implement");
//				sb.append("def: #").append(labels.get(tsin.dflt).id);
				
			}
				break;
			case LOOKUPSWITCH_INSN: {
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) insn;
				sb.append("[def -> #").append(labels.get(lsin.dflt).id).append("]");
				
				for(int i=0; i < lsin.keys.size(); i++) {
					sb.append(", ");
					int k = lsin.keys.get(i);
					LabelNode l = lsin.labels.get(i);
					sb.append("[").append(k).append(" -> #").append(labels.get(l).id).append("]");
				}
			}
				break;
			case MULTIANEWARRAY_INSN:
				MultiANewArrayInsnNode m = (MultiANewArrayInsnNode) insn;
				sb.append(m.desc).append(' ').append(m.dims);
				break;
		}
		return sb.toString();
	}
}