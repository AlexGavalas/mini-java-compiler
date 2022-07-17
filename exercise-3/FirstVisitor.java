import syntaxtree.*;
import java.util.*;
import visitor.GJDepthFirst;

public class FirstVisitor extends GJDepthFirst<String, ClassInfo> {

	public String[] defaultTypes = {"int", "int[]", "boolean"};

	public String searchDefaults(String s) {
		
		if(s == null) return null;

		for(String st : defaultTypes) {
			if(st.equals(s)) return st;
		}

		return null;
	}

	public String getType(ClassInfo node, String var) {
		
		if(var == null) return "";

		while(node != null) {
			if(node.members.get(var) != null) return node.members.get(var).type;
			else node = node.parent;
		}
		return "";
	}

	/*
	f0 -> MainClass()
	f1 -> ( TypeDeclaration() )*
	f2 -> <EOF>
	*/
	public String visit(Goal n, ClassInfo info) {

		String status = n.f0.accept(this, info);
		if(status.equals("NOT_OK")) return status;
		
		if(n.f1.present()) {
			for(Enumeration<Node> e = n.f1.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info);
				if(status.equals("NOT_OK")) return status;
			}
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
		
		String name = n.f1.accept(this, info);
		
		ClassInfo node = new ClassInfo();
		node.parent = info;
		node.name = name;
		node.type = "class";

		info.members.put(node.name, node);
		
		ClassInfo main = new ClassInfo();
		main.parent = node;
		main.name = "main";
		main.type = "void";

		node.functions.put(main.name, main);

		name = n.f11.accept(this, info);
		
		ClassInfo nameInfo = new ClassInfo();
		nameInfo.parent = main;
		nameInfo.name = name;
		nameInfo.type = "String[]";
		
		main.members.put(nameInfo.name, nameInfo);

		if(n.f14.present()) {	
			for(Enumeration<Node> e = n.f14.elements() ; e.hasMoreElements() ; ) {
				String status = e.nextElement().accept(this, node.functions.get("main"));
				if(status == null) return "NOT_OK";
			}
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
		
		ClassInfo node = new ClassInfo();
		node.parent = info;
		node.name = name;
		node.type = name;

		if(info.members.get(node.name) == null) info.members.put(name, node);
		else return "NOT_OK";
		
		if(n.f3.present()) {
			for(Enumeration<Node> e = n.f3.elements() ; e.hasMoreElements() ; ) {
				String status = e.nextElement().accept(this, info.members.get(name));
				if(status == null) return "NOT_OK";
			}
		}

		if(n.f4.present()) {
			for(Enumeration<Node> e = n.f4.elements() ; e.hasMoreElements() ; ) {
				String status = e.nextElement().accept(this, info.members.get(name));
				if(status == null) return "NOT_OK";
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
		String parent = n.f3.accept(this, info);

		ClassInfo node = new ClassInfo();
		node.parent = info.members.get(parent);
		node.name = name;
		node.type = name;

		if(node.parent == null || info.members.get(node.name) != null) return "NOT_OK";
		else info.members.put(name, node);

		if(n.f5.present()) {
			node.offset = node.parent.offset;
			for(Enumeration<Node> e = n.f5.elements() ; e.hasMoreElements() ; ) {
				String status = e.nextElement().accept(this, info.members.get(name));
				if(status == null) return "NOT_OK";
			}
		}
		
		node.functionOffset = node.parent.functionOffset;
		for(int i = 0 ; i < node.parent.functionsOffset.size() ; i++) node.functionsOffset.add(node.parent.functionsOffset.get(i));		

		if(n.f6.present()) {

			for(Enumeration<Node> e = n.f6.elements() ; e.hasMoreElements() ; ) {
				String status = e.nextElement().accept(this, info.members.get(name));
				if(status == null) return "NOT_OK";
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
		
		ClassInfo member = new ClassInfo();
		member.parent = info;
		member.type = n.f0.accept(this, info);
		member.name = n.f1.accept(this, info);

		if(info.members.get(member.name) == null) {

			member.offset = info.offset;

			if(member.type.equals("int")) info.offset += 4;
			else if(member.type.equals("boolean")) info.offset += 1;
			else info.offset += 8;

			info.membersOffset.add(member);
			info.members.put(member.name, member);
			return "OK";
		}

		else return null;
	}

	public int searchFuns(ClassInfo n, String name) {

		while(n != null) {
			
			for(String s : n.functions.keySet()) {

				ClassInfo t = n.functions.get(s);

				if(t.name.equals(name)) return t.offset; 
			}

			n = n.parent;
		}

		return -1;
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
		
		String type = n.f1.accept(this, info);
		String name = n.f2.accept(this, info);
		String status;

		ClassInfo node = new ClassInfo();
		node.parent = info;
		node.name = name;
		node.type = type;

		if(info.functions.get(name) == null) {
			
			int temp = searchFuns(info, node.name);

			if(temp == -1) {

				node.functionOffset = info.functionOffset;
				info.functionOffset += 1;
				info.functionsOffset.add(node);
			}
			else {
				node.functionOffset = temp;
				node.overriden = 1;
			}

			info.functions.put(node.name, node);
		}
		else return null;

		if(n.f4.present()) {
			status = n.f4.accept(this, info.functions.get(name));
			if(status == null) return status;
		}

		if(checkOverload(info.parent, name, info.functions.get(name))) return null;

		if(n.f7.present()) {
			for(Enumeration<Node> e = n.f7.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info.functions.get(name));
				if(status == null) return status;
			}
		}
		
		status = n.f10.accept(this, info.functions.get(name));
		return status;
	}

	public boolean checkOverload(ClassInfo node, String method, ClassInfo function) {
		
		while(node != null) {
			if(node.functions.get(method) != null) {
				if(function.arguments.size() != node.functions.get(method).arguments.size()) return true;
				for(int i = 0 ; i < function.arguments.size() ; i++) {
					if(!function.arguments.get(i).equals(node.functions.get(method).arguments.get(i))) return true;
				}
				return false;
			}
			else node = node.parent;
		}
		return false;
	}
	
	/*
	f0 -> FormalParameter()
	f1 -> FormalParameterTail()
	*/
	public String visit(FormalParameterList n, ClassInfo info) {

		String status = n.f0.accept(this, info);		
		if(status != null) status = n.f1.accept(this, info);
		
		return status;
	}

	/*
	f0 -> Type()
	f1 -> Identifier()
	*/
	public String visit(FormalParameter n, ClassInfo info) {
		
		ClassInfo memberInfo = new ClassInfo();
		memberInfo.parent = info;
		memberInfo.name = n.f1.accept(this, info);
		memberInfo.type = n.f0.accept(this, info);
		
		if(info.members.get(memberInfo.name) == null) {
			info.members.put(memberInfo.name, memberInfo);
			info.arguments.add(memberInfo.type);
			return "OK";
		}

		else return null;
	}

	/*
	f0 -> ( FormalParameterTerm() )*
	*/
	public String visit(FormalParameterTail n, ClassInfo info) {
		
		String status = "OK";
		
		if(n.f0.present()) {
			for(Enumeration<Node> e = n.f0.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info);
				if(status == null) return status;
			}
		}

		return status;
	}

	/*
	f0 -> ","
	f1 -> FormalParameter()
	*/
	public String visit(FormalParameterTerm n, ClassInfo info) {
		return n.f1.accept(this, info);
	}

	/*
	f0 -> ArrayType() | BooleanType() | IntegerType() | Identifier()
	*/
	public String visit(Type n, ClassInfo info) {
		return n.f0.accept(this, info);
	}

	public String visit(ArrayType n, ClassInfo info) {
		return "int[]";
	}

	public String visit(BooleanType n, ClassInfo info) {
		return "boolean";
	}

	public String visit(IntegerType n, ClassInfo info) {
		return "int";
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
		
		String clause = n.f0.accept(this, info);

		if(getType(info, clause).equals("boolean") || clause.equals("boolean")) {
			clause = n.f2.accept(this, info);
			if(getType(info, clause).equals("boolean") || clause.equals("boolean")) return "boolean";
			else return null;
		}

		else return null;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "<"
	f2 -> PrimaryExpression()
	*/
	public String visit(CompareExpression n, ClassInfo info) {
		
		String exp = n.f0.accept(this, info);
				
		if(getType(info, exp).equals("int") || exp.equals("int")) {
			exp = n.f2.accept(this, info);
			if(getType(info, exp).equals("int") || exp.equals("int")) return "boolean";
			else return null;
		}

		else return null;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "+"
	f2 -> PrimaryExpression()
	*/
	public String visit(PlusExpression n, ClassInfo info) {
		
		String exp = n.f0.accept(this, info);
				
		if(getType(info, exp).equals("int") || exp.equals("int")) {
			exp = n.f2.accept(this, info);
			if(getType(info, exp).equals("int") || exp.equals("int")) return "int";
			else return null;
		}
		
		else return null;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "-"
	f2 -> PrimaryExpression()
	*/
	public String visit(MinusExpression n, ClassInfo info) {
		
		String exp = n.f0.accept(this, info);
				
		if(getType(info, exp).equals("int") || exp.equals("int")) {
			exp = n.f2.accept(this, info);
			if(getType(info, exp).equals("int") || exp.equals("int")) return "int";
			else return null;
		}
		
		else return null;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "*"
	f2 -> PrimaryExpression()
	*/
	public String visit(TimesExpression n, ClassInfo info) {
		
		String exp = n.f0.accept(this, info);
				
		if(getType(info, exp).equals("int") || exp.equals("int")) {
			exp = n.f2.accept(this, info);
			if(getType(info, exp).equals("int") || exp.equals("int")) return "int";
			else return null;
		}
		
		else return null;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "["
	f2 -> PrimaryExpression()
	f3 -> "]"
	*/
	public String visit(ArrayLookup n, ClassInfo info) {
		
		String name = n.f0.accept(this, info);

		if(getType(info, name).equals("int[]")) {
			
			name = n.f2.accept(this, info);
			if(name.equals("int") || getType(info, name).equals("int")) return "int";
			else return null;
		}

		else return null;
	}

	/*
	f0 -> PrimaryExpression()
	f1 -> "."
	f2 -> "length"
	*/
	public String visit(ArrayLength n, ClassInfo info) {

		String name = n.f0.accept(this, info);
		if(getType(info, name).equals("int[]")) return "int";
		else return null;
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
		
		n.f0.accept(this, info);
		n.f2.accept(this, info);
		if(n.f4.present()) {
			n.f4.accept(this, info);
		}
		return "???";
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
		return "int";
	}

	public String visit(TrueLiteral n, ClassInfo info) {
		return "boolean";
	}

	public String visit(FalseLiteral n, ClassInfo info) {
		return "boolean";
	}

	public String visit(Identifier n, ClassInfo info) {
		return n.f0.toString();
	}

	/*
	f0 -> "!"
	f1 -> Clause()
	*/
	public String visit(NotExpression n, ClassInfo info) {
		
		String clause = n.f1.accept(this, info);

		if(clause.equals("boolean") || getType(info, clause).equals("boolean")) return "boolean";
		else return null;
	}

	/*
	f0 -> "("
	f1 -> Expression()
	f2 -> ")"
	*/
	public String visit(BracketExpression n, ClassInfo info) {
		return n.f1.accept(this, info);
	}
}