package org.nullbool.api.obfuscation.refactor;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nullbool.api.Context;
import org.objectweb.custom_asm.tree.ClassNode;
import org.objectweb.custom_asm.tree.MethodNode;

/**
 * @author Bibl (don't ban me pls)
 * @created 25 May 2015 (actually before this)
 */
public class InheritedMethodMap {
	private final Map<MethodNode, ChainData> methods;

	public InheritedMethodMap(ClassTree tree, boolean allowStatic) {
		methods = new HashMap<MethodNode, ChainData>();
		build(tree, allowStatic);
	}
	
	public InheritedMethodMap(ClassTree tree) {
		this(tree, false);
	}

	private int mCount = 0;
	private int aCount = 0;
	
	private void build(ClassTree tree, boolean allowStatic) {
		for (ClassNode node : tree.getClasses().values()) {
			for (MethodNode m : node.methods) {
				if (allowStatic || (!Modifier.isStatic(m.access))) {
					Set<MethodNode> supers    = tree.getMethodsFromSuper(node, m.name, m.desc);
					Set<MethodNode> delegates = tree.getMethodsFromDelegates(node, m.name, m.desc);
					ChainData data            = new ChainData(m, supers, delegates);
					this.methods.put(m, data);

					mCount ++;
					aCount += data.getAggregates().size();
				}
			}
		}
		
		//for(ChainData data : methods.values()){
		//	System.out.println(data);
		//}
	}
	
	public void output() {
		if(Context.current().getFlags().getOrDefault("basicout", true))
			System.out.println(String.format("Built map with %d methods connected with %d others.", mCount, aCount));
	}

	public ChainData getData(MethodNode m) {
		return methods.get(m);
	}
}