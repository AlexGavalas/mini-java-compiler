import java.util.HashMap;
import java.util.ArrayList;

class ClassInfo {

	ClassInfo parent;
	
	String name;
	String type;

	int offset;
	int functionOffset;
	int overriden;
	
	HashMap<String, ClassInfo> members;
	HashMap<String, ClassInfo> functions;
	ArrayList<String> arguments;

	ArrayList<ClassInfo> membersOffset;
	ArrayList<ClassInfo> functionsOffset;
	HashMap<String, String> registers;
	HashMap<String, String> allocaReg;


	@Override
	public String toString() {
		return this.name + "  " + this.type + "  " + members + "  " + functions + "  " + arguments;
	}

	ClassInfo() {
		this.members = new HashMap<>();
		this.functions = new HashMap<>();
		this.arguments = new ArrayList<>();
		this.membersOffset = new ArrayList<>();
		this.functionsOffset = new ArrayList<>();
		this.registers = new HashMap<>();
		this.allocaReg = new HashMap<>();
	}

	public void printOffset(ClassInfo node) {

		for(String s : node.members.keySet()) {
			
			ClassInfo n = node.members.get(s);

			if(!n.type.equals("class")) {
			
				System.out.println("-----------Class " + n.name + "-----------");
				System.out.println("--Variables---");
				
				for(int i = 0 ; i < n.membersOffset.size() ; i++) {
					if(n.membersOffset.get(i).overriden == 0) System.out.println(n.name + "." + n.membersOffset.get(i).name + " : " + n.membersOffset.get(i).offset);
				}

				System.out.println("---Methods---");

				for(int i = 0 ; i < n.functionsOffset.size() ; i++) {
					if(n.functionsOffset.get(i).overriden == 0) System.out.println(n.name + "." + n.functionsOffset.get(i).name + " : " + n.functionsOffset.get(i).functionOffset);
				}

				System.out.println();
			}
		}
	}
}