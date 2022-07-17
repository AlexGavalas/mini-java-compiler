import syntaxtree.*;
import java.util.*;
import visitor.GJDepthFirst;
import java.io.*;

public class ThirdVisitor extends GJDepthFirst<String, ClassInfo> {

	int registerCounter;
	int labelCounter;
	String file;
	BufferedWriter bw;

	ThirdVisitor(String fileName) {
		try {
			file = fileName;
			bw = new BufferedWriter(new FileWriter(fileName.replace(".java", ".ll")));
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	private String getI(String s) {
		if(s.equals("int")) return "i32";
		if(s.equals("boolean")) return "i1";
		if(s.equals("int[]")) return "i32*";
		return "i8*";
	}

	private String getRegister() {
		return "%_" + registerCounter++;
	}

	private String getLabel() {
		return "label" + labelCounter++;
	}

	/*
	f0 -> MainClass()
	f1 -> ( TypeDeclaration() )*
	f2 -> <EOF>
	*/
	public String visit(Goal n, ClassInfo info) {
		
		try {

			for(String s : info.members.keySet()) {

				ClassInfo c = info.members.get(s);

				if(c.name.equals(file.replaceAll(".*/", "").replace(".java", ""))) {
					bw.write("@." + c.name + "_vtable = global [0 x i8*] []\n");
				}
				else {
					bw.write("@." + c.name + "_vtable = global [" + c.functionsOffset.size() + " x i8*] [");
					
					for(int i = 0 ; i < c.functionsOffset.size() ; i++) {

						if(i != 0) bw.write(", ");

						bw.write("i8* bitcast (" + getI(c.functionsOffset.get(i).type) + " (i8*");

						for(int j = 0 ; j < c.functionsOffset.get(i).arguments.size() ; j++) {
							String type = c.functionsOffset.get(i).arguments.get(j);
							bw.write("," + getI(type));
						}

						ClassInfo p = getOuterDef(info,c.name ,c.functionsOffset.get(i).name);

						bw.write(")* @" + p.parent.name + "." + c.functionsOffset.get(i).name + " to i8*) ");
					}

					bw.write("]\n");
				}
			}

			bw.write(
				"\ndeclare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\n" +
				"declare void @exit(i32)" +
    			"@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
            	"@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n\n" +
            	"define void @print_int(i32 %i) {\n" +
            	"    %_str = bitcast [4 x i8]* @_cint to i8*\n" +
            	"    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
            	"    ret void\n" +
            	"}\n\n" +
            	"define void @throw_oob() {\n" +
            	"    %_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
            	"    call i32 (i8*, ...) @printf(i8* %_str)\n" +
            	"    call void @exit(i32 1)\n" +
            	"    ret void\n" +
            	"}\n\n");

			String status = n.f0.accept(this, info);
		
		
			if(n.f1.present()) {
				for(Enumeration<Node> e = n.f1.elements() ; e.hasMoreElements() ; ) {
					
					status = e.nextElement().accept(this, info);
				}
			}

			bw.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}

		return "OK";
	}

	/*
	f0 -> "class"
	f1 -> Identifier()
	f2 -> "{"
	f3 -> "public"
	f4 -> "static"
	f5 -> "void"
	f6 -> "main"
	f7 -> "("
	f8 -> "String"
	f9 -> "["
	f10 -> "]"
	f11 -> Identifier()
	f12 -> ")"
	f13 -> "{"
	f14 -> ( VarDeclaration () )*
	f15 -> ( Statement() )*
	f16 -> "}"
	f17 -> "}"
	*/
	public String visit(MainClass n, ClassInfo info) {
		
		try {

			bw.write("define i32 @main() {\n");

			String name = n.f1.accept(this, info);
		
			String register;
			
			ClassInfo main = info.members.get(name).functions.get("main");

			if(n.f14.present()) {

				for(Enumeration<Node> e = n.f14.elements() ; e.hasMoreElements() ; ) {
					register = e.nextElement().accept(this, main);
				}
			}
			if(n.f15.present()) {

				for(Enumeration<Node> e = n.f15.elements() ; e.hasMoreElements() ; ) {
					
					register = e.nextElement().accept(this, main);
				}
			}

			bw.write("ret i32 0\n}\n");
		}
		catch(IOException e) {
			e.printStackTrace();
		}

		return "OK";
	}

	/*
	f0 -> ClassDeclaration() | ClassExtendsDeclaration()
	*/
	public String visit(TypeDeclaration n, ClassInfo info) {
		return n.f0.accept(this, info);
	}

	/*
	f0 -> "class"
	f1 -> Identifier()
	f2 -> "{"
	f3 -> ( VarDeclaration() )*
	f4 -> ( MethodDeclaration() )*
	f5 -> "}"
	*/
	public String visit(ClassDeclaration n, ClassInfo info) {
		
		String name = n.f1.accept(this, info);

		if(n.f4.present()) {

			for(Enumeration<Node> e = n.f4.elements() ; e.hasMoreElements() ; ) {
				e.nextElement().accept(this, info.members.get(name));
			}
		}

		return "OK";
	}

	/*
	f0 -> "class"
	f1 -> Identifier()
	f2 -> "extends"
	f3 -> Identifier()
	f4 -> "{"
	f5 -> ( VarDeclaration() )*
	f6 -> ( MethodDeclaration() )*
	f7 -> "}"
	*/
	public String visit(ClassExtendsDeclaration n, ClassInfo info) {
		
		String name = n.f1.accept(this, info);

		if(n.f6.present()) {

			for(Enumeration<Node> e = n.f6.elements() ; e.hasMoreElements() ; ) {
				e.nextElement().accept(this, info.members.get(name));
			}
		}

		return "OK";
	}

	/*
	f0 -> Type()
	f1 -> Identifier()
	f2 -> ";"
	*/
	public String visit(VarDeclaration n, ClassInfo info) {
		
		String type = n.f0.accept(this, info);
		String name = n.f1.accept(this, info);

		try {
			bw.write("%" + name + " = alloca " + getI(type) + "\n");
			info.registers.put(name, "%" + name);
		}
		catch (IOException e) {
			e.printStackTrace();
		} 

		return "OK";
	}

	/*
	f0 -> "public"
	f1 -> Type()
	f2 -> Identifier()
	f3 -> "("	
	f4 -> ( FormalParameterList() )?
	f5 -> ")"
	f6 -> "{"
	f7 -> ( VarDeclaration() )*
	f8 -> ( Statement() )*
	f9 -> "return"
	f10 -> Expression()
	f11 -> ";"
	f12 -> "}"
	*/
	public String visit(MethodDeclaration n, ClassInfo info) {

		try {

			String type = n.f1.accept(this, info);
			String name = n.f2.accept(this, info);

			ClassInfo function = info.functions.get(name);

			bw.write("define " + getI(info.functions.get(name).type) + " @" + info.name + "." + name + "(i8* %this");

			if(n.f4.present()) {

				String[] args = n.f4.accept(this, info.functions.get(name)).split(" ");
				
				for(int i = 0 ; i < function.arguments.size() ; i++) bw.write(", " + getI(function.arguments.get(i)) + " %." + args[i]);

				bw.write(") {\n");

				for(int i = 0 ; i < function.arguments.size() ; i++) {
					
					String typeI = getI(function.arguments.get(i));

					bw.write("%" + args[i] + " = alloca " + typeI + "\n");
					bw.write("store " + typeI + " %." + args[i] + ", " + typeI + "* %" + args[i] + "\n");

					info.registers.put(args[i], "%" + args[i]);
				}
			}

			else bw.write(") {\n");
			
			if(n.f7.present()) {
				for(Enumeration<Node> e = n.f7.elements() ; e.hasMoreElements() ; ) {
					e.nextElement().accept(this, info.functions.get(name));
				}
			}

			if(n.f8.present()) { 
				for(Enumeration<Node> e = n.f8.elements() ; e.hasMoreElements() ; ) {
					e.nextElement().accept(this, info.functions.get(name));
				}
			}
			
			String register = n.f10.accept(this, info.functions.get(name));
			String t = getRegister();

			if(!getType(info.functions.get(name), register).equals("") && findReg(info.functions.get(name), register) != null) {

				register = findReg(info.functions.get(name), register);
				bw.write(t + " = load " + getI(type) + ", " + getI(type) + "* " + register + "\n");
			}
			else if(!getType(info.functions.get(name), register).equals("")) {

				String reg1 = getRegister();
				String reg2 = getRegister();
				String reg3 = getRegister();
				int o = getOffset(info.functions.get(name), register) + 8;

				bw.write("\t" + reg1 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write("\t" + reg2 + " = bitcast i8* " + reg1 + " to " + getI(type) + "*\n");
				bw.write(t + " = load " + getI(type) + ", " + getI(type) + "* " + reg2 + "\n");
			}
			else t = register;

			bw.write("ret " + getI(type) + " " + t + "\n");
			bw.write("}\n");
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		return "OK";
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "."
	f2 -> Identifier()
	f3 -> "("
	f4 -> ( ExpressionList() )?
	f5 -> ")"
	*/
	public String visit(MessageSend n, ClassInfo info) {
		
		String name = n.f0.accept(this, info);
		
		String method = n.f2.accept(this, info);
		String reg7 = getRegister();
		String reg6 = getRegister();
		ClassInfo def;
		String reg;

		if(info.registers.get(name) != null) reg = info.registers.get(name);
		else reg = info.allocaReg.get(name);

		String obj = getType(info, name);
		
		if(name.equals("this")) {
			def = getDef(info, method);
			name = "%this";
		}
		else if(obj.equals("")) def = getOuterDef(info, reg, method);	
		else def = getOuterDef(info, obj, method);

		try {

			String reg1 = getRegister();
			String reg2 = getRegister();
			String reg3 = getRegister();
			String reg4 = getRegister();
			String reg5 = getRegister();
			String reg8 = getRegister(); 

			if(findReg(info, name) != null) {
				String g = getRegister();
				bw.write(g + " = load i8*, i8** " + findReg(info, name) + "\n");
				name = g;
			}

			bw.write("\t" + reg1 + " = bitcast i8* " + name + " to i8***\n");
			bw.write("\t" + reg2 + " = load i8**, i8*** " + reg1 + "\n");
			bw.write("\t" + reg3 + " = getelementptr i8*, i8** " + reg2 + ", i32 " + def.functionOffset + "\n");
			bw.write("\t" + reg4 + " = load i8*, i8** " + reg3 + "\n");
			bw.write("\t" + reg5 + " = bitcast i8* " + reg4 + " to " + getI(def.type) + "(i8*");

			for(int i = 0 ; i < def.arguments.size() ; i++) bw.write(", " + getI(def.arguments.get(i)));

			bw.write(")*\n");

			if(n.f4.present()) {

				String[] args = n.f4.accept(this, info).split(" ");

				for(int i = 0 ; i < args.length ; i++) {
					
					String t = getRegister();
					
					if(args[i].equals("this")) args[i] = "%this";
					else if(findReg(info, args[i]) != null) {
						String r = getRegister();
						bw.write(r + " = load " + getI(def.arguments.get(i)) + ", " + getI(def.arguments.get(i)) + "* " +  findReg(info, args[i]) + "\n");
						args[i] = r;
					}
					else if(!getType(info, args[i]).equals("") && info.members.get(args[i]) == null) {
						int o = getOffset(info, args[i]) + 8;
						String type = getType(info, args[i]);
						String reg11 = getRegister();
						String reg12 = getRegister();
						String reg13 = getRegister();

						bw.write("\t" + reg11 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
						bw.write("\t" + reg12 + " = bitcast i8* " + reg11 + " to " + getI(type) + "*\n");
						bw.write("\t" + reg13 + " = load " + getI(def.arguments.get(i)) + ", " + getI(def.arguments.get(i)) + "* " +  reg12 + "\n");

						args[i] = reg13;

						if(!name.equals("%this")) info.registers.put(name, reg12);
					}
				}

				String reg10 = getRegister();
				
				bw.write(reg6 + " = call " + getI(def.type) + " " + reg5 + "(i8* " + name);

				for(int i = 0 ; i < args.length ; i++) bw.write(", " + getI(def.arguments.get(i)) + " " + args[i]);
			}

			else {
				String reg9 = getRegister();

				bw.write(reg6 + " = call " + getI(def.type) + " " + reg5 + "(i8* " + name);
			}
			
			bw.write(")\n");
			info.allocaReg.put(reg6, def.type);
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		return reg6;
	}

	public ClassInfo getDef(ClassInfo node, String name) {

		while(node != null) {

			if(node.functions.get(name) != null) return node.functions.get(name);
			else node = node.parent;
		}

		return null;
	}

	public ClassInfo getOuterDef(ClassInfo node, String name, String method) {

		while(node.parent != null) node = node.parent;

		ClassInfo n = node.members.get(name);

		while(n != null) {
			if(n.functions.get(method) != null) return n.functions.get(method);
			else n = n.parent;
		}

		return null;
	}

	/*
	f0 -> Expression()
	f1 -> ExpressionTail()
	*/	
	public String visit(ExpressionList n, ClassInfo info) {
		return n.f0.accept(this, info) + n.f1.accept(this, info);
	}

	/*
	f0 -> ( ExpressionTerm() )*
	*/
	public String visit(ExpressionTail n, ClassInfo info) {
		
		if(n.f0.present()) {
			
			String result = "";
			
			for(Enumeration<Node> e = n.f0.elements() ; e.hasMoreElements() ; ) {
				result = result + " " + e.nextElement().accept(this, info);
			}
			
			return result;
		}
		else return "";
	}

	/*
	f0 -> ","
	f1 -> Expression()
	*/
	public String visit(ExpressionTerm n, ClassInfo info) {
		return n.f1.accept(this, info).toString();
	}

	/*
	f0 -> FormalParameter()
	f1 -> FormalParameterTail()
	*/
	public String visit(FormalParameterList n, ClassInfo info) {
		return n.f0.accept(this, info) + n.f1.accept(this, info);
	}

	/*
	f0 -> Type()
	f1 -> Identifier()
	*/
	public String visit(FormalParameter n, ClassInfo info) {
		return n.f1.accept(this, info);
	}

	/*
	f0 -> ( FormalParameterTerm() )*
	*/
	public String visit(FormalParameterTail n, ClassInfo info) {

		String args = "";

		if(n.f0.present()) {
			for(Enumeration<Node> e = n.f0.elements() ; e.hasMoreElements() ; ) {
				args += " " + e.nextElement().accept(this, info);
			}
		}

		return args;
	}

	/*
	f0 -> ","
	f1 -> FormalParameter()
	*/
	public String visit(FormalParameterTerm n, ClassInfo info) {
		return n.f1.accept(this, info);
	}

	/*
	f0 -> Block() | AssignmentStatement() | ArrayAssignmentStatement() | IfStatement() | WhileStatement() | PrintStatement()
	*/
	public String visit(Statement n, ClassInfo info) {
		return n.f0.accept(this, info);
	}

	/*
	f0 -> "if"
	f1 -> "("
	f2 -> Expression()
	f3 -> ")"
	f4 -> Statement()
	f5 -> "else"
	f6 -> Statement()
	*/
	public String visit(IfStatement n, ClassInfo info) {
		
		String exp = n.f2.accept(this, info);

		try {
			String t = getRegister();
			String label1 = getLabel();
			String label2 = getLabel();
			String label3 = getLabel();

			if(findReg(info, exp) != null) {
				bw.write(t + " = load i1, i1* %" + exp + "\n");
				exp = t;
			}
			else if(!getType(info, exp).equals("")) {
				int o = getOffset(info, exp) + 8;
				String reg11 = getRegister();
				String reg12 = getRegister();
				String reg13 = getRegister();

				bw.write("\t" + reg11 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write("\t" + reg12 + " = bitcast i8* " + reg11 + " to i1*\n");
				bw.write("\t" + reg13 + " = load i1, i1* " + reg12 + "\n");

				exp = reg13;
			}

			bw.write("br i1 " + exp + ", label %" + label1 + ", label %" + label2 + "\n");

			bw.write(label1 + ":\n");
			n.f4.accept(this, info);
			
			bw.write("br label %" + label3 + "\n");
			bw.write(label2 + ":\n");
			
			n.f6.accept(this, info);
			bw.write("br label %" + label3 + "\n");
			bw.write(label3 + ":\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return "OK";
	}

	/*
	f0 -> "while"
	f1 -> "("
	f2 -> Expression()
	f3 -> ")"
	f4 -> Statement()
	*/
	public String visit(WhileStatement n, ClassInfo info) {

		try {
			String t = getRegister();
			String t2 = getRegister();
			String label1 = getLabel();
			String label2 = getLabel();
			String start = getLabel();

			bw.write("br label %" + start + "\n");
			bw.write(start + ":\n");

			String exp = n.f2.accept(this, info);

			if(findReg(info, exp) != null) {
				exp = findReg(info, exp);
				bw.write(t + " = load i1, i1* " + exp + "\n");
			}
			else t = exp;
			
			bw.write("br i1 " + t + ", label %" + label1 + ", label %" + label2 + "\n");
			bw.write(label1 + ":\n");

			n.f4.accept(this, info);

			bw.write("br label %" + start + "\n");
			bw.write(label2 + ":\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return "OK";
	}

	/*
	f0 -> "{"
	f1 -> ( Statement() )*
	f2 -> "}"
	*/
	public String visit(Block n, ClassInfo info) {

		if(n.f1.present()) {
			for(Enumeration<Node> e = n.f1.elements() ; e.hasMoreElements() ; ) {
				e.nextElement().accept(this, info);
			}
		}
		
		return "OK";
	}

	/*
	f0 -> Identifier()
	f1 -> "="
	f2 -> Expression()
	f3 -> ";"
	*/
	public String visit(AssignmentStatement n, ClassInfo info) {
		
		String name = n.f0.accept(this, info);
		String exp = n.f2.accept(this, info);

		String type = getType(info, name);

		try {
			String t = getRegister();
			String origin = info.registers.get(name);

			if(findReg(info, name) == null) {
				int o = getOffset(info, name) + 8;
				String reg1 = getRegister();
				String reg2 = getRegister();
				String reg3 = getRegister();

				if(findReg(info, exp) != null) {
					exp = findReg(info, exp);
					bw.write(reg3 + " = load " + getI(type) + ", " + getI(type) + "* " + exp + "\n");
				}
				else reg3 = exp;

				bw.write("\t" + reg1 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write("\t" + reg2 + " = bitcast i8* " + reg1 + " to " + getI(type) + "*\n");
				bw.write("\t" + "store " + getI(type) + " " + reg3 + " , " + getI(type) + "* " + reg2 + "\n\n");

				info.registers.put(name, reg2);
			}
			else if(!exp.equals("this") && findReg(info, exp) == null && !getType(info, exp).equals("")) {
				int o = getOffset(info, exp) + 8;
				String reg1 = getRegister();
				String reg2 = getRegister();
				String reg3 = getRegister();
				String reg4 = getRegister();

				bw.write("\t" + reg1 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write("\t" + reg2 + " = bitcast i8* " + reg1 + " to " + getI(type) + "*\n");
				bw.write("\t" + reg4 + " = load " + getI(type) + ", "  +getI(type) + "* " + reg2 + "\n");
				bw.write("\t" + "store " + getI(type) + " " + reg4 + " , " + getI(type) + "* " + findReg(info, name) + "\n\n");
			}
			else {
				if(exp.equals("this")) exp = "%" + exp;
				if(findReg(info, exp) != null) {
					String reg3 = getRegister();
					bw.write(reg3 + " = load " + getI(type) + ", " + getI(type) + "* " + findReg(info, exp) + "\n");
					exp = reg3;
				}
				bw.write("\t" + "store " + getI(type) + " " + exp + " , " + getI(type) + "* %" + name + "\n\n");

				info.registers.put(name, "%" + name);
			}

		}

		catch (IOException e) {
			e.printStackTrace();
		}			

		return "OK";
	}

	public int getOffset(ClassInfo n, String s) {
		
		while(n != null) {
			if(n.members.get(s) != null) return n.members.get(s).offset;
			else n = n.parent;
		}
		return 0;
	}

	public String getType(ClassInfo node, String var) {
		
		if(var == null) return "";

		if(var.equals("this")) {
			while(node != null) {
				if(node.type.equals(node.name)) return node.name;
				else node = node.parent;
			}
			return "";
		}

		while(node != null) {

			if(node.members.get(var) != null) return node.members.get(var).type;
			else node = node.parent;
		}
		return "";
	}

	/*
	f0 -> AndExpression() | CompareExpression() | PlusExpression() | MinusExpression() | TimesExpression() | ArrayLookup() | ArrayLength() | MessageSend() | Clause()
	*/
	public String visit(Expression n, ClassInfo info) {
		return n.f0.accept(this, info);
	}

	/*
	f0 -> Clause()
	f1 -> "&&"
	f2 -> Clause()
	*/
	public String visit(AndExpression n, ClassInfo info) {
		
		String reg4 = getRegister();
		String reg3 = getRegister();

		try {

			String reg1 = getRegister();
			String reg2 = getRegister();

			String exp = n.f0.accept(this, info);
			String exp2 = n.f2.accept(this, info);

			if(info.registers.get(exp) != null) {
				exp = info.registers.get(exp);
				bw.write(reg1 + " = load i1 , i1* " + exp + "\n");
			}
			else reg1 = exp;

			if(info.registers.get(exp2) != null) {
				exp2 = info.registers.get(exp2);
				bw.write(reg2 + " = load i1 , i1* " + exp2 + "\n");
			}
			else reg2 = exp2;

			bw.write(reg3 + " = and i1 " + reg1 + ", " + reg2 + "\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return reg3;
	}

	private String findReg(ClassInfo n, String name) {
		while(n != null) {
			if(n.registers.get(name) != null) return n.registers.get(name);
			else n = n.parent;
		}

		return null;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "<"
	f2 -> PrimaryExpression()
	*/
	public String visit(CompareExpression n, ClassInfo info) {
		
		String reg4 = getRegister();
		String reg3 = getRegister();

		try {

			String reg1 = getRegister();
			String reg2 = getRegister();

			String exp = n.f0.accept(this, info);
			String exp2 = n.f2.accept(this, info);

			if(findReg(info, exp) != null) {
				if(info.members.get(exp) == null) {
					int o = getOffset(info, exp) + 8;
					String reg6 = getRegister();
					String reg5 = getRegister();

					bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
					bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
					exp = reg5;
				}
				else exp = findReg(info, exp);

				bw.write(reg1 + " = load i32 , i32* " + exp + "\n");
			}
			else if(!getType(info, exp).equals("") && info.members.get(exp) == null) {
				int o = getOffset(info, exp) + 8;
				String reg6 = getRegister();
				String reg5 = getRegister();

				bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
				bw.write(reg1 + " = load i32 , i32* " + reg5 + "\n");
			}
			else reg1 = exp;

			if(findReg(info, exp2) != null) {
				if(info.members.get(exp2) == null) {
					int o = getOffset(info, exp2) + 8;
					String reg6 = getRegister();
					String reg5 = getRegister();

					bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
					bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
					exp2 = reg5;
				}
				else exp2 = findReg(info, exp2);

				bw.write(reg2 + " = load i32 , i32* " + exp2 + "\n");
			}
			else if(!getType(info, exp2).equals("") && info.members.get(exp2) == null) {
				int o = getOffset(info, exp2) + 8;
				String reg6 = getRegister();
				String reg5 = getRegister();

				bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
				bw.write(reg2 + " = load i32 , i32* " + reg5 + "\n");
			}
			else reg2 = exp2;

			bw.write(reg3 + " = icmp slt i32 " + reg1 + ", " + reg2 + "\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return reg3;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "+"
	f2 -> PrimaryExpression()
	*/
	public String visit(PlusExpression n, ClassInfo info) {
		
		String reg4 = getRegister();
		String reg3 = getRegister();

		try {

			String reg1 = getRegister();
			String reg2 = getRegister();

			String exp = n.f0.accept(this, info);
			String exp2 = n.f2.accept(this, info);

			if(findReg(info, exp) != null) {
				if(info.members.get(exp) == null) {
					int o = getOffset(info, exp) + 8;
					String reg6 = getRegister();
					String reg5 = getRegister();

					bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
					bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
					exp = reg5;
				}
				else exp = findReg(info, exp);

				bw.write(reg1 + " = load i32 , i32* " + exp + "\n");
			}
			else if(!getType(info, exp).equals("") && info.members.get(exp) == null) {
				int o = getOffset(info, exp) + 8;
				String reg6 = getRegister();
				String reg5 = getRegister();

				bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
				bw.write(reg1 + " = load i32 , i32* " + reg5 + "\n");
			}
			else reg1 = exp;

			if(findReg(info, exp2) != null) {
				if(info.members.get(exp2) == null) {
					int o = getOffset(info, exp2) + 8;
					String reg6 = getRegister();
					String reg5 = getRegister();

					bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
					bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
					exp2 = reg5;
				}
				else exp2 = findReg(info, exp2);

				bw.write(reg2 + " = load i32 , i32* " + exp2 + "\n");
			}
			else if(!getType(info, exp2).equals("") && info.members.get(exp2) == null) {
				int o = getOffset(info, exp2) + 8;
				String reg6 = getRegister();
				String reg5 = getRegister();

				bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
				bw.write(reg2 + " = load i32 , i32* " + reg5 + "\n");
			}
			else reg2 = exp2;

			bw.write(reg3 + " = add i32 " + reg1 + ", " + reg2 + "\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return reg3;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "-"
	f2 -> PrimaryExpression()
	*/
	public String visit(MinusExpression n, ClassInfo info) {


		String reg4 = getRegister();
		String reg3 = getRegister();

		try {

			String reg1 = getRegister();
			String reg2 = getRegister();

			String exp = n.f0.accept(this, info);
			String exp2 = n.f2.accept(this, info);

			if(findReg(info, exp) != null) {
				if(info.members.get(exp) == null) {
					int o = getOffset(info, exp) + 8;
					String reg6 = getRegister();
					String reg5 = getRegister();

					bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
					bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
					exp = reg5;
				}
				else exp = findReg(info, exp);

				bw.write(reg1 + " = load i32 , i32* " + exp + "\n");
			}
			else if(!getType(info, exp).equals("") && info.members.get(exp) == null) {
				int o = getOffset(info, exp) + 8;
				String reg6 = getRegister();
				String reg5 = getRegister();

				bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
				bw.write(reg1 + " = load i32 , i32* " + reg5 + "\n");
			}
			else reg1 = exp;

			if(findReg(info, exp2) != null) {
				if(info.members.get(exp2) == null) {
					int o = getOffset(info, exp2) + 8;
					String reg6 = getRegister();
					String reg5 = getRegister();

					bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
					bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
					exp2 = reg5;
				}
				else exp2 = findReg(info, exp2);

				bw.write(reg2 + " = load i32 , i32* " + exp2 + "\n");
			}
			else if(!getType(info, exp2).equals("") && info.members.get(exp2) == null) {
				int o = getOffset(info, exp2) + 8;
				String reg6 = getRegister();
				String reg5 = getRegister();

				bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
				bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
				bw.write(reg2 + " = load i32 , i32* " + reg5 + "\n");
			}
			else reg2 = exp2;

			bw.write(reg3 + " = sub i32 " + reg1 + ", " + reg2 + "\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return reg3;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "*"
	f2 -> PrimaryExpression()
	*/
	public String visit(TimesExpression n, ClassInfo info) {
		
		String reg4 = getRegister();
		String reg3 = getRegister();

		try {

			String reg1 = getRegister();
			String reg2 = getRegister();

			String exp = n.f0.accept(this, info);
			String exp2 = n.f2.accept(this, info);

			if(findReg(info, exp) != null) {
				if(info.members.get(exp) == null) {
					int o = getOffset(info, exp) + 8;
					String reg6 = getRegister();
					String reg5 = getRegister();

					bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
					bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
					exp = reg5;
				}
				else exp = findReg(info, exp);

				bw.write(reg1 + " = load i32 , i32* " + exp + "\n");
			}
			else reg1 = exp;

			if(findReg(info, exp2) != null) {
				if(info.members.get(exp2) == null) {
					int o = getOffset(info, exp2) + 8;
					String reg6 = getRegister();
					String reg5 = getRegister();

					bw.write(reg6 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
					bw.write(reg5 + " = bitcast i8* " + reg6 + " to i32*\n");
					exp2 = reg5;
				}
				else exp2 = findReg(info, exp2);

				bw.write(reg2 + " = load i32 , i32* " + exp2 + "\n");
			}
			else reg2 = exp2;

			bw.write(reg3 + " = mul i32 " + reg1 + ", " + reg2 + "\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return reg3;
	}

	/*
	f0 -> "System.out.println"
	f1 -> "("
	f2 -> Expression()
	f3 -> ")"
	f4 -> ";"
	*/
	public String visit(PrintStatement n, ClassInfo info) {
		
		String exp = n.f2.accept(this, info);

		try {
			if(findReg(info, exp) != null) {
				String t = getRegister();
				bw.write(t + " = load i32, i32* " + findReg(info, exp) + "\n");
				exp = t;
			}

			bw.write("call void (i32) @print_int(i32 " + exp + ")\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return "OK";
	}

	/*
	f0 -> NotExpression() | PrimaryExpression()
	*/
	public String visit(Clause n, ClassInfo info) {
		return n.f0.accept(this, info);
	}

	/*
	f0 -> IntegerLiteral() | TrueLiteral() | FalseLiteral() | Identifier() | ThisExpression() | ArrayAllocationExpression() | AllocationExpression() | BracketExpression()
	*/
	public String visit(PrimaryExpression n, ClassInfo info) {
		return n.f0.accept(this, info);
	}

	public String visit(IntegerLiteral n, ClassInfo info) {
		return n.f0.toString();
	}

	public String visit(TrueLiteral n, ClassInfo info) {
		return "1";
	}

	public String visit(FalseLiteral n, ClassInfo info) {
		return "0";
	}

	public String visit(ThisExpression n, ClassInfo info) {
		return "this";
	}

	public String visit(Identifier n, ClassInfo info) {
		return n.f0.toString();
	}

	/*
	f0 -> "new"
	f1 -> "int"
	f2 -> "["
	f3 -> Expression()
	f4 -> "]"
	*/
	public String visit(ArrayAllocationExpression n, ClassInfo info) {
		
		String exp = n.f3.accept(this, info);

		String reg = getRegister();
		String reg5 = getRegister();

		try {
			String reg1 = getRegister();
			String reg2 = getRegister();
			String label1 = getLabel();
			String label2 = getLabel();

			if(findReg(info, exp) != null) bw.write(reg1 + " = load i32, i32* " + findReg(info, exp) + "\n");
			else reg1 = exp; 

			bw.write(reg2 + " = icmp slt i32 " + reg1 + ", 0\n");
			bw.write("br i1 " + reg2 + ", label %" + label1 + ", label %" + label2 + "\n");
			bw.write(label1 + ":\n");
			bw.write("call void @throw_oob()\n");
			bw.write("br label %" + label2 + "\n");
			bw.write(label2 + ":\n");

			String reg3 = getRegister();
			String reg4 = getRegister();

			bw.write(reg3 + " = add i32 " + reg1 + ", 1\n");
			bw.write(reg4 + " = call i8* @calloc(i32 4, i32 " + reg3 + ")\n");
			bw.write(reg5 + " = bitcast i8* " + reg4 + " to i32*\n");
			bw.write("store i32 " + reg1 + ", i32* " + reg5 + "\n\n");
		}

		catch (IOException e) {
			e.printStackTrace();
		}
		
		return reg5;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "["
	f2 -> PrimaryExpression()
	f3 -> "]"
	*/
	public String visit(ArrayLookup n, ClassInfo info) {

		String reg = getRegister();
		String reg9 = getRegister();

		try {
			String reg1 = getRegister();
			String reg2 = getRegister();
			String reg3 = getRegister();
			String reg4 = getRegister();
			String reg5 = getRegister();
			String reg6 = getRegister();
			String reg7 = getRegister();
			String reg8 = getRegister();
			
			String label1 = getLabel();
			String label2 = getLabel();
			String label3 = getLabel();

			String name = n.f0.accept(this, info);
			int o = getOffset(info, name) + 8;

			bw.write(reg1 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
			bw.write(reg2 + " = bitcast i8* " + reg1 + " to i32**\n");
			bw.write(reg3 + " = load i32*, i32** " + reg2 + "\n");

			name = n.f2.accept(this, info);

			if(findReg(info, name) != null) bw.write(reg4 + " = load i32, i32* " + findReg(info, name) + "\n");
			else reg4 = name;

			bw.write(reg5 + " = load i32, i32* " + reg3 + "\n");
			bw.write(reg6 + " = icmp ult i32 " + reg4 + ", " + reg5 + "\n");
			bw.write("br i1 " + reg6 + ", label %" + label1 + ", label %" + label2 + "\n");

			bw.write(label1 + ":\n");
			bw.write(reg7 + " = add i32 " + reg4 + ", 1\n");
			bw.write(reg8 + " = getelementptr i32, i32* " + reg3 + ", i32 " + reg7 + "\n");
			bw.write(reg9 + " = load i32, i32* " + reg8 + "\n");

			bw.write("br label %" + label3 + "\n");
			bw.write(label2 + ":\n");
			bw.write("call void @throw_oob()\n");
			bw.write("br label %" + label3 + "\n");
			bw.write(label3 + ":\n");			
		}

		catch (IOException e) {
			e.printStackTrace();
		}
		
		return reg9;
	}

	/*
	f0 -> Identifier()
	f1 -> "["
	f2 -> Expression()
	f3 -> "]"
	f4 -> "="
	f5 -> Expression()
	f6 -> ";"
	*/
	public String visit(ArrayAssignmentStatement n, ClassInfo info) {

		try {
			String name = n.f0.accept(this, info);
			String reg = findReg(info, name);
			int o = getOffset(info, name) + 8;
			String reg1 = getRegister();
			String reg2 = getRegister();
			String reg3 = getRegister();

			String label1 = getLabel();
			String label2 = getLabel();
			String label3 = getLabel();
			
			bw.write(reg1 + " = getelementptr i8, i8* %this, i32 " + o + "\n");
			bw.write(reg2 + " = bitcast i8* " + reg1 + " to i32**\n");
			bw.write(reg3 + " = load i32*, i32** " + reg2 + "\n");

			String exp = n.f2.accept(this, info);

			String reg4 = findReg(info, exp);
			
			String reg5 = getRegister();
			String reg6 = getRegister();
			String reg7 = getRegister();

			if(reg4 != null) bw.write(reg5 + " = load i32, i32* " + reg4 + "\n");
			else reg5 = exp;

			bw.write(reg6 + " = load i32, i32* " + reg3 + "\n");
			bw.write(reg7 + " = icmp ult i32 " + reg5 + ", " + reg6 + "\n");
			bw.write("br i1 " + reg7 + ", label %" + label1 + ", label %" + label2 + "\n");
			bw.write(label1 + ":\n");

			String reg8  = getRegister();
			String reg9 = getRegister();

			bw.write(reg8 + " = add i32 " + reg5 + ", 1\n");
			bw.write(reg9 + " = getelementptr i32, i32* " + reg3 + ", i32 " + reg8 + "\n");
			
			exp = n.f5.accept(this, info);

			String reg10 = getRegister();
			if(findReg(info, exp) != null) {
				bw.write(reg10 + " = load i32, i32* " + findReg(info, exp) + "\n");
				exp = reg10;
			}

			bw.write("store i32 " + exp + ", i32* " + reg9 + "\n");
			bw.write("br label %" + label3 + "\n");
			
			bw.write(label2 + ":\n");
			bw.write("call void @throw_oob()\n");
			bw.write("br label %" + label3 + "\n");
			bw.write(label3 + ":\n");
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		return "OK";
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "."
	f2 -> "length"
	*/
	public String visit(ArrayLength n, ClassInfo info) {

		String name = n.f0.accept(this, info);
		return "OK";
	}

	/*
	f0 -> "new"
	f1 -> Identifier()
	f2 -> "("
	f3 -> ")"
	*/
	public String visit(AllocationExpression n, ClassInfo info) {

		String name = n.f1.accept(this, info);
		String reg1 = getRegister();

		try {

			String reg2 = getRegister();
			String reg3 = getRegister();
			ClassInfo node = search(info, name);
			int o = getOffset(info, name) + 8;

			bw.write("\t" + reg1 + " = call i8* @calloc(i32 1, i32 " + o + ")\n");
			bw.write("\t" + reg2 + " = bitcast i8* " + reg1 + " to i8***\n");
			bw.write("\t" + reg3 + " = getelementptr [" + node.functionsOffset.size() + " x i8*], [" + node.functionsOffset.size() + " x i8*]* @." + name + "_vtable, i32 0, i32 0\n");
			bw.write("\t" + "store i8** " + reg3 + ", i8*** " + reg2 + "\n\n");

			info.allocaReg.put(reg1, name);
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		return reg1;
	}

	public ClassInfo search(ClassInfo n, String name) {

		while(n.parent != null) n = n.parent;
		return n.members.get(name);
	}

	/*
	f0 -> "!"
	f1 -> Clause()
	*/
	public String visit(NotExpression n, ClassInfo info) {
		
		String clause = n.f1.accept(this, info);
		String reg = getRegister();
		String reg3 = getRegister();
		
		try {
			if(findReg(info, clause) != null) {
				bw.write(reg + " = load i1, i1* %" + clause + "\n");
				clause = reg;
			}
			bw.write(reg3 + " = xor i1 " + clause + ", 1\n");
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		return reg3;
	}

	/*
	f0 -> "("
	f1 -> Expression()
	f2 -> ")"
	*/
	public String visit(BracketExpression n, ClassInfo info) {
		return n.f1.accept(this, info);
	}

	/*
	f0 -> ArrayType() | BooleanType() | IntegerType() | Identifier()
	*/
	public String visit(Type n, ClassInfo info) {
		return n.f0.accept(this, info);
	}

	/*
	f0 -> "int"
	f1 -> "["
	f2 -> "]"
	*/
	public String visit(ArrayType n, ClassInfo info) {
		return "int[]";
	}

	/*
	f0 -> "boolean"
	*/
	public String visit(BooleanType n, ClassInfo info) {
		return "boolean";
	}

	/*
	f0 -> "int"
	*/
	public String visit(IntegerType n, ClassInfo info) {
		return "int";
	}
}
