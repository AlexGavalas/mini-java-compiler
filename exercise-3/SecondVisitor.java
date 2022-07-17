import syntaxtree.*;
import java.util.*;
import visitor.GJDepthFirst;

public class SecondVisitor extends GJDepthFirst<String, ClassInfo> {

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

	public String findClass(ClassInfo node, String name) {

		while(node.parent != null) node = node.parent;
		
		if(node.members.get(name) != null) return "OK";
		else return null;
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
				if(status.equals("NOT_OK")) return "NOT_OK";
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
		String status;
		ClassInfo main = info.members.get(name).functions.get("main");

		if(n.f14.present()) {
			
			String[] names;

			for(Enumeration<Node> e = n.f14.elements() ; e.hasMoreElements() ; ) {
				
				status = e.nextElement().accept(this, main);
				names = status.split(" ");
				
				if(searchDefaults(names[0]) == null && findClass(main, names[0]) == null) return "NOT_OK";
			}
		}
		if(n.f15.present()) {

			for(Enumeration<Node> e = n.f15.elements() ; e.hasMoreElements() ; ) {
				
				status = e.nextElement().accept(this, main);
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
		String status;
		
		if(n.f3.present()) {

			String[] names;

			for(Enumeration<Node> e = n.f3.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info.members.get(name));
				names = status.split(" ");

				if(searchDefaults(names[0]) == null && findClass(info.members.get(name), names[0]) == null) return "NOT_OK";
			}
		}
		if(n.f4.present()) {

			for(Enumeration<Node> e = n.f4.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info.members.get(name));
				
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
		String status;

		if(n.f5.present()) {
			String[] names;

			for(Enumeration<Node> e = n.f5.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info.members.get(name));
				names = status.split(" ");
		
				if(searchDefaults(names[0]) == null && findClass(info.members.get(name), names[0]) == null) return "NOT_OK";
			}
		}
		if(n.f6.present()) {

			for(Enumeration<Node> e = n.f6.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info.members.get(name));
				
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
		String type = n.f0.accept(this, info);
		String name = n.f1.accept(this, info);
		return type + " " + name;
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

		if(searchDefaults(type) == null && findClass(info, type) == null) return "NOT_OK";

		if(n.f4.present()) {
			status = n.f4.accept(this, info.functions.get(name));
			if(status == null) return status;
		}
		
		if(n.f7.present()) {
			String[] names;

			for(Enumeration<Node> e = n.f7.elements() ; e.hasMoreElements() ; ) {
				
				status = e.nextElement().accept(this, info.functions.get(name));
				names = status.split(" ");
				
				if(searchDefaults(names[0]) == null && findClass(info.functions.get(name), names[0]) == null) return "NOT_OK";
			}
		}

		if(n.f8.present()) { 
			for(Enumeration<Node> e = n.f8.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info.functions.get(name));
				if(status == null) return status;
			}
		}
		
		status = n.f10.accept(this, info.functions.get(name));

		if(status == null) return null;
		else if(status.equals(type)) return "OK";
		else if(getType(info.functions.get(name), status).equals(type)) return "OK";
		else return null;
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
		memberInfo.type = n.f0.accept(this, info);
		memberInfo.name = n.f1.accept(this, info);
		
		if(searchDefaults(memberInfo.type) == null && findClass(info, memberInfo.type) == null) return null;
		else return "OK";
	}

	/*
	f0 -> ( FormalParameterTerm() )*
	*/
	public String visit(FormalParameterTail n, ClassInfo info) {
		
		String status;

		if(n.f0.present()) {
			for(Enumeration<Node> e = n.f0.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info);
				if(status == null) return status;
			}
		}

		return "OK";
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

	/*
	f0 -> Block() | AssignmentStatement() | ArrayAssignmentStatement() | IfStatement() | WhileStatement() | PrintStatement()
	*/
	public String visit(Statement n, ClassInfo info) {
		return n.f0.accept(this, info);
	}

	/*
	f0 -> "{"
	f1 -> ( Statement() )*
	f2 -> "}"
	*/
	public String visit(Block n, ClassInfo info) {
		
		String status;

		if(n.f1.present()) {
			for(Enumeration<Node> e = n.f1.elements() ; e.hasMoreElements() ; ) {
				status = e.nextElement().accept(this, info);
				if(status == null) return status;
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

		String type;
		String type2;

		if(exp == null) return null;
		if(exp.equals("this")) exp = info.parent.name;

		if(searchDefaults(name) == null) type = getType(info, name);
		else type = name;

		if(searchDefaults(exp) == null) type2 = getType(info, exp);
		else type2 = exp;

		if(type.equals(type2)) return "OK";
		else if(checkup2(info, type, type2)) return "OK";
		else return null;
	}

	public boolean checkup2(ClassInfo node, String s, String t) {
		while(node.parent != null) node = node.parent;

		ClassInfo n = node.members.get(t);

		while(n != null) {
			if(n.name.equals(s)) return true;
			else n = n.parent;
		}
		return false;
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
		
		String name = n.f0.accept(this, info);
		String type = getType(info, name);

		if(type != null && type.equals("int[]")) {
			String exp = n.f2.accept(this, info);
			if(exp != null && (exp.equals("int") || getType(info, exp).equals("int"))) return n.f5.accept(this, info);
			else return null;
		}
		else return null;
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
		String statement;

		if(exp != null && (exp.equals("boolean") || getType(info, exp).equals("boolean"))) {
			statement = n.f4.accept(this, info);
			if(statement != null) return n.f6.accept(this, info);
			else return null;
		}

		else return null;
	}

	/*
	f0 -> "while"
	f1 -> "("
	f2 -> Expression()
	f3 -> ")"
	f4 -> Statement()
	*/
	public String visit(WhileStatement n, ClassInfo info) {
		
		String exp = n.f2.accept(this, info);

		if(exp != null && (exp.equals("boolean") || getType(info, exp).equals("boolean"))) return n.f4.accept(this, info);
		else return null;
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
		
		if(exp != null && (exp.equals("int") || getType(info, exp).equals("int"))) return "OK";
		else return null;
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

		if(clause != null && (getType(info, clause).equals("boolean") || clause.equals("boolean"))) {
			clause = n.f2.accept(this, info);
			if(clause != null && (getType(info, clause).equals("boolean") || clause.equals("boolean"))) return "boolean";
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
				
		if(exp != null && (getType(info, exp).equals("int") || exp.equals("int"))) {
			exp = n.f2.accept(this, info);
			if(exp != null && (getType(info, exp).equals("int") || exp.equals("int"))) return "boolean";
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
				
		if(exp != null && (getType(info, exp).equals("int") || exp.equals("int"))) {
			exp = n.f2.accept(this, info);
			if(exp != null && (getType(info, exp).equals("int") || exp.equals("int"))) return "int";
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
				
		if(exp != null && (getType(info, exp).equals("int") || exp.equals("int"))) {
			exp = n.f2.accept(this, info);
			if(exp != null && (getType(info, exp).equals("int") || exp.equals("int"))) return "int";
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
				
		if(exp != null && (getType(info, exp).equals("int") || exp.equals("int"))) {
			exp = n.f2.accept(this, info);
			if(exp != null && (getType(info, exp).equals("int") || exp.equals("int"))) return "int";
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

		if(name != null && getType(info, name).equals("int[]")) {
			
			name = n.f2.accept(this, info);
			if(name != null && (name.equals("int") || getType(info, name).equals("int"))) return "int";
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

		if(name != null && getType(info, name).equals("int[]")) return "int";
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
		
		String name = n.f0.accept(this, info);		
		String method = n.f2.accept(this, info);
		ClassInfo def;

		if(name.equals("this")) {

			def = getDef(info, method);
			if(def == null) return null;
		}

		else {

			name = getType(info, name);
			def = getOuterDef(info, name, method);
			if(def == null) return null;
		}

		if(n.f4.present()) {

			String args = n.f4.accept(this, info);
			String[] names = args.split(" ");
			ArrayList<String> temp = def.arguments;
			
			if(temp != null) {

				if(temp.size() != names.length) return null;
				for(int i = 0 ; i < temp.size() ; i++) {
					
					String type = names[i];
					String expectedType = temp.get(i);

					if(!type.equals(expectedType)) {
						if(!getType(info, type).equals(expectedType)) {
							if(!checkup(info, getType(info, type), expectedType)) return null;
						}
					}
				}
			}
			else return null;
		}
		else if(def.arguments.size() != 0) return null;
		return def.type;
	}

	public boolean checkup(ClassInfo node, String s, String t) {

		while(node.parent != null) node = node.parent;

		ClassInfo n = node.members.get(s); 

		while(n != null) {
			if(n.name.equals(t)) return true;
			else n = n.parent;
		}
		return false;
	}

	/*
	f0 -> Expression()
	f1 -> ExpressionTail()
	*/	
	public String visit(ExpressionList n, ClassInfo info) {
		String exp = n.f0.accept(this, info);
		String expT = n.f1.accept(this, info);
		return exp + expT;
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

	public String visit(ThisExpression n, ClassInfo info) {
		return "this";
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
		
		if(exp != null && (exp.equals("int") || getType(info, exp).equals("int"))) return "int[]";
		else return null;
	}

	/*
	f0 -> "new"
	f1 -> Identifier()
	f2 -> "("
	f3 -> ")"
	*/
	public String visit(AllocationExpression n, ClassInfo info) {
		return n.f1.accept(this, info);
	}

	/*
	f0 -> "!"
	f1 -> Clause()
	*/
	public String visit(NotExpression n, ClassInfo info) {
		
		String clause = n.f1.accept(this, info);

		if(clause != null && (clause.equals("boolean") || getType(info, clause).equals("boolean"))) return "boolean";
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