package org.nullbool.impl.analysers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nullbool.api.Context;
import org.nullbool.api.analysis.AbstractClassAnalyser;
import org.nullbool.api.analysis.AnalysisException;
import org.nullbool.api.analysis.IFieldAnalyser;
import org.nullbool.api.analysis.IMethodAnalyser;
import org.nullbool.api.analysis.SupportedHooks;
import org.nullbool.api.output.APIGenerator;
import org.nullbool.api.util.EventCallGenerator;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.topdank.banalysis.asm.insn.InstructionPrinter;
import org.zbot.hooks.FieldHook;
import org.zbot.hooks.MethodHook;
import org.zbot.hooks.MethodHook.MethodType;

/**
 * @author MalikDz
 */
@SupportedHooks(fields = { "getNPCs&[NPC", "getPlayers&[Player", "getRegion&Region", "getWidgetPositionsX&[I", "getWidgetPositionsY&[I",
		"getCanvas&Ljava/awt/Canvas;", "getLocalPlayer&Player", "getWidgetNodes&Hashtable", "getMenuActions&[Ljava/lang/String;", "isSpellSelected&Z",
		"getSelectionState&I", "getMenuOptions&[Ljava/lang/String;", "getLoopCycle&I", "getCurrentWorld&I", "getGameState&I", "getCurrentLevels&[I",
		"getRealLevels&[I", "getSkillsExp&[I", "getSelectedItem&I", "isMenuOpen&Z", "getMenuX&I", "getMenuY&I", "getMenuWidth&I", "getMenuHeight&I",
		"getMenuSize&I", "getGroundItems&[[[Deque", "getTileSettings&[[[B", "getTileHeights&[[[I", "getMapScale&I", "getMapOffset&I", "getMapAngle&I",
		"getPlane&I", "getCameraX&I", "getCameraY&I", "getCameraZ&I", "getCameraYaw&I", "getCameraPitch&I", "getBaseX&I", "getBaseY&I", "getWidgets&[[Widget",
		"getClientSettings&[I", "getWidgetsSettings&[I", }, methods = { "loadObjDefinition&(II)LObjectDefinition;", "loadItemDefinition&(I)LItemDefinition;",
		"getPlayerModel&(I)LModel;", "reportException&(Ljava/lang/Throwable;Ljava/lang/String;)WrappedException", "processAction&?" })
public class ClientAnalyser extends AbstractClassAnalyser {

	public ClientAnalyser() throws AnalysisException {
		super("Client");
	}

	@Override
	protected List<IFieldAnalyser> registerFieldAnalysers() {
		return Arrays.asList(new ActorArrayHook(), new CurrentRegionHook(), new WidgetPositionXY(), new CanvasPlayerHook(), new ClientArrayHooks(),
				new MenuScreenHooks(), new GroundItemsHook(), new TileInfoHooks(), new MinimapHooks(), new CameraHooks(), new BaseXYHooks(), new WidgetsHook(),
				new SettingsHook());
	}

	@Override
	protected List<IMethodAnalyser> registerMethodAnalysers() {
		return Arrays.asList(new LoadDefinitionHook(), new ReportMethodHookAnalyser(), new ProccessActionMethodHookAnalyser());
	}

	@Override
	public boolean matches(ClassNode c) {
		return c.name.equalsIgnoreCase("client");
	}

	public class CanvasPlayerHook implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String type = "L" + "java/awt/Canvas" + ";";
			List<FieldHook> list = new ArrayList<FieldHook>();
			String hook = identifyField(Context.current().getClassNodes(), type);
			list.add(asFieldHook(hook, "getCanvas"));

			type = "L" + findObfClassName("Player") + ";";
			String p = identifyField(Context.current().getClassNodes(), type);
			list.add(asFieldHook(p, "getLocalPlayer"));

			return list;
		}
	}

	public class ActorArrayHook implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String hook, tempo;
			List<FieldHook> list = new ArrayList<FieldHook>();

			tempo = "[L" + findObfClassName("NPC") + ";";
			hook = identify(cn, tempo, 's');
			list.add(asFieldHook(hook, "getNPCs"));

			tempo = "[L" + findObfClassName("Player") + ";";
			hook = identify(cn, tempo, 's');
			list.add(asFieldHook(hook, "getPlayers"));

			return list;
		}
	}

	public class MinimapHooks implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String h, regex = ";.{0,1};V";
			List<FieldHook> list = new ArrayList<FieldHook>();
			MethodNode[] mn = findMethods(Context.current().getClassNodes(), regex, true);
			MethodNode m = identifyMethod(mn, false, "ldc 120.0");

			h = findField(m, true, true, 1, 's', "ldc 120.0");
			list.add(asFieldHook(h, "getMapScale", findMultiplier(h, true)));

			h = findField(m, true, true, 1, 's', "ldc 30.0");
			list.add(asFieldHook(h, "getMapOffset", findMultiplier(h, true)));

			h = findField(m, true, true, 1, 's', "ldc 20.0");
			list.add(asFieldHook(h, "getMapAngle", findMultiplier(h, true)));

			return list;
		}
	}

	public class CameraHooks implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String h, regex = ";III\\w{0,1};V";
			List<FieldHook> list = new ArrayList<FieldHook>();
			MethodNode[] mn = findMethods(Context.current().getClassNodes(), regex, true);
			MethodNode m = startWithBc(Context.current().getPattern("camera"), mn)[0];

			h = findNearIns(m, "invokestatic", "put", "get");
			list.add(asFieldHook(h, "getPlane", findMultiplier(h, true)));

			h = findField(m, true, false, 1, 's', "isub", "istore 0");
			list.add(asFieldHook(h, "getCameraX", findMultiplier(h, true)));

			h = findField(m, true, false, 1, 's', "isub", "istore 1");
			list.add(asFieldHook(h, "getCameraY", findMultiplier(h, true)));

			h = findField(m, true, false, 1, 's', "imul", "isub", "istore 4");
			list.add(asFieldHook(h, "getCameraZ", findMultiplier(h, true)));

			h = findField(m, true, false, 1, 's', "iaload", "istore 7");
			list.add(asFieldHook(h, "getCameraYaw", findMultiplier(h, true)));

			h = findField(m, true, false, 1, 's', "iaload", "istore 5");
			list.add(asFieldHook(h, "getCameraPitch", findMultiplier(h, true)));

			return list;
		}
	}

	public class MenuScreenHooks implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String h, regex = ";I.{0,2};V";
			List<FieldHook> list = new ArrayList<FieldHook>();
			String[] p = { "getstatic", "getstatic", "invokevirtual", "istore" };
			MethodNode[] mn = findMethods(Context.current().getClassNodes(), regex, false);

			MethodNode[] m = startWithBc(p, mn);
			AbstractInsnNode[] ins = followJump(m[0], 323);

			h = findField(ins, true, true, 1, 's', "sipush 503");
			list.add(asFieldHook(h, "isMenuOpen", findMultiplier(h, true)));

			h = findField(ins, true, true, 2, 's', "sipush 503");
			list.add(asFieldHook(h, "getMenuX", findMultiplier(h, true)));

			h = findField(ins, true, true, 3, 's', "sipush 503");
			list.add(asFieldHook(h, "getMenuY", findMultiplier(h, true)));

			h = findField(ins, true, true, 4, 's', "sipush 503");
			list.add(asFieldHook(h, "getMenuWidth", findMultiplier(h, true)));

			h = findField(ins, true, true, 5, 's', "sipush 503");
			list.add(asFieldHook(h, "getMenuSize", findMultiplier(h, true)));

			h = findField(ins, true, true, 6, 's', "sipush 503");
			list.add(asFieldHook(h, "getMenuHeight", findMultiplier(h, true)));

			return list;
		}
	}

	public class ClientArrayHooks implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String h, regex = ";;V";
			String[] p = { "iconst_1", "putstatic" };
			String[] pattern = { "bipush 9", "iconst_2", "iastore" };
			List<FieldHook> list = new ArrayList<FieldHook>();
			MethodNode[] mn = findMethods(Context.current().getClassNodes(), regex, false);
			MethodNode[] ms = startWithBc(p, mn);

			MethodNode m = identifyMethod(ms, false, pattern);
			AbstractInsnNode[] ins = followJump(m, 323);

			String[] pat = { "iconst_1", "putstatic", "iconst_0" };
			String[] r = { "aconst_null", "putstatic .* Ljava/lang/String;", "iconst_0" };
			r = new String[] { "putstatic .*", "new", "dup", "bipush 8" };
			h = findField(ins, true, true, 2, 's', r);
			list.add(asFieldHook(h, "getWidgetNodes"));

			h = findField(ins, true, true, 1, 's', "sipush 500", "anewarray");
			list.add(asFieldHook(h, "getMenuActions"));

			h = findField(ins, true, true, 9, 's', "sipush 500", "anewarray");
			list.add(asFieldHook(h, "isSpellSelected"));

			h = findField(ins, true, true, 7, 's', "sipush 500", "anewarray");
			list.add(asFieldHook(h, "getSelectionState"));

			h = findField(ins, true, true, 2, 's', "sipush 500", "anewarray");
			list.add(asFieldHook(h, "getMenuOptions"));

			h = findField(ins, false, true, 2, 's', pat);
			list.add(asFieldHook(h, "getLoopCycle"));

			h = findField(ins, true, true, 2, 's', "iconst_1");
			list.add(asFieldHook(h, "getCurrentWorld"));

			h = findField(ins, true, true, 8, 's', "iconst_1");
			list.add(asFieldHook(h, "getGameState"));

			h = findField(ins, true, true, 1, 's', "bipush 25", "newarray 10");
			list.add(asFieldHook(h, "getCurrentLevels"));

			h = findField(ins, true, true, 2, 's', "bipush 25", "newarray 10");
			list.add(asFieldHook(h, "getRealLevels"));

			h = findField(ins, true, true, 3, 's', "bipush 25", "newarray 10");
			list.add(asFieldHook(h, "getSkillsExp"));

			h = findField(ins, true, true, 1, 's', r);
			list.add(asFieldHook(h, "getSelectedItem"));

			return list;
		}
	}

	public class TileInfoHooks implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String hook, regex = ";III\\w{0,1};I";
			List<FieldHook> list = new ArrayList<FieldHook>();
			String bytesPattern = "getstatic \\w*.\\w* \\[\\[\\[B";
			String heightPattern = "getstatic \\w*.\\w* \\[\\[\\[I";

			MethodNode[] mn = findMethods(Context.current().getClassNodes(), regex, true);
			MethodNode method = identifyMethod(mn, false, "bipush 103");

			hook = findField(method, true, false, 1, 's', bytesPattern);
			list.add(asFieldHook(hook, "getTileSettings"));

			hook = findField(method, true, false, 1, 's', heightPattern);
			list.add(asFieldHook(hook, "getTileHeights"));

			return list;
		}
	}

	public class BaseXYHooks implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String obj = "L" + findObfClassName("Actor");
			String h, regex = ";\\w*" + obj + ";" + "\\w*;V";
			List<FieldHook> list = new ArrayList<FieldHook>();
			String mPattern = "invokestatic java/lang/Math.atan2 (DD)D";
			MethodNode[] mn = findMethods(Context.current().getClassNodes(), regex, true);
			MethodNode method = identifyMethod(mn, false, mPattern);
			AbstractInsnNode[] ins = followJump(method, 120);

			h = findField(ins, false, false, 1, 's', "isub", "isub", "istore");
			list.add(asFieldHook(h, "getBaseX", findMultiplier(h, true)));

			h = findField(ins, false, true, 1, 's', "isub", "isub", "istore");
			list.add(asFieldHook(h, "getBaseY", findMultiplier(h, true)));

			return list;
		}
	}

	public class SettingsHook implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String hook, r = ";;V";
			String[] pat = { "bipush", "newarray" };
			String[] pat2 = { "sipush 2000", "newarray 10" };
			List<FieldHook> list = new ArrayList<FieldHook>();
			MethodNode[] mn = findMethods(Context.current().getClassNodes(), r, true);
			MethodNode[] ms = startWithBc(pat, mn);
			MethodNode m = identifyMethod(ms, false, pat2);

			hook = findField(m, true, true, 1, 's', pat2);
			list.add(asFieldHook(hook, "getClientSettings"));

			hook = findField(m, true, true, 2, 's', pat2);
			list.add(asFieldHook(hook, "getWidgetsSettings"));

			return list;
		}
	}

	public class WidgetPositionXY implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			// String hook, regex = ";;V";
			List<FieldHook> list = new ArrayList<FieldHook>();

			List<MethodNode> methods = new ArrayList<MethodNode>();
			Context.current()
					.getClassNodes()
					.values()
					.forEach(
							x -> methods.addAll(x.methods.stream().filter(m -> m.desc.startsWith("(IIIII")).filter(m1 -> fourIaloads(m1))
									.collect(Collectors.toList())));

			for (MethodNode m : methods) {
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File("out/test/" + m.owner.name + " " + m.name.replace("<", "").replace(">", "")
						+ " " + m.desc.replace("<", "").replace(">", ""))))) {
					bw.write(m.owner.name + " " + m.name + " " + m.desc);
					for (String s : InstructionPrinter.getLines(m)) {
						bw.write(s);
						bw.newLine();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// String[] mPattern = { "iconst_1", "putstatic" };
			// MethodNode[] mn = findMethods(Context.current().getClassNodes(), regex, true);
			// MethodNode method = startWithBc(mPattern, mn)[0];

			// AbstractInsnNode[] ins = followJump(method, 220);
			// String[] p = { "bipush 100", "newarray 10", "putstatic client\\.\\w* \\[I", "bipush 100", "newarray 10", "putstatic client\\.\\w* \\[I"
			// };
			//
			// hook = findField(ins, true, true, 1, 's', p);
			//
			// list.add(asFieldHook(hook, "getWidgetPositionsX"));
			//
			// hook = findField(ins, true, true, 2, 's', p);
			// list.add(asFieldHook(hook, "getWidgetPositionsY"));

			return list;
		}

		private boolean fourIaloads(MethodNode m1) {
			for (AbstractInsnNode ain : m1.instructions.toArray()) {
				if (ain.getOpcode() == GETSTATIC || ain.getOpcode() == PUTSTATIC) {
					FieldInsnNode fin = (FieldInsnNode) ain;
					if (fin.name.equals("lv") || fin.name.equals("lu"))
						return true;
				}
			}
			return false;
		}
	}

	// TODO: METHODS
	public class LoadDefinitionHook implements IMethodAnalyser {

		@Override
		public List<MethodHook> find(ClassNode _unused) {
			List<MethodHook> list = new ArrayList<MethodHook>();
			String npcClass = findObfClassName("NPC");
			ClassNode cn = getClassNodeByRefactoredName("Renderable");
			MethodNode[] ms = getMethodNodes(cn.methods.toArray());
			String playerClass = findObfClassName("Player");
			MethodNode m = identifyMethod(ms, true, "aconst_null", "areturn");
			String v = "L" + findObfClassName("ItemDefinition") + ";";
			String mn, t = "L" + findObfClassName("ObjectDefinition") + ";";
			//
			MethodNode mNode = findMethodNode(Context.current().getClassNodes(), ";I.{0,1};" + t);
			MethodHook mhook = getAnalyser("ObjectDefinition").asMethodHook(MethodType.CALLBACK, mNode, "loadObjDefinition");
			list.add(mhook);
			//
			mNode = findMethodNode(Context.current().getClassNodes(), ";I.{0,1};" + v);
			mhook = getAnalyser("ItemDefinition").asMethodHook(MethodType.CALLBACK, mNode, "loadItemDefinition");
			list.add(mhook);
			//
			mn = npcClass + "." + m.name;
			mNode = findMethodNode(Context.current().getClassNodes(), ";I.{0,1};" + v);
			//
			mn = playerClass + "." + m.name;
			mhook = getAnalyser("Model").asMethodHook(MethodType.CALLBACK, mn, "getPlayerModel");
			list.add(mhook);

			// these hooks are actually method hook, I think we should make a
			// method format look

			// String name = "ObjectDefinition";
			// AbstractClassAnalyser analyser = getAnalyser(name);
			// ClassNode aCn = analyser.getFoundClass();
			// String defName = String.format("L%s;", aCn.name);
			// for (ClassNode cn : Context.current().getClassNodes().values()) {
			// for (MethodNode m : cn.methods) {
			// if (!Modifier.isStatic(m.access))
			// continue;
			// String desc = m.desc;
			// if (desc.startsWith("(I") && desc.endsWith(defName)) {
			// MethodHook hook = getAnalyser(name).asMethodHook(MethodType.CALLBACK, m, "loadObjDefinition");
			// list.add(hook);
			// }
			// }
			// }

			return list;
		}

		private MethodNode findMethodNode(Map<String, ClassNode> nodes, String r) {
			String g = "[()]";
			Iterator<ClassNode> it = nodes.values().iterator();
			while (it.hasNext()) {
				ClassNode cn = it.next();
				for (Object m : cn.methods)
					if ((Modifier.isStatic(((MethodNode) m).access)))
						if (((MethodNode) m).desc.replaceAll(g, ";").matches(r))
							return (MethodNode) m;
			}
			return null;
		}

		private String findMethod(Map<String, ClassNode> nodes, String r) {
			String g = "[()]", result = null;
			Iterator<ClassNode> it = nodes.values().iterator();
			while (it.hasNext()) {
				ClassNode cn = it.next();
				for (Object m : cn.methods)
					if ((Modifier.isStatic(((MethodNode) m).access)))
						if (((MethodNode) m).desc.replaceAll(g, ";").matches(r))
							return cn.name + "." + ((MethodNode) m).name;
			}
			return result;
		}
	}

	public class WidgetsHook implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			List<FieldHook> list = new ArrayList<FieldHook>();
			String t = "[[L" + findObfClassName("Widget") + ";";
			String widgetField = identifyField(Context.current().getClassNodes(), t);
			list.add(asFieldHook(widgetField, "getWidgets"));

			return list;
		}
	}

	public class GroundItemsHook implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			String t = "[[[L" + findObfClassName("Deque") + ";";
			String p = identifyField(Context.current().getClassNodes(), t);
			List<FieldHook> list = new ArrayList<FieldHook>();
			list.add(asFieldHook(p, "getGroundItems"));

			return list;
		}
	}

	public class CurrentRegionHook implements IFieldAnalyser {

		@Override
		public List<FieldHook> find(ClassNode cn) {
			List<FieldHook> list = new ArrayList<FieldHook>();

			String type = "L" + findObfClassName("Region") + ";";
			String p = identifyField(Context.current().getClassNodes(), type);
			list.add(asFieldHook(p, "getRegion"));

			return list;
		}
	}

	public class ReportMethodHookAnalyser implements IMethodAnalyser {

		@Override
		public List<MethodHook> find(ClassNode cn) {
			List<MethodHook> hooks = new ArrayList<MethodHook>();
			for (MethodNode m : getClassNodeByRefactoredName("ExceptionReporter").methods) {
				if (m.desc.startsWith("(Ljava/lang/Throwable;Ljava/lang/String;") && m.desc.contains(")L")) {
					MethodHook mhook = getAnalyser("ExceptionReporter").asMethodHook(MethodType.PATCH, m, "reportException");
					hooks.add(mhook);
					VarInsnNode beforeReturn = null;
					try {
						for (AbstractInsnNode ain : m.instructions.toArray()) {
							if (ain.getOpcode() == ARETURN) {
								if (beforeReturn != null)
									System.err.println("WTF BOI");
								beforeReturn = (VarInsnNode) ain.getPrevious();
							}
						}
						/* 1. Generate the event creation. 2. Call the dispatch method. */

						InsnList objCreateList = new InsnList();
						objCreateList.add(EventCallGenerator.generateEventCreate("org/zbot/api/event/ErrorEvent", "(Lorg/zbot/accessors/IWrappedException;)V",
								new VarInsnNode(beforeReturn.getOpcode(), beforeReturn.var), // load the raw object
								// cast it to an IWrappedException
								new TypeInsnNode(CHECKCAST, APIGenerator.ACCESSOR_BASE + APIGenerator.API_CANONICAL_NAMES.get("WrappedException"))));
						InsnList newInsns = EventCallGenerator.generateDispatch(objCreateList);

						InsnList mInsns = m.instructions;
						mInsns.insertBefore(beforeReturn, newInsns);
						mhook.setInstructions(mInsns);
						mhook.setMaxStack(7);
						mhook.setMaxLocals(m.maxLocals);
						mInsns.reset();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			return hooks;
		}
	}

	public class ProccessActionMethodHookAnalyser implements IMethodAnalyser {

		@Override
		public List<MethodHook> find(ClassNode _cn) {
			List<MethodHook> hooks = new ArrayList<MethodHook>();
			String descStart = "(IIIILjava/lang/String;Ljava/lang/String;II";
			for (ClassNode cn : Context.current().getClassNodes().values()) {
				for (MethodNode m : cn.methods) {
					if (m.desc.startsWith(descStart)) {
						InsnList insns = new InsnList();
						final String sb = "java/lang/StringBuilder";
						final String intAppendDesc = "(I)Ljava/lang/StringBuilder;";
						final String stringAppendDesc = "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
						final String append = "append";

						insns.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
						insns.add(new TypeInsnNode(NEW, sb));
						insns.add(new InsnNode(DUP));

						insns.add(new LdcInsnNode("[doAction] Op: "));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, "<init>", "(Ljava/lang/String;)V", false));

						insns.add(new VarInsnNode(ILOAD, 2));
						insns.add(new MethodInsnNode(INVOKEVIRTUAL, sb, append, intAppendDesc, false));
						insns.add(new LdcInsnNode(", Arg1: "));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, stringAppendDesc, false));

						insns.add(new VarInsnNode(ILOAD, 0));
						insns.add(new MethodInsnNode(INVOKEVIRTUAL, sb, append, intAppendDesc, false));
						insns.add(new LdcInsnNode(", Arg2: "));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, stringAppendDesc, false));

						insns.add(new VarInsnNode(ILOAD, 1));
						insns.add(new MethodInsnNode(INVOKEVIRTUAL, sb, append, intAppendDesc, false));
						insns.add(new LdcInsnNode(", Arg0: "));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, stringAppendDesc, false));

						insns.add(new VarInsnNode(ILOAD, 3));
						insns.add(new MethodInsnNode(INVOKEVIRTUAL, sb, append, intAppendDesc, false));
						insns.add(new LdcInsnNode(", Action: "));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, stringAppendDesc, false));

						insns.add(new VarInsnNode(ALOAD, 4));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, stringAppendDesc, false));

						insns.add(new LdcInsnNode(", Target: "));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, stringAppendDesc, false));

						insns.add(new VarInsnNode(ALOAD, 5));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, stringAppendDesc, false));
						insns.add(new LdcInsnNode(", var6: "));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, stringAppendDesc, false));

						insns.add(new VarInsnNode(ILOAD, 6));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, intAppendDesc, false));
						insns.add(new LdcInsnNode(", var7: "));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, stringAppendDesc, false));

						insns.add(new VarInsnNode(ILOAD, 7));
						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, append, intAppendDesc, false));

						insns.add(new MethodInsnNode(INVOKESPECIAL, sb, "toString", "()Ljava/lang/String;", false));
						insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));

						MethodHook h = asMethodHook(MethodType.PATCH_START, m, "processAction");
						h.setInstructions(insns);

						hooks.add(h);
					}
				}
			}
			return hooks;
			// static final void processAction (
			// int arg1, int arg2, int opcode, int arg0, String action, String target,
			// int mouseX, int mouseY, xxx DUMMY
			// )
		}
	}

	static final void processAction(int arg1, int arg2, int opcode, int arg0, String action, String target, int mouseX, int mouseY, int DUMMY) {
		System.out.println("[doAction] Op: " + opcode + ", Arg1: " + arg1 + ", Arg2: " + arg2 + ", Arg0: " + arg0 + ", Action: " + action + ", Target: "
				+ target + ", var6: " + mouseX + ", var7: " + mouseY);
	}
}